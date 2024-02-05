package com.hyperion.hotel.handlers

import cats.effect._
import com.hyperion.hotel.models.SpecialDeal

class SpecialDealHandler[F[_]: Sync](kafkaProducer: (String, SpecialDeal) => F[Unit]) {

  def publish(key: String, message: SpecialDeal): F[Unit] =
    kafkaProducer(key, message)

}
