package com.hyperion.hotel.models

import java.time.ZonedDateTime

case class Booking(roomId: Int,
                   customerName: String,
                   startDate: ZonedDateTime,
                   endDate: ZonedDateTime)
