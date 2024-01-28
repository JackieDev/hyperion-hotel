package com.hyperion.hotel.handlers

import cats.Monad
import cats.implicits._
import com.hyperion.hotel.database.Store
import com.hyperion.hotel.models.{Booking, BookingForRoomType, BookingReceived, BookingResult, FailedBooking, Room, SpecialDeal}

import java.time.{LocalDate, ZonedDateTime}

class BookingHandler[F[_]: Monad, G[_]](bookingsDB: Store[F, G], generatedRooms: List[Room]) {

  // need to make sure the same room cannot be booked for any overlapping dates
  def getAllAvailableRooms(startDate: ZonedDateTime,
                           endDate: ZonedDateTime): F[List[Room]] = {
    // remove bookedRooms from generated rooms
    def removeBookedRooms(bookedRooms: List[Int]): List[Room] = {
      def go(availableRooms: List[Room], restOfGenerated: List[Room]): List[Room] =
        if (restOfGenerated.nonEmpty)
          if (bookedRooms.contains(restOfGenerated.head.id))
            go(availableRooms, restOfGenerated.tail) // drop the head (booked room)
          else
            go(availableRooms ++ List(restOfGenerated.head), restOfGenerated.tail) // add the head (available room)
        else
          availableRooms

      go(List.empty, generatedRooms)
    }

    bookingsDB.getAllBookingsForDates(startDate, endDate).map(removeBookedRooms)
  }

  private def validateDates(startDate: LocalDate, endDate: LocalDate): Option[String] = {
    if (startDate.isBefore(LocalDate.now()) || endDate.isBefore(LocalDate.now()))
      Some("Invalid dates, at least one of these dates is in the past")
    else if (startDate.isAfter(endDate))
      Some("Invalid dates, start date is after the end date")
    else
      None
  }

  def processBooking(booking: BookingReceived): F[BookingResult] = {
    validateDates(booking.startDate.toLocalDate, booking.endDate.toLocalDate) match {
      case Some(invalidDates) =>
        FailedBooking(List(invalidDates)).pure[F].widen
      case None => // all good, no date errors to report
        getAllAvailableRooms(booking.startDate, booking.endDate).flatMap {
          availableRooms =>
            if (availableRooms.exists(r => (r.id == booking.roomId) && !r.offLimits)) {
              val calculatedPrice = calculateTotalPrice(booking)
              bookingsDB.insertBooking(Booking.bookingReceivedToBooking(booking, calculatedPrice))
            } else {
              FailedBooking(List(s"Sorry, roomId: ${booking.roomId} is not available to book for the dates you've given")).pure[F].widen
            }
        }
    }
  }

  def processSpecialDealBooking(booking: BookingReceived, specialDealId: String): F[BookingResult] = {
    getAllAvailableRooms(booking.startDate, booking.endDate).flatMap {
      availableRooms =>
        if (availableRooms.exists(r => (r.id == booking.roomId) && !r.offLimits)) {
          // is special deal available for this booking?
          if (SpecialDeal.specialBookingValidator(specialDealId, booking)) {
            val specialPrice = calculateTotalPrice(booking) * (1 - SpecialDeal.getDiscountRate(specialDealId))
            bookingsDB.insertBooking(Booking.bookingReceivedToBooking(booking, specialPrice))
          } else {
            FailedBooking(List(s"Sorry, this special is not available to book with the details you've provided")).pure[F].widen
          }
        } else {
          FailedBooking(List(s"Sorry, roomId: ${booking.roomId} is not available to book for the dates you've given")).pure[F].widen
        }
    }
  }

  def cancelBooking(booking: BookingReceived): F[Boolean] = {
    for {
      // check booking exists
      exists <- bookingsDB.checkBookingExists(booking)
      // if true, cancel
      res <- exists match {
        case true => bookingsDB.cancelBooking(booking) // will return true
        case false => false.pure[F] // "The booking doesn't exist"
      }
    } yield res
  }

  private def getRoomPricePerNight(roomId: Int): Option[Double] = {
    generatedRooms.find(_.id == roomId).map(_.roomType.pricePerNight)
  }

  private def calculateTotalPrice(booking: BookingReceived): Double = {
    // get roomType to get room price per night
    // endDate (date section, not time) - startDate to get number of days
    // price will be no of days x room price per night
    getRoomPricePerNight(booking.roomId) match {
      case Some(price) => (booking.endDate.toLocalDate.toEpochDay - booking.startDate.toLocalDate.toEpochDay) * price
      case None => 0.00
    }
  }

  def getNextAvailableRoomByType(startDate: ZonedDateTime,
                                 endDate: ZonedDateTime,
                                 roomType: String): F[Option[Room]] = {
    for {
      rooms <- getAllAvailableRooms(startDate, endDate)
      roomsOfType = rooms.filter(_.roomType.name == roomType)
    } yield roomsOfType.headOption
  }

  def bookNextAvailableRoomByType(booking: BookingForRoomType): F[BookingResult] = {
    for {
      maybeRoom <- getNextAvailableRoomByType(booking.startDate, booking.endDate, booking.roomType)
      booking <- maybeRoom match {
        case Some(room) => processBooking(BookingReceived(room.id, booking.customerName, booking.startDate, booking.endDate))
        case None => FailedBooking(List(s"Sorry ${booking.customerName}, there are no rooms left of type: ${booking.roomType} for the dates you've given.")).pure[F]
      }
    } yield booking
  }

}
