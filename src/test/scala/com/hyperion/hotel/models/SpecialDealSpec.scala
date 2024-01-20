package com.hyperion.hotel.models

import java.time.{ZoneId, ZonedDateTime}
import org.scalatest.flatspec._
import org.scalatest.matchers.should._

class SpecialDealSpec extends AnyFlatSpec with Matchers {

  // check behaviour of specialBookingValidator

  "a Valentines stay with the correct dates" should "pass validation" in {
    val testValentineBooking = Booking(110,
      "Jackie",
      ZonedDateTime.of(2024, 2, 14, 15, 0, 0, 0, ZoneId.of("Z")),
      ZonedDateTime.of(2024, 2, 16, 15, 0, 0, 0, ZoneId.of("Z")),
      0.00 // This should get calculated later on
    )

    val result = SpecialDeal.specialBookingValidator("VALENTINES", testValentineBooking)
    assert(result) // true
  }


  "a Valentines stay with the wrong dates" should "not pass validation" in {
    val testValentineBooking = Booking(110,
      "Jackie",
      ZonedDateTime.of(2024, 2, 15, 15, 0, 0, 0, ZoneId.of("Z")),
      ZonedDateTime.of(2024, 2, 16, 15, 0, 0, 0, ZoneId.of("Z")),
      0.00 // This should get calculated later on
    )

    val result = SpecialDeal.specialBookingValidator("VALENTINES", testValentineBooking)
    assert(!result) // false
  }


  "a booking made for December for 5 days" should "pass validation" in {
    val testDecBooking = Booking(110,
      "Jackie",
      ZonedDateTime.of(2024, 12, 14, 15, 0, 0, 0, ZoneId.of("Z")),
      ZonedDateTime.of(2024, 12, 19, 15, 0, 0, 0, ZoneId.of("Z")),
      0.00 // This should get calculated later on
    )

    val result = SpecialDeal.specialBookingValidator("DEC", testDecBooking)
    assert(result) // true
  }


  "a booking made for December for more than 5 days" should "not pass validation" in {
    val testDecBooking = Booking(110,
      "Jackie",
      ZonedDateTime.of(2024, 12, 10, 15, 0, 0, 0, ZoneId.of("Z")),
      ZonedDateTime.of(2024, 12, 16, 15, 0, 0, 0, ZoneId.of("Z")),
      0.00 // This should get calculated later on
    )

    val result = SpecialDeal.specialBookingValidator("DEC", testDecBooking)
    assert(!result) // false
  }


  "discounted price" should "be calculated correctly for a 5 day December booking" in {
    val test5DayDecBooking = Booking(110,
      "Jackie",
      ZonedDateTime.of(2024, 12, 10, 15, 0, 0, 0, ZoneId.of("Z")),
      ZonedDateTime.of(2024, 12, 15, 15, 0, 0, 0, ZoneId.of("Z")),
      0.00 // This should get calculated later on
    )

    val originalPricePerNight = List(Room(110, false, Standard)).map(_.roomType.pricePerNight).head // 70.00
    val originalTotalPrice = (test5DayDecBooking.endDate.toLocalDate.toEpochDay - test5DayDecBooking.startDate.toLocalDate.toEpochDay) * originalPricePerNight
    // 5 * 70 = 350

    val specialPrice = originalTotalPrice * (1 - SpecialDeal.getDiscountRate("DEC"))
    // 350 * 0.85 = 297.5

    assert(specialPrice == 297.5)
  }

}
