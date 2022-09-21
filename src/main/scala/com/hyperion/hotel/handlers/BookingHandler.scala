package com.hyperion.hotel.handlers

import cats.Applicative
import cats.implicits._
import com.hyperion.hotel.database.Store
import com.hyperion.hotel.models.{Booking, Room}

class BookingHandler[F[_]: Applicative, G[_]](bookingsDB: Store[F, G], roomsAvailable: List[Room]) {

  def processBooking(booking: Booking): F[Boolean] = {
    if (roomsAvailable.exists(r => (r.id == booking.roomId) && !r.offLimits))
      bookingsDB.insertBooking(booking)
    else {
      println(s"--- Sorry, roomId: ${booking.roomId} is not available")
      false.pure[F]
    }
  }
}
