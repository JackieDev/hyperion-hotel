package com.hyperion.hotel.database

import cats.Show

import java.time.{ZoneOffset, ZonedDateTime}
import com.hyperion.hotel.models._
import doobie._
import doobie.implicits._
import doobie.implicits.javasql.TimestampMeta
import org.postgresql.util.PGobject

import java.sql.Timestamp

object SQLQueries {

  implicit val han: LogHandler = LogHandler.nop

  implicit val metaInstance: Meta[ZonedDateTime] = Meta[Timestamp]
    .imap(ts => ZonedDateTime.ofInstant(ts.toInstant, ZoneOffset.UTC))(zdt => Timestamp.from(zdt.toInstant))

  implicit val pgObjectShow: Show[PGobject] = _.toString

  def insertBooking(booking: Booking): Update0 =
    sql"""
         | insert into bookings (room_id, customer_name, start_date, end_date, total_price, added_on) values (${booking.roomId},
         | ${booking.customerName}, ${booking.startDate}, ${booking.endDate}, ${booking.totalPrice}, now())
         | on conflict (room_id, start_date)
         | do nothing""".stripMargin
      .update

  def cancelBooking(booking: BookingReceived): Update0 =
    sql"""
         | delete from bookings where room_id=${booking.roomId} AND
         | customer_name=${booking.customerName} AND
         | start_date=${booking.startDate} AND
         | end_date=${booking.endDate}""".stripMargin
      .update

  def getBookings(roomId: Int): ConnectionIO[List[Booking]] =
    sql"""
         | select room_id, customer_name, start_date, end_date, total_price from bookings where
         | room_id=$roomId""".stripMargin
      .query[Booking].to[List]

  // calculate overlapping date ranges
  // startDate1 < endDate2 && startDate2 < endDate1
  /*
    An example - non overlapping
    startDate1 = 1 AUG 2024
    endDate1 = 6 AUG 2024

    startDate2 = 1 JUL 2024
    endDate2 = 8 JUL 2024

    Is startDate1 < endDate2? No it's not
    Is startDate2 < endDate1? Yes

    answer is false as we need both to be true to have overlapping dates

    Another example - overlapping
    startDate1 = 1 AUG 2024
    endDate1 = 6 AUG 2024

    startDate2 = 4 AUG 2024
    endDate2 = 8 AUG 2024

    Is startDate1 < endDate2? Yes
    Is startDate2 < endDate1? Yes

    answer is true, this booking attempt should be rejected as room is already taken
   */
  def getAllBookingsForDates(startDate: ZonedDateTime,
                             endDate: ZonedDateTime): ConnectionIO[List[Int]] =
    sql"""
         | select room_id from bookings
         | where start_date < $endDate
         | AND $startDate < end_date
         """.stripMargin
      .query[Int].to[List]

  def getAllBookingsForCustomer(name: String): ConnectionIO[List[Booking]] =
    sql"""
         | select room_id, customer_name, start_date, end_date, total_price from bookings where
         | customer_name=$name""".stripMargin
      .query[Booking].to[List]

  def checkBookingExists(booking: BookingReceived): ConnectionIO[List[Booking]] =
    sql"""
         | select * from bookings where room_id=${booking.roomId} AND
         | customer_name=${booking.customerName} AND
         | start_date=${booking.startDate} AND
         | end_date=${booking.endDate}""".stripMargin
      .query[Booking].to[List]

  def removeExpiredBookings(): Update0 =
    sql"""
         | delete from bookings where end_date <= now()""".stripMargin
      .update

}
