package com.hyperion.hotel.models

sealed trait BookingResult

case object SuccessfulBooking extends BookingResult
case class SuccessfulBookingMade(roomId: Int) extends BookingResult
case class FailedBooking(errors: List[String]) extends BookingResult


