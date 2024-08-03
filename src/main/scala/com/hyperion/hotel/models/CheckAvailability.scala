package com.hyperion.hotel.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.ZonedDateTime

case class CheckAvailability(specialId: String,
                             startDate: ZonedDateTime,
                             endDate: ZonedDateTime)

object CheckAvailability {
  implicit val decode: Decoder[CheckAvailability] = deriveDecoder[CheckAvailability]
  implicit val encode: Encoder[CheckAvailability] = deriveEncoder[CheckAvailability]
}
