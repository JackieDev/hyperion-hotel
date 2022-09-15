package com.hyperion.hotel.models

case class Hotel(name: String, floors: Int, roomsPerFloor: Int, offLimitRooms: List[Room])
