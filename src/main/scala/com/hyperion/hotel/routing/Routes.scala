package com.hyperion.hotel.routing

import cats.effect._
import cats.implicits._
import com.hyperion.hotel.database.Store
import com.hyperion.hotel.models._
import com.typesafe.scalalogging.Logger
import doobie.ConnectionIO
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.Http4sDsl

object Routes extends Http4sDsl[IO] {

  val logger = Logger(getClass)

  def routes(store: Store[IO, ConnectionIO]): HttpRoutes[IO] = {

    HttpRoutes
      .of[IO] {

        case (GET | HEAD) -> Root / "ping" =>
          Ok("Pong!")

        case GET -> Root / "bookings" / roomIdString => {
          val dbResult = for {
            roomId <- IO(roomIdString.toInt)
            resultUnit <- store.getBookings(roomId)
          } yield resultUnit

          Ok(s"Bookings for roomId: $roomIdString: $dbResult")
        }


        //    case POST -> Root / "new-booking" => {
        //      req.as[Booking]
        //
        //      Ok(s"Booking for roomId: ${} has been created")
        //    }

      }



  }

}
