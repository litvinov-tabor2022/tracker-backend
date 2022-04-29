package cz.jenda.tracker.config

import com.avast.sst.doobie.DoobieHikariConfig
import com.avast.sst.doobie.pureconfig.implicits._
import com.avast.sst.http4s.server.Http4sBlazeServerConfig
import com.avast.sst.http4s.server.pureconfig.implicits._
import com.avast.sst.jvm.execution.ThreadPoolExecutorConfig
import com.avast.sst.jvm.pureconfig.implicits._
import cz.jenda.tracker.module.RabbitMQModule
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

final case class Configuration(
    server: Http4sBlazeServerConfig,
    database: DoobieHikariConfig,
    boundedConnectExecutor: ThreadPoolExecutorConfig,
    rabbitmq: RabbitMQModule.Configuration,
    allowedOrigins: List[String]
)

object Configuration {
  implicit val reader: ConfigReader[Configuration] = deriveReader
}
