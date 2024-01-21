package com.hyperion.hotel.database

import cats.arrow.FunctionK
import cats.effect.{Async, Blocker, Bracket, ContextShift, Resource, Sync}
import cats.syntax.all._
import cats.~>
import com.hyperion.hotel.config.DatabaseConfig
import com.hyperion.hotel.models._
import com.typesafe.scalalogging.Logger
import doobie.{ConnectionIO, Transactor}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import sourcecode.{FullName, Line}

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext

trait Store[F[_], G[_]] {

  def liftK: F ~> G
  def commit[A](f: G[A]): F[A]
  def insertBooking(booking: Booking): F[BookingResult]
  def getBookings(roomId: Int): F[List[Booking]]
  def getAllBookingsForDates(startDate: ZonedDateTime,
                             endDate: ZonedDateTime): F[List[Int]]
  def getAllBookingsForCustomer(name: String): F[List[Booking]]
  def checkBookingExists(booking: BookingReceived): F[Boolean]
  def cancelBooking(booking: BookingReceived): F[Boolean]
  def removeExpired(): F[String]

}

final class PostgresStore[F[_]: Sync](transactor: Transactor[F], val liftK: FunctionK[F, ConnectionIO])(implicit b: Bracket[F, Throwable])
  extends Store[F, ConnectionIO] {

  //private val cioUnit: ConnectionIO[Unit] = ().pure[ConnectionIO]
  val logger: Logger = Logger(getClass)

  override def commit[A](f: ConnectionIO[A]): F[A] = f.transact(transactor)

  override def insertBooking(booking: Booking): F[BookingResult] =
    commit(SQLQueries.insertBooking(booking).run.attempt.map {
      case Right(rows) => {
        rows match {
          case 1 =>
            SuccessfulBooking
          case _ =>
            FailedBooking(List(s"Booking was not successful for roomId: ${booking.roomId}"))
        }
      }
      case Left(e) => {
        FailedBooking(List(s"Inserting booking failed for roomId: ${booking.roomId}, error: ${e.getMessage}"))
      }
    })

  override def getBookings(roomId: Int): F[List[Booking]] =
    SQLQueries.getBookings(roomId).transact(transactor)

  override def getAllBookingsForDates(startDate: ZonedDateTime, endDate: ZonedDateTime): F[List[Int]] =
    SQLQueries.getAllBookingsForDates(startDate, endDate).transact(transactor)

  override def getAllBookingsForCustomer(name: String): F[List[Booking]] =
    SQLQueries.getAllBookingsForCustomer(name).transact(transactor)

  override def checkBookingExists(booking: BookingReceived): F[Boolean] =
    SQLQueries.checkBookingExists(booking).transact(transactor).map(_.nonEmpty)

  override def cancelBooking(booking: BookingReceived): F[Boolean] =
    commit(SQLQueries.cancelBooking(booking).run.attempt.flatMap {
      case Right(rows) => {
        rows match {
          case 1 => println(s"Booking cancellation was successful for roomId: ${booking.roomId}")
            true.pure[ConnectionIO]
          case _ =>
            println(s"Booking cancellation was unsuccessful for roomId: ${booking.roomId}")
            false.pure[ConnectionIO]
        }
      }
      case Left(e) => {
        println(s"------booking cancellation failed for roomId: ${booking.roomId}, error: ${e.getMessage}")
        databaseError(s"Error when trying to remove booking from database, error: ${e.getMessage}", Some(e))
        false.pure[ConnectionIO]
      }
    })

  override def removeExpired(): F[String] =
    commit(SQLQueries.removeExpiredBookings().run.attempt.flatMap {
      case Right(rows) => s"$rows were removed as they had expired".pure[ConnectionIO]

      case Left(e) => {
        databaseError(s"Error when trying to remove expired bookings from database, error: ${e.getMessage}", Some(e))
        s"Removing expired booking failed, error: ${e.getMessage}".pure[ConnectionIO]
      }
    })

  private def databaseError[A](msg: String, e: Option[Throwable] = None)(implicit file: FullName, line: Line): ConnectionIO[A] = {
    e.fold(logger.error(msg))(logger.error(msg, _))
    DatabaseError(msg).raiseError[ConnectionIO, A]
  }
}

object PostgresStore {
  def resource[F[_]: Async: ContextShift](config: DatabaseConfig,
                                          ec: ExecutionContext,
                                          blocker: Blocker,
                                          lift: FunctionK[F, ConnectionIO]
                                         ): Resource[F, Store[F, ConnectionIO]] =
    HikariTransactor
      .newHikariTransactor[F](config.driver, config.url, config.username, config.password, ec, blocker)
      .map(trans => new PostgresStore[F](trans, lift))
}
