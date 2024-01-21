package com.hyperion.hotel.models

import cats.Show
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.ZonedDateTime

case class Booking(roomId: Int,
                   customerName: String,
                   startDate: ZonedDateTime,
                   endDate: ZonedDateTime,
                   totalPrice: Double)

case class JustDates(startDate: ZonedDateTime,
                     endDate: ZonedDateTime
                    )

case class BookingReceived(roomId: Int,
                           customerName: String,
                           startDate: ZonedDateTime,
                           endDate: ZonedDateTime
                          )

object Booking {
  implicit val decode: Decoder[Booking] = deriveDecoder[Booking]
  implicit val encode: Encoder[Booking] = deriveEncoder[Booking]

  implicit val show: Show[Booking] =
    Show.show(b => s"This room is booked for: ${b.customerName} from ${b.startDate.toString} until ${b.endDate.toString}")

  def bookingReceivedToBooking(bookingReceived: BookingReceived, totalPrice: Double): Booking =
    Booking(bookingReceived.roomId,
      bookingReceived.customerName,
      bookingReceived.startDate,
      bookingReceived.endDate,
      totalPrice
    )
}

object JustDates {
  implicit val decodeDates: Decoder[JustDates] = deriveDecoder[JustDates]
  implicit val encodeDates: Encoder[JustDates] = deriveEncoder[JustDates]
}

object BookingReceived {
  implicit val decode: Decoder[BookingReceived] = deriveDecoder[BookingReceived]
  implicit val encode: Encoder[BookingReceived] = deriveEncoder[BookingReceived]
}