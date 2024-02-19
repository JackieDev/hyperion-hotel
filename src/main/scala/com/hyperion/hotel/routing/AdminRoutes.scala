package com.hyperion.hotel.routing

import cats.{Applicative, Defer}
import cats.implicits._
import com.hyperion.hotel.handlers.SpecialDealHandler
import com.hyperion.hotel.models.SpecialDeal
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

class AdminRoutes[F[_]: Applicative: Defer](specialDealHandler: SpecialDealHandler[F]) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = {

    HttpRoutes
      .of[F] {

        case (GET | HEAD) -> Root / "admin" / "publish-deal" / specialId =>
          SpecialDeal.getSpecialDeal(specialId) match {
            case Some(deal) =>
              specialDealHandler.publish(specialId, deal) *>
                Ok(s"Special deal with id $specialId was published to the kafka topic")
            case None =>
              Ok(s"There were no special deals with id $specialId available to publish")
          }
      }
  }

}
