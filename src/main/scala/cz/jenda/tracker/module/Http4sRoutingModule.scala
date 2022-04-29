package cz.jenda.tracker.module

import cats.effect.Blocker
import com.avast.sst.http4s.server.Http4sRouting
import cz.jenda.tracker.{Dao, GpxGenerator}
import io.circe.syntax._
import monix.eval.Task
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{Header, HttpApp, HttpRoutes, Response}
import org.typelevel.log4cats.slf4j.Slf4jLogger

class Http4sRoutingModule(dao: Dao, blocker: Blocker) extends Http4sDsl[Task] {
  private val logger = Slf4jLogger.getLoggerFromClass[Task](classOf[Http4sRoutingModule])

  private val routes = HttpRoutes.of[Task] {
    case GET -> Root / "status" => Ok("OK")

    case GET -> Root / "tracks-list" =>
      logger.info(s"Listing tracks")
      dao
        .listTracks()
        .flatMap(Ok(_))
        .map(
          _.withHeaders(
            Header.ToRaw.keyValuesToRaw(("Content-Type", "application/json")),
            Header.ToRaw.keyValuesToRaw(("Access-Control-Allow-Origin", "*"))
          )
        )

    case GET -> Root / "list" / "json" / IntVar(trackId) =>
      logger.info(s"Listing positions (as JSON) for track ID $trackId") >>
        dao
          .listCoordinatesFor(trackId)
          .compile
          .toList
          .map(_.asJson)
          .flatMap(Ok(_))
          .map(
            _.withHeaders(
              Header.ToRaw.keyValuesToRaw(("Content-Type", "application/json")),
              Header.ToRaw.keyValuesToRaw(("Access-Control-Allow-Origin", "*"))
            )
          )

    case GET -> Root / "list" / "gpx" / IntVar(trackId) =>
      logger.info(s"Listing positions (as GPX) for tracker ID $trackId") >>
        dao.getTrack(trackId).flatMap {
          case Some(track) =>
            dao.listWaypointsFor(trackId).flatMap { waypoints =>
              Ok(dao.listCoordinatesFor(trackId).through(GpxGenerator.createGpx(track, waypoints)))
                .map(
                  _.withHeaders(
                    Header.ToRaw.keyValuesToRaw(("Content-Type", "application/gpx+xml")),
                    Header.ToRaw.keyValuesToRaw(("Access-Control-Allow-Origin", "*"))
                  )
                )
            }

          case None => NotFound(s"Track ID $trackId not found")
        }

    case GET -> Root            => streamResource("index.html")
    case GET -> Root / resource => streamResource(resource)

  }

  private def streamResource(name: String): Task[Response[Task]] = {
    Task(getClass.getClassLoader.getResourceAsStream(s"frontend/$name")).map(Option(_)).flatMap {
      case Some(is) =>
        Ok(fs2.io.readInputStream(Task.now(is), 4096, blocker))

      case None => NotFound(s"Resource '$name' not found")
    }
  }

  val router: HttpApp[Task] = Http4sRouting.make {
    routes
  }

}
