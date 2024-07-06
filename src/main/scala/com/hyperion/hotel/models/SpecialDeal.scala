package com.hyperion.hotel.models

import cats.implicits._
import java.time.{LocalDate, Year, ZoneId, ZonedDateTime}
import java.time.Month.{DECEMBER, FEBRUARY, JULY, MAY}
import java.time.DayOfWeek.MONDAY
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import vulcan.Codec
import vulcan.Codec._

case class SpecialDeal(id: String,
                       description: String,
                       hotelName: String = "Hyperion Hotel",
                       cityOfLocation: String = "Los Angeles",
                       totalNights: Option[Int], // only use if deal is for fixed number of nights such as Valentines and MayBankHoilday
                       discountPercentageOff: Double,
                       availableFrom: ZonedDateTime,
                       availableTo: ZonedDateTime)

object SpecialDeal {

  implicit val decode: Decoder[SpecialDeal] = deriveDecoder[SpecialDeal]
  implicit val encode: Encoder[SpecialDeal] = deriveEncoder[SpecialDeal]

  implicit val zonedDateTimeCodec: Codec[ZonedDateTime] =
    Codec.instant.imap(zdt => ZonedDateTime.ofInstant(zdt, ZoneId.of("Z")))(_.toInstant)

  implicit val codec: Codec[SpecialDeal] =
    Codec.record("SpecialDeal", "special-deal", None){ f =>
      (
        f("id", _.id),
        f("description", _.description),
        f("hotelName", _.hotelName),
        f("cityOfLocation", _.cityOfLocation),
        f("totalNights", _.totalNights),
        f("discountPercentageOff", _.discountPercentageOff),
        f("availableFrom", _.availableFrom),
        f("availableTo", _.availableTo)
        ).mapN(SpecialDeal(_,_,_,_,_,_,_,_))
    }


  // we don't care about the number of people when calculating the price

  // SpecialDeals for:
  // Valentines - 2People, 2Nights, 20% off, 14th-16th
  // MayBankHoliday - Any number of people, 3Nights, 20% off, Fri,Sat,Sun only, enforce this
  // JollyJuly - The whole month, 10% off for all bookings under this deal
  // December - The whole month 15% off all bookings under this deal, 5 nights max per booking

  val valentinesDeal = SpecialDeal(
    id = "VALENTINES",
    description = "2 Nights only, 14th-16th Feb, 20% off total booking",
    totalNights = Some(2),
    discountPercentageOff = 0.2,
    availableFrom = ZonedDateTime.of(2024, 2, 14, 15, 0, 0, 0, ZoneId.of("Z")),
    availableTo = ZonedDateTime.of(2024, 2, 16, 11, 0, 0, 0, ZoneId.of("Z")),
  )

  // need to figure out the first Mon of May each year, then work backwards to get dates for Fri, Sat and Sun
  val mayBankHoliday = SpecialDeal(
    id = "MAYBH",
    description = "3 Nights only, first May BH Fri, Sat, Sun nights only, 20% off total booking",
    totalNights = Some(3),
    discountPercentageOff = 0.2,
    availableFrom = ZonedDateTime.of(2024, 5, 1, 15, 0, 0, 0, ZoneId.of("Z")),
    availableTo = ZonedDateTime.of(2024, 5, 7, 11, 0, 0, 0, ZoneId.of("Z")),
  )

  val jollyJuly = SpecialDeal(
    id = "JJ",
    description = "10% off total booking for all of July",
    totalNights = None,
    discountPercentageOff = 0.1,
    availableFrom = ZonedDateTime.of(2024, 7, 1, 15, 0, 0, 0, ZoneId.of("Z")),
    availableTo = ZonedDateTime.of(2024, 7, 31, 11, 0, 0, 0, ZoneId.of("Z")),
  )

  val decemberDeal = SpecialDeal(
    id = "DEC",
    description = "15% off total bookings for all of December, 5 nights max per booking",
    totalNights = None,
    discountPercentageOff = 0.15,
    availableFrom = ZonedDateTime.of(2024, 12, 1, 15, 0, 0, 0, ZoneId.of("Z")),
    availableTo = ZonedDateTime.of(2024, 12, 31, 11, 0, 0, 0, ZoneId.of("Z")),
  )

  val currentDeals = List(valentinesDeal, mayBankHoliday, jollyJuly, decemberDeal)

  def getFirstMondayInMay(year: Year): LocalDate = {
    (1 to 7).map { d =>
      (year.atMonth(MAY).atDay(d))
    }.filter(_.getDayOfWeek == MONDAY).head
  }

  def specialBookingValidator(specialDealId: String, booking: BookingReceived): Boolean = {
    specialDealId match {
      case "VALENTINES" => booking.startDate.toLocalDate.getDayOfMonth == 14 &
       booking.startDate.toLocalDate.getMonth == FEBRUARY &
        booking.endDate.toLocalDate.getDayOfMonth == 16 &
        booking.endDate.toLocalDate.getMonth == FEBRUARY

      case "MAYBH" =>
        val firstMonday = getFirstMondayInMay(Year.of(booking.startDate.toLocalDate.getYear))

        booking.startDate.toLocalDate.plusDays(3) == firstMonday & // MON minus 3 days will be FRI
        booking.endDate.toLocalDate == firstMonday // MON will be the end of the stay

      case "JJ" =>
        booking.startDate.toLocalDate.getMonth == JULY &
          booking.endDate.toLocalDate.getMonth == JULY

      case "DEC" =>
        booking.startDate.toLocalDate.getMonth == DECEMBER &
        booking.endDate.toLocalDate.getMonth == DECEMBER &
          (booking.endDate.toLocalDate.getDayOfMonth -
        booking.startDate.toLocalDate.getDayOfMonth) <= 5
    }
  }

  def specialBookingValidator(specialDealId: String,
                              startDate: ZonedDateTime,
                              endDate: ZonedDateTime): Boolean = {
    specialDealId match {
      case "VALENTINES" => startDate.toLocalDate.getDayOfMonth == 14 &
        startDate.toLocalDate.getMonth == FEBRUARY &
        endDate.toLocalDate.getDayOfMonth == 16 &
        endDate.toLocalDate.getMonth == FEBRUARY

      case "MAYBH" =>
        val firstMonday = getFirstMondayInMay(Year.of(startDate.toLocalDate.getYear))

        startDate.toLocalDate.plusDays(3) == firstMonday & // MON minus 3 days will be FRI
          endDate.toLocalDate == firstMonday // MON will be the end of the stay

      case "JJ" =>
        startDate.toLocalDate.getMonth == JULY &
          endDate.toLocalDate.getMonth == JULY

      case "DEC" =>
        startDate.toLocalDate.getMonth == DECEMBER &
          endDate.toLocalDate.getMonth == DECEMBER &
          (endDate.toLocalDate.getDayOfMonth -
            startDate.toLocalDate.getDayOfMonth) <= 5
    }
  }

  def getDiscountRate(specialDealId: String): Double =
    currentDeals.find(_.id == specialDealId) match {
      case Some(special) => special.discountPercentageOff
      case None => 0
    }

  def getSpecialDeal(specialDealId: String): Option[SpecialDeal] =
    currentDeals.find(_.id == specialDealId) match {
      case Some(special) => Some(special)
      case None => None
    }

}

// What's next to think about?
/**
 * TODO
 * Will these deals be in their own db table? - maybe in future
 * The deals themselves and the SpecialBookings made by customers?
 */
