package com.hyperion.hotel.models

import java.time.{LocalDate, Year, ZoneId, ZonedDateTime}
import java.time.Month.{DECEMBER, FEBRUARY, JULY, MAY}
import java.time.DayOfWeek.MONDAY

case class SpecialDeal(id: String,
                       description: String,
                       totalNights: Option[Int], // only use if deal is for fixed number of nights such as Valentines and MayBankHoilday
                       discountPercentageOff: Double,
                       availableFrom: ZonedDateTime,
                       availableTo: ZonedDateTime)

object SpecialDeal {
  // we don't care about the number of people when calculating the price, we don't rip people off here unnecessarily

  // SpecialDeals for:
  // Valentines - 2People, 2Nights, 20% off, 14th-16th
  // MayBankHoliday - Any number of people, 3Nights, 20% off, Fri,Sat,Sun only, enforce this
  // JollyJuly - The whole month, 10% off for all bookings under this deal
  // December - The whole month 15% off all bookings under this deal, 5 nights max per booking

  val valentinesDeal = SpecialDeal("VALENTINES", "2 Nights only, 14th-16th Feb, 20% off total booking", Some(2), 0.2,
    ZonedDateTime.of(2024, 2, 14, 15, 0, 0, 0, ZoneId.of("Z")),
    ZonedDateTime.of(2024, 2, 16, 11, 0, 0, 0, ZoneId.of("Z")),
  )

  // need to figure out the first Mon of May each year, then work backwards to get dates for Fri, Sat and Sun
  val mayBankHoliday = SpecialDeal("MAYBH", "3 Nights only, first May BH Fri, Sat, Sun nights only, 20% off total booking", Some(3), 0.2,
    ZonedDateTime.of(2024, 5, 1, 15, 0, 0, 0, ZoneId.of("Z")),
    ZonedDateTime.of(2024, 5, 7, 11, 0, 0, 0, ZoneId.of("Z")),
  )

  val jollyJuly = SpecialDeal("JJ", "10% off total booking for all of July", None, 0.1,
    ZonedDateTime.of(2024, 7, 1, 15, 0, 0, 0, ZoneId.of("Z")),
    ZonedDateTime.of(2024, 7, 31, 11, 0, 0, 0, ZoneId.of("Z")),
  )

  val decemberDeal = SpecialDeal("DEC", "15% off total bookings for all of December, 5 nights max per booking", None, 0.15,
    ZonedDateTime.of(2024, 12, 1, 15, 0, 0, 0, ZoneId.of("Z")),
    ZonedDateTime.of(2024, 12, 31, 11, 0, 0, 0, ZoneId.of("Z")),
  )

  val currentDeals = List(valentinesDeal, mayBankHoliday, jollyJuly, decemberDeal)

  def getFirstMondayInMay(year: Year): LocalDate = {
    (1 to 7).map { d =>
      (year.atMonth(MAY).atDay(d))
    }.filter(_.getDayOfWeek == MONDAY).head
  }

  def specialBookingValidator(specialDealId: String, booking: Booking): Boolean = {
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

  def getDiscountRate(specialDealId: String): Double =
    currentDeals.find(_.id == specialDealId) match {
      case Some(special) => special.discountPercentageOff
      case None => 0
    }

}

// What's next to think about?
/**
 * TODO
 * Will these deals be in their own db table? - maybe in future
 * The deals themselves and the SpecialBookings made by customers?
 */
