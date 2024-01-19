package com.hyperion.hotel.models

import java.time.ZonedDateTime

case class SpecialDeal(name: String,
                       roomType: RoomType,
                       totalNights: Int,
                       totalPeople: Int,
                       pricePerNight: Double,
                       availableFrom: ZonedDateTime,
                       availableTo: ZonedDateTime)

object SpecialDeal {
  // use this totalPrice in booking
  // we don't care about the number of people when calculating the price
  def totalPrice(totalNights: Int, pricePerNight: Double): Double =
    totalNights * pricePerNight

  // SpecialDeals for:
  // Valentines - 2People, 2Nights, 20% off, 14th-16th
  // MayBankHoliday - Any number of people, 3Nights, 20% off, Fri,Sat,Sun only, enforce this
  // JollyJuly - The whole month, 10% off for all bookings under this deal
  // December - The whole month 15% off all bookings under this deal
}
