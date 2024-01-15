package com.hyperion.hotel.models

case class Room(id: Int, offLimits: Boolean)

object Room {
  def createRoom(id: Int): Room = Room(id, false)
}
