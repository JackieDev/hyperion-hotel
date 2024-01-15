package com.hyperion.hotel.handlers

import cats.Monad
import cats.implicits._
import com.hyperion.hotel.database.Store
import com.hyperion.hotel.models.{Booking, Room}

import java.time.ZonedDateTime

class BookingHandler[F[_]: Monad, G[_]](bookingsDB: Store[F, G], generatedRooms: List[Room]) {

  // need to make sure the same room cannot be booked for any overlapping dates
  def getAllAvailableRooms(startDate: ZonedDateTime,
                           endDate: ZonedDateTime): F[List[Room]] = {
    // remove bookedRooms from generated rooms
    def removeBookedRooms(bookedRooms: List[Int]): List[Room] = {
      def go(availableRooms: List[Room], restOfGenerated: List[Room]): List[Room] =
        if (restOfGenerated.nonEmpty)
          if (bookedRooms.contains(restOfGenerated.head.id))
            go(availableRooms, restOfGenerated.tail) // drop the head (booked room)
          else
            go(availableRooms ++ List(restOfGenerated.head), restOfGenerated.tail) // add the head (available room)
        else
          availableRooms

      go(List.empty, generatedRooms)
    }
    bookingsDB.getAllBookingsForDates(startDate, endDate).map(removeBookedRooms)
  }

  def processBooking(booking: Booking): F[Boolean] = {
    getAllAvailableRooms(booking.startDate, booking.endDate).flatMap {
      availableRooms =>
        println(s"---------- availableRooms: ${availableRooms.map(_.id)}")
        if (availableRooms.exists(r => (r.id == booking.roomId) && !r.offLimits)) {
          bookingsDB.insertBooking(booking)
        } else {
          println(s"--- Sorry, roomId: ${booking.roomId} is not available to book for the dates you've chosen")
          false.pure[F]
        }
    }
  }

  //TO-DO
  def cancelBooking(booking: Booking): F[Boolean] = {
    for {
      // check booking exists
      exists <- bookingsDB.checkBookingExists(booking)
      // if true, cancel
      res <- exists match {
        case true => bookingsDB.cancelBooking(booking) // will return true
        case false => false.pure[F] // "The booking doesn't exist"
      }
    } yield res

    // if true, cancel
    // return a response

  }

}
