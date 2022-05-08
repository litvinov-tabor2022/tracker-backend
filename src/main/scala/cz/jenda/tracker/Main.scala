package cz.jenda.tracker

import cats.effect.{Clock, Resource}
import com.avast.sst.bundle.MonixServerApp
import com.avast.sst.doobie.DoobieHikariModule
import com.avast.sst.http4s.server.Http4sBlazeServerModule
import com.avast.sst.jvm.execution.ConfigurableThreadFactory.Config
import com.avast.sst.jvm.execution.{ConfigurableThreadFactory, ExecutorModule, ThreadPoolExecutorConfig}
import com.avast.sst.jvm.system.console.{Console, ConsoleModule}
import com.avast.sst.pureconfig.PureConfigModule
import cz.jenda.tracker.config.Configuration
import cz.jenda.tracker.module.{Http4sRoutingModule, RabbitMQModule}
import monix.eval.Task
import org.http4s.server.Server
import org.http4s.websocket.WebSocketFrame
import org.typelevel.ci.CIString

import java.util.concurrent.{ForkJoinPool, TimeUnit}
import scala.concurrent.ExecutionContext

object Main extends MonixServerApp {

  def program: Resource[Task, Server] = {
    for {
      config <- Resource.eval(PureConfigModule.makeOrRaise[Task, Configuration])
      executorModule <- ExecutorModule.makeFromExecutionContext[Task](ExecutionContext.fromExecutor(new ForkJoinPool()))
      clock = Clock.create[Task]
      currentTime <- Resource.eval(clock.realTime(TimeUnit.MILLISECONDS))
      console <- Resource.pure[Task, Console[Task]](ConsoleModule.make[Task])
      _ <- Resource.eval(
        console.printLine(s"The current Unix epoch time is $currentTime. This system has ${executorModule.numOfCpus} CPUs.")
      )
      boundedConnectExecutionContext <- {
        executorModule
          .makeThreadPoolExecutor(config.boundedConnectExecutor, new ConfigurableThreadFactory(Config(Some("hikari-connect-%02d"))))
          .map(ExecutionContext.fromExecutorService)
      }
      doobieTransactor <- DoobieHikariModule.make[Task](config.database, boundedConnectExecutionContext, executorModule.blocker, None)
      dao = new Dao(doobieTransactor)
      subscriptions = new Subscriptions
      logic = new EventsLogic(dao, subscriptions)

      rex <- executorModule.makeThreadPoolExecutor(
        ThreadPoolExecutorConfig(1, 4),
        new ConfigurableThreadFactory(Config(Some("rabbitmq-%02d")))
      )
      rabbit <- RabbitMQModule.make(config.rabbitmq, rex)

      wsQueue <- Resource.eval(fs2.concurrent.Queue.unbounded[Task, WebSocketFrame])
      analytics = new Analytics(dao)
      allowedOrigins = config.allowedOrigins.map(CIString(_))
      routingModule = new Http4sRoutingModule(dao, analytics, allowedOrigins, wsQueue, subscriptions, executorModule.blocker)
      server <- Http4sBlazeServerModule.make[Task](config.server, routingModule.router, executorModule.executionContext)
      _ <- Resource.eval {
        rabbit.eventsStream.evalMap(logic.saveEvent).compile.drain
      }
    } yield server
  }

}
