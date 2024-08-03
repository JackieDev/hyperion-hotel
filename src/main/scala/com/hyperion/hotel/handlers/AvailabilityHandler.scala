package com.hyperion.hotel.handlers

import cats.Applicative
import cats.effect._
import cats.implicits._
import com.hyperion.hotel.database.Store
import com.hyperion.hotel.models.{AvailableRooms, Room, SpecialDeal}

import java.time.ZonedDateTime


class AvailabilityHandler[F[_]: Applicative, G[_]](bookingsDB: Store[F, G],
                                                   generatedRooms: List[Room]) {

  def calculateAvailableRooms(specialId: String,
                              startDate: ZonedDateTime,
                              endDate: ZonedDateTime
                             ): F[Option[AvailableRooms]] = {

    if (SpecialDeal.specialBookingValidator(specialId, startDate, endDate)) {
      println(s"-------------- special booking passed validation")
      for {
        takenRoomIds <- bookingsDB.getAllBookingsForDates(startDate, endDate)
        roomsAvailable = generatedRooms.filter(generatedRoomId => !takenRoomIds.contains(generatedRoomId.id))
      } yield Some(AvailableRooms.convertToAvailableRooms(roomsAvailable, SpecialDeal.getDiscountRate(specialId)))

    } else
      println(s"-------------- special booking didn't pass validation")
      none[AvailableRooms].pure[F]
  }

}
