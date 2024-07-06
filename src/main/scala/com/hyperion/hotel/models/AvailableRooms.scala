package com.hyperion.hotel.models

import io.circe.{Decoder, Encoder, HCursor }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class AvailableRoom(roomTypeName: String,
                         currentlyAvailable: Int,
                         discountedPricePerNight: Option[Double])

case class AvailableRooms(availableRooms: Set[AvailableRoom])

object AvailableRooms {

  implicit val decodeAR: Decoder[AvailableRoom] = deriveDecoder[AvailableRoom]
  implicit val encodeAR: Encoder[AvailableRoom] = deriveEncoder[AvailableRoom]

  implicit val decodeARs: Decoder[AvailableRooms] = new Decoder[AvailableRooms] {
    final def apply(c: HCursor): Decoder.Result[AvailableRooms] =
      for {
        availableRooms <- c.downField("availableRooms").as[Set[AvailableRoom]]
      } yield {
        new AvailableRooms(availableRooms)
      }
  }

  implicit val encodeARs: Encoder[AvailableRooms] = deriveEncoder[AvailableRooms]


  def convertToAvailableRooms(rooms: List[Room], discountRate: Double): AvailableRooms = {
    val availableRooms = rooms.groupBy(_.roomType)
      .map(kv => AvailableRoom(kv._1.name, kv._2.size, Some(kv._1.discountedPricePerNight(discountRate))))
      .toSet

    AvailableRooms(availableRooms)
  }
}