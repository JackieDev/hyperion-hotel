package com.hyperion.hotel.producers

import cats.effect._
import cats.implicits._
import com.hyperion.hotel.models.SpecialDeal
import fs2.kafka.vulcan.{Auth, AvroSerializer, AvroSettings, SchemaRegistryClientSettings}
import fs2.kafka._
import fs2.kafka.ProducerSettings

object SpecialDealProducer {
  import com.hyperion.hotel.models.SpecialDeal._

  val topic = "special-deals"

  def sendMessage(key: String,
                  message: SpecialDeal,
                  producer: KafkaProducer[IO, String, SpecialDeal]
                 ): IO[Unit] =
    producer.produce(ProducerRecords.one(ProducerRecord(topic, key, message))).flatten.void

  def apply[F[_]: ConcurrentEffect: ContextShift](): fs2.Stream[F, KafkaProducer[F, String, SpecialDeal]] = {

    val avroSettings = AvroSettings[F](
      SchemaRegistryClientSettings[F]("http://0.0.0.0:8081")
        .withAuth(Auth.Basic("username", "password")))

    implicit val serializer: RecordSerializer[F, SpecialDeal] =
      AvroSerializer[SpecialDeal].using(avroSettings)

    val producerSettings =
      ProducerSettings[F, String, SpecialDeal](
        keySerializer = Serializer[F, String],
        valueSerializer = serializer
      ).withBootstrapServers("localhost:9092")

    KafkaProducer
      .stream(producerSettings)
  }

}
