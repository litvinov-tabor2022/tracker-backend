package cz.jenda.tracker.module

import cats.effect.{ConcurrentEffect, Resource}
import com.avast.clients.rabbitmq.api.StreamedDelivery
import com.avast.clients.rabbitmq.extras.format.JsonDeliveryConverter.createJsonDeliveryConverter
import com.avast.clients.rabbitmq.{RabbitMQConnection, RabbitMQConnectionConfig, StreamingConsumerConfig}
import com.avast.metrics.scalaeffectapi.Monitor
import cz.jenda.tracker.TrackerEvent
import monix.eval.Task
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

import java.util.concurrent.ExecutorService
import javax.net.ssl.SSLContext

case class RabbitMQModule(eventsStream: fs2.Stream[Task, StreamedDelivery[Task, TrackerEvent]])

object RabbitMQModule {
  def make(config: Configuration, ex: ExecutorService)(implicit ce: ConcurrentEffect[Task]): Resource[Task, RabbitMQModule] = {
    RabbitMQConnection.make[Task](config.connection, ex, Some(SSLContext.getDefault)).flatMap { conn =>
      conn
        .newStreamingConsumer[TrackerEvent](config.consumer, Monitor.noOp())
        .map(_.deliveryStream)
        .map(RabbitMQModule(_))
    }
  }

  case class Configuration(connection: RabbitMQConnectionConfig, consumer: StreamingConsumerConfig)

  object Configuration {
    import com.avast.clients.rabbitmq.pureconfig.implicits._

    implicit val configReader: ConfigReader[Configuration] = deriveReader
  }
}
