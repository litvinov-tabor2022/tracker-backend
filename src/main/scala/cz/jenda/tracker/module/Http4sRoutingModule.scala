package cz.jenda.tracker.module

import com.avast.sst.http4s.server.Http4sRouting
import cz.jenda.tracker.Dao
import io.circe.syntax._
import monix.eval.Task
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpApp, HttpRoutes}

class Http4sRoutingModule(dao: Dao) extends Http4sDsl[Task] {

  private val routes = HttpRoutes.of[Task] {
    case GET -> Root / "status"                   => Ok("OK")
    case GET -> Root / "list" / IntVar(trackerId) => dao.listFor(trackerId).map(_.asJson).flatMap(Ok(_))
  }

  val router: HttpApp[Task] = Http4sRouting.make {
    routes
  }

}
