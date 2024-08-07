package com.hyperion.hotel.routing

import cats.effect._
import cats.implicits._
import com.hyperion.hotel.database.Store
import com.hyperion.hotel.handlers.{AvailabilityHandler, BookingHandler}
import com.hyperion.hotel.models._
import com.typesafe.scalalogging.Logger
import org.http4s.circe._
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, InvalidMessageBodyFailure}
import org.http4s.dsl.Http4sDsl

class Routes[F[_]: Sync, G[_]](store: Store[F, G],
                               bookingsHandler: BookingHandler[F, G],
                               availabilityHandler: AvailabilityHandler[F, G]
                              ) extends Http4sDsl[F] {

  val logger = Logger(getClass)

  val routes: HttpRoutes[F] = {

    implicit def decodeBooking: EntityDecoder[F, BookingReceived] = jsonOf[F, BookingReceived]
    implicit def decodeBookingForRoomType: EntityDecoder[F, BookingForRoomType] = jsonOf[F, BookingForRoomType]
    implicit def decodeDates: EntityDecoder[F, JustDates] = jsonOf[F, JustDates]
    implicit def decodeSpecialDealBooking: EntityDecoder[F, SpecialDealBooking] = jsonOf[F, SpecialDealBooking]
    implicit val encodeAvailableRooms: EntityEncoder[F, Option[AvailableRooms]] = jsonEncoderOf[F, Option[AvailableRooms]]
    implicit def decodeCheckAvailability: EntityDecoder[F, CheckAvailability] = jsonOf[F, CheckAvailability]

    HttpRoutes
      .of[F] {

        case (GET | HEAD) -> Root / "ping" =>
          Ok("Pong!")

        case GET -> Root / "bookings" / roomIdString =>
          for {
            roomId <- roomIdString.toInt.pure[F]
            bookings <- store.getBookings(roomId)
            res <- bookings match {
              case Nil => Ok(s"There are currently no bookings for room: $roomIdString")
              case _ => Ok(s"Bookings for roomId: $roomIdString: ${bookings.mkString_("\n")}")
            }
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
          req.as[BookingReceived].flatMap { booking =>
            for {
              result <- bookingsHandler.processBooking(booking)
              response <- result match {
                case FailedBooking(details) => Ok(s"Booking for roomId: ${booking.roomId} was unsuccessful, details: $details")
                case SuccessfulBooking => Ok(s"Booking for roomId: ${booking.roomId} was successful")
                case _ => BadRequest()
              }
            } yield response
          }
            .handleErrorWith {
              case InvalidMessageBodyFailure(dets, _) => BadRequest(s"Error details: $dets")
            }

        case req @ POST -> Root / "special-deal-booking" / specialId =>
          req.as[BookingReceived].flatMap { booking =>
            for {
              result <- bookingsHandler.processSpecialDealBooking(booking, specialId)
              response <- result match {
                case FailedBooking(details) => Ok(s"Special Booking for $specialId roomId: ${booking.roomId} was unsuccessful, details: $details")
                case SuccessfulBooking => Ok(s"Special Booking for $specialId roomId: ${booking.roomId} was successful")
                case _ => BadRequest()
              }
            } yield response
          }
            .handleErrorWith {
              case InvalidMessageBodyFailure(dets, _) => BadRequest(s"Error details: $dets")
            }

        case req @ POST -> Root / "booking-by-room-type" =>
          req.as[BookingForRoomType].flatMap { booking =>
            for {
              result <- bookingsHandler.bookNextAvailableRoomByType(booking)
              response <- result match {
                case FailedBooking(details) => Ok(s"Booking was unsuccessful, details: $details")
                case SuccessfulBookingMade(roomId) => Ok(s"Booking was successful for ${booking.customerName}, you will be staying in room: $roomId")
                case SuccessfulBooking => Ok(s"Booking was successful for ${booking.customerName}")
              }
            } yield response
          }
            .handleErrorWith {
              case InvalidMessageBodyFailure(dets, _) => BadRequest(s"Error details: $dets")
            }

        case req @ POST -> Root / "cancel-booking" =>
          req.as[BookingReceived].flatMap { booking =>
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

        case GET -> Root / "check" =>
          println("---------------- The check endpoint has been hit ---------------")
          Ok(s"You've hit our check endpoint")

        case GET -> Root / "check" / specialId =>
          Ok(s"You've hit us with specialId: $specialId")

        case req @ POST -> Root / "check-special-availability" =>
          println("------------------ check-special-availability was hit")
          req.as[CheckAvailability].flatMap { sdEnquiry =>
            println(s"--------------- check-special-availability was hit with $sdEnquiry")
            for {
              rooms <- availabilityHandler.calculateAvailableRooms(sdEnquiry.specialId, sdEnquiry.startDate, sdEnquiry.endDate)
              res <- Ok(rooms)
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
