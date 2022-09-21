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

    HttpRoutes
      .of[F] {

        case (GET | HEAD) -> Root / "ping" =>
          Ok("Pong!")

        case GET -> Root / "bookings" / roomIdString =>
          for {
            roomId <- roomIdString.toInt.pure[F]
            resultUnit <- store.getBookings(roomId)
            res <- Ok(s"Bookings for roomId: $roomIdString: ${resultUnit.mkString_(" | ")}")
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

      }



  }

}
