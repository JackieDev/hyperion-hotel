package com.hyperion.hotel.models

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
}
