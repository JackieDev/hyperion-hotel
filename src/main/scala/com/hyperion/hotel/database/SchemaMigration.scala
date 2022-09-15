package com.hyperion.hotel.database

import cats.effect.Sync
import cats.syntax.all._
import com.hyperion.hotel.config.DatabaseConfig
import com.typesafe.scalalogging.Logger
import org.flywaydb.core.Flyway

import scala.util.control.NonFatal

object SchemaMigration {
  def apply[F[_]: Sync](dbConfig: DatabaseConfig): F[Unit] = {
    val logger: Logger = Logger(getClass)

    Sync[F]
      .delay {
        val flyway =
          Flyway
            .configure()
            .dataSource(dbConfig.url, dbConfig.username, dbConfig.password)
            .table("schema_version")
            .baselineOnMigrate(true)
            .baselineVersion("0")

        flyway.load().migrate().migrationsExecuted
      }
      .flatMap { migrationsApplied =>
        logger.info(s"Successfully applied $migrationsApplied migrations to the 'hyperion-hotel' database").pure[F]
      }
      .void
      .onError { case NonFatal(t) =>
        logger.error(s"Unable to run Flyway migration with error: ${t.getMessage}").pure[F]
      }
  }

}
