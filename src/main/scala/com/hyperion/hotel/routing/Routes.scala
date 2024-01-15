package com.hyperion.hotel.routing

import cats.effect._
import cats.implicits._
import com.hyperion.hotel.database.Store
import com.hyperion.hotel.handlers.BookingHandler
import com.hyperion.hotel.models._
import com.typesafe.scalalogging.Logger
import org.http4s.circe._
import org.http4s.{EntityDecoder, HttpRoutes, InvalidMessageBodyFailure}
import org.http4s.dsl.Http4sDsl

class Routes[F[_]: Sync, G[_]](store: Store[F, G],
                               bookingsHandler: BookingHandler[F, G]) extends Http4sDsl[F] {

  val logger = Logger(getClass)

  val routes: HttpRoutes[F] = {

    implicit def decodeBooking: EntityDecoder[F, Booking] = jsonOf[F, Booking]
    implicit def decodeDates: EntityDecoder[F, JustDates] = jsonOf[F, JustDates]

    HttpRoutes
      .of[F] {

        case (GET | HEAD) -> Root / "ping" =>
          Ok("Pong!")

        case GET -> Root / "bookings" / roomIdString =>
          for {
            roomId <- roomIdString.toInt.pure[F]
            bookings <- store.getBookings(roomId)
            res <- Ok(s"Bookings for roomId: $roomIdString: ${bookings.mkString_("\n")}")
          } yield res

        case GET -> Root / "bookings-for" / customerName =>
          for {
            bookings <- store.getAllBookingsForCustomer(customerName)
            res <- bookings match {
              case Nil => Ok(s"We don't currently have any bookings for $customerName")
              case list => Ok(s"We have the following bookings for $customerName: ${list.mkString_("\n")}")
            }
          } yield res

        case req @ POST -> Root / "new-booking" =>
          req.as[Booking].flatMap { booking =>
            for {
              count <- bookingsHandler.processBooking(booking)
              res <- count match {
                case false => Ok(s"Booking for roomId: ${booking.roomId} was unsuccessful")
                case true => Ok(s"Booking for roomId: ${booking.roomId} has been created")
              }
            } yield res
          }
            .handleErrorWith {
              case InvalidMessageBodyFailure(dets, _) => BadRequest(s"Error details: $dets")
            }

        case req @ POST -> Root / "cancel-booking" =>
          req.as[Booking].flatMap { booking =>
            for {
              result <- bookingsHandler.cancelBooking(booking)
              response <- result match {
                case false => Ok(s"Booking for ${booking.customerName} starting on the: ${booking.startDate} was unsuccessfully cancelled")
                case true => Ok(s"Booking for ${booking.customerName} starting on the: ${booking.startDate} has been cancelled as requested")
              }
            } yield response
          }
            .handleErrorWith {
              case InvalidMessageBodyFailure(dets, _) => BadRequest(s"Error details: $dets")
            }

        case req @ POST -> Root / "available-rooms" =>
          req.as[JustDates].flatMap { dates =>
            for {
              rooms <- bookingsHandler.getAllAvailableRooms(dates.startDate, dates.endDate)
              res <- Ok(s"The following rooms are available from ${dates.startDate} to ${dates.endDate}: $rooms")
            } yield res
          }

        case GET -> Root / "remove-expired" =>
          for {
            result <- store.removeExpired()
            response <- Ok(result)
          } yield response
      }


  }

}
