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
         | insert into bookings (room_id, customer_name, start_date, end_date, added_on) values (${booking.roomId},
         | ${booking.customerName}, ${booking.startDate}, ${booking.endDate}, now())
         | on conflict (room_id, start_date)
         | do nothing""".stripMargin
      .update

  def getBookings(roomId: Int): ConnectionIO[List[Booking]] =
    sql"""
         | select room_id, customer_name, start_date, end_date from bookings where
         | room_id=$roomId""".stripMargin
      .query[Booking].to[List]

}
