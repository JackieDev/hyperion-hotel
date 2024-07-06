package com.hyperion.hotel.models

sealed trait RoomType {
  def name: String
  def pricePerNight: Double
  def discountedPricePerNight(discount: Double): Double = (1 - discount) * pricePerNight
}

case object Standard extends RoomType {
  val name: String = "Standard"
  val pricePerNight: Double = 70.00
}

case object Deluxe extends RoomType {
  val name: String = "Deluxe"
  val pricePerNight: Double = 100.00
}

case object Suite extends RoomType {
  val name: String = "Suite"
  val pricePerNight: Double = 190.00
}

case class Room(id: Int, offLimits: Boolean, roomType: RoomType)

object Room {
  def createStandardRoom(id: Int): Room = Room(id, false, Standard)
  def createDeluxeRoom(id: Int): Room = Room(id, false, Deluxe)
  def createSuiteRoom(id: Int): Room = Room(id, false, Suite)
}
