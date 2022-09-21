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

import scala.concurrent.ExecutionContext

trait Store[F[_], G[_]] {

  def liftK: F ~> G
  def commit[A](f: G[A]): F[A]
  def insertBooking(booking: Booking): F[Boolean]
  def getBookings(roomId: Int): F[List[Booking]]

}

final class PostgresStore[F[_]: Sync](transactor: Transactor[F], val liftK: FunctionK[F, ConnectionIO])(implicit b: Bracket[F, Throwable])
  extends Store[F, ConnectionIO] {

  //private val cioUnit: ConnectionIO[Unit] = ().pure[ConnectionIO]
  val logger: Logger = Logger(getClass)

  override def commit[A](f: ConnectionIO[A]): F[A] = f.transact(transactor)

  override def insertBooking(booking: Booking): F[Boolean] =
    commit(SQLQueries.insertBooking(booking).run.attempt.flatMap {
      case Right(rows) => {
        rows match {
          case 1 => println(s"-------Booking was successful for roomId: ${booking.roomId}")
            true.pure[ConnectionIO]
          case _ =>
            println(s"-----------Booking was not successful for roomId: ${booking.roomId}")
            false.pure[ConnectionIO]
        }
      }
      case Left(e) => {
        println(s"------------- insert booking fails for for roomId: ${booking.roomId}, error: ${e.getMessage}")
        databaseError(s"Error when trying to insert booking into database, error: ${e.getMessage}", Some(e))
        false.pure[ConnectionIO]
      }
    })

  override def getBookings(roomId: Int): F[List[Booking]] =
    SQLQueries.getBookings(roomId).transact(transactor)


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
