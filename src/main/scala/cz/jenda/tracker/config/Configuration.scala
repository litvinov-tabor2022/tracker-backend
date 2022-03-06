package cz.jenda.tracker.config

import com.avast.sst.doobie.DoobieHikariConfig
import com.avast.sst.doobie.pureconfig.implicits._
import com.avast.sst.http4s.server.Http4sBlazeServerConfig
import com.avast.sst.http4s.server.pureconfig.implicits._
import com.avast.sst.jvm.execution.ThreadPoolExecutorConfig
import com.avast.sst.jvm.pureconfig.implicits._
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

import scala.concurrent.duration.FiniteDuration

final case class Configuration(
    server: Http4sBlazeServerConfig,
    database: DoobieHikariConfig,
    boundedConnectExecutor: ThreadPoolExecutorConfig,
    mqtt: MqttConfiguration
)

object Configuration {
  implicit val reader: ConfigReader[Configuration] = deriveReader
}

final case class MqttConfiguration(
    host: String,
    port: Int,
    ssl: Boolean,
    user: Option[String],
    pass: Option[String],
    topic: String,
    subscriberName: String,
    readTimeout: FiniteDuration,
    connectionRetries: Int,
    keepAliveSecs: Int
)

object MqttConfiguration {
  implicit val reader: ConfigReader[MqttConfiguration] = deriveReader
}
