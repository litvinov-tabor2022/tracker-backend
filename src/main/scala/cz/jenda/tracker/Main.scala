package cz.jenda.tracker

import java.util.concurrent.{ForkJoinPool, TimeUnit}

import cats.effect.{Clock, Resource}
import com.avast.sst.bundle.MonixServerApp
import com.avast.sst.doobie.DoobieHikariModule
import com.avast.sst.http4s.server.Http4sBlazeServerModule
import com.avast.sst.jvm.execution.ConfigurableThreadFactory.Config
import com.avast.sst.jvm.execution.{ConfigurableThreadFactory, ExecutorModule}
import com.avast.sst.jvm.system.console.{Console, ConsoleModule}
import com.avast.sst.pureconfig.PureConfigModule
import cz.jenda.tracker.config.Configuration
import cz.jenda.tracker.module.Http4sRoutingModule
import monix.eval.Task
import org.http4s.server.Server

import scala.concurrent.ExecutionContext

object Main extends MonixServerApp {

  def program: Resource[Task, Server] = {
    for {
      configuration <- Resource.eval(PureConfigModule.makeOrRaise[Task, Configuration])
      executorModule <- ExecutorModule.makeFromExecutionContext[Task](ExecutionContext.fromExecutor(new ForkJoinPool()))
      clock = Clock.create[Task]
      currentTime <- Resource.eval(clock.realTime(TimeUnit.MILLISECONDS))
      console <- Resource.pure[Task, Console[Task]](ConsoleModule.make[Task])
      _ <- Resource.eval(
        console.printLine(s"The current Unix epoch time is $currentTime. This system has ${executorModule.numOfCpus} CPUs.")
      )
      boundedConnectExecutionContext <-
        executorModule
          .makeThreadPoolExecutor(
            configuration.boundedConnectExecutor,
            new ConfigurableThreadFactory(Config(Some("hikari-connect-%02d")))
          )
          .map(ExecutionContext.fromExecutorService)
//      doobieTransactor <-
//        DoobieHikariModule.make[Task](configuration.database, boundedConnectExecutionContext, executorModule.blocker, None)
      routingModule = new Http4sRoutingModule()
      server <- Http4sBlazeServerModule.make[Task](configuration.server, routingModule.router, executorModule.executionContext)
    } yield server
  }

}
