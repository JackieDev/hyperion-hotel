package com.hyperion.hotel.config

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}

import pureconfig.generic.auto._

final case class ServiceConfig(hyperionHotel: HyperionHotel)

final case class HyperionHotel(db: DatabaseConfig, httpd: HttpdConfig)

final case class DatabaseConfig(url: String,
                                username: String,
                                password: String,
                                driver: String,
                                maxConnectionPoolSize: Int,
                                maxAge: Int
                               )

final case class HttpdConfig(host: String, port: Int)


object ServiceConfig {
  import pureconfig._

  val loadApplicationConfig: IO[(ServiceConfig, Config)] =
    for {
      config        <- IO(ConfigFactory.load())
      serviceConfig <- IO(ConfigSource.fromConfig(config).withFallback(ConfigSource.default).loadOrThrow[ServiceConfig])
    } yield (serviceConfig, config)

}
