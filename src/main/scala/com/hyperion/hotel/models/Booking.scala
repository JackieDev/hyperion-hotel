package com.hyperion.hotel.models

import cats.Show
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.ZonedDateTime

case class Booking(roomId: Int,
                   customerName: String,
                   startDate: ZonedDateTime,
                   endDate: ZonedDateTime)

object Booking {
  implicit val decode: Decoder[Booking] = deriveDecoder[Booking]
  implicit val encode: Encoder[Booking] = deriveEncoder[Booking]

  implicit val show: Show[Booking] = Show.show(b => s"This room is booked for: ${b.customerName} on ${b.startDate.toString}")
//  implicit val showForList: Show[List[Booking]] = Show.catsShowForList
}
