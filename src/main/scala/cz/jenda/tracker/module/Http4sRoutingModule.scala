package cz.jenda.tracker.module

import cats.effect.Blocker
import com.avast.sst.http4s.server.Http4sRouting
import cz.jenda.tracker.Subscription.ForCoordinates
import cz.jenda.tracker.{Analytics, Dao, GpxGenerator, Subscriptions}
import fs2.Pipe
import fs2.text.utf8Encode
import io.circe.syntax._
import monix.eval.Task
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CORS
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{Header, HttpApp, HttpRoutes, Response}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.slf4j.Slf4jLogger

class Http4sRoutingModule(
    dao: Dao,
    analytics: Analytics,
    allowedOrigins: List[CIString],
    wsQueue: fs2.concurrent.Queue[Task, WebSocketFrame],
    subscriptions: Subscriptions,
    blocker: Blocker
) extends Http4sDsl[Task] {
  private val logger = Slf4jLogger.getLoggerFromClass[Task](classOf[Http4sRoutingModule])

  private val routes = HttpRoutes.of[Task] {
    case GET -> Root / "status" => Ok("OK")

    case GET -> Root / "tracks-list" =>
      logger.debug(s"Listing tracks") >>
        dao
          .listTracks()
          .flatMap(Ok(_))
          .map(
            _.withHeaders(
              Header.ToRaw.keyValuesToRaw(("Content-Type", "application/json"))
            )
          )

    case GET -> Root / "trackers-list" =>
      logger.debug(s"Listing trackers") >>
        dao
          .listTrackers()
          .flatMap(Ok(_))
          .map(
            _.withHeaders(
              Header.ToRaw.keyValuesToRaw(("Content-Type", "application/json"))
            )
          )

    case GET -> Root / "track-assign" / IntVar(trackerId) / IntVar(trackId) =>
      logger.info(s"Assigning track ID $trackId to tracker ID $trackerId") >>
        dao.assignCurrentTrack(trackerId, trackId) >>
        Ok()

    case GET -> Root / "track-create" / IntVar(trackerId) / name =>
      logger.info(s"Creating track '$name' for tracker ID $trackerId") >>
        dao.createTrack(trackerId, name) >>
        Ok()

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
              Header.ToRaw.keyValuesToRaw(("Content-Type", "application/json"))
            )
          )

    case GET -> Root / "list" / "gpx" / IntVar(trackId) =>
      logger.info(s"Listing positions (as GPX) for track ID $trackId") >>
        dao.getTrack(trackId).flatMap {
          case Some(track) =>
            dao.listWaypointsFor(trackId).flatMap { waypoints =>
              Ok(dao.listCoordinatesFor(trackId).through(GpxGenerator.createGpx(track, waypoints)))
                .map(
                  _.withHeaders(
                    Header.ToRaw.keyValuesToRaw(("Content-Type", "application/gpx+xml"))
                  )
                )
            }

          case None => NotFound(s"Track ID $trackId not found")
        }

    case GET -> Root / "subscribe" =>
      logger.info(s"Subscribing for updates") >>
        WebSocketBuilder[Task].build(wsQueue.dequeue, subscribe)

    case GET -> Root / "analyze" / IntVar(trackId) =>
      logger.info(s"Analyzing track ID $trackId") >>
        Ok(analytics.analyze(trackId).through(utf8Encode)).map(
          _.withHeaders(
            Header.ToRaw.keyValuesToRaw(("Content-Type", "text/plain; charset=utf-8"))
          )
        )

    case GET -> Root            => streamResource("index.html")
    case GET -> Root / resource => streamResource(resource)
  }

  @SuppressWarnings(Array("DisableSyntax.=="))
  private val subscribe: Pipe[Task, WebSocketFrame, Unit] = {
    _.evalMap {
      case Text(str, _) =>
        str.trim.split("/") match {
          case Array(cmd, arg) if cmd == "coordinates" =>
            val trackId = arg.toInt

            logger.info(s"Subscribing for updates of track ID $trackId") >>
              subscriptions.subscribe(ForCoordinates(trackId, coords => wsQueue.enqueue1(WebSocketFrame.Text(coords.asJson.noSpaces))))
          case _ => logger.warn(s"Received invalid subscription request: '$str'")
        }

      case ev => logger.debug(s"WS event: $ev")
    }
  }

  private def streamResource(name: String): Task[Response[Task]] = {
    Task(getClass.getClassLoader.getResourceAsStream(s"frontend/$name")).map(Option(_)).flatMap {
      case Some(is) =>
        Ok(fs2.io.readInputStream(Task.now(is), 4096, blocker))

      case None => NotFound(s"Resource '$name' not found")
    }
  }

  val router: HttpApp[Task] = Http4sRouting.make {
    CORS.policy.withAllowMethodsAll.withAllowHeadersAll.withAllowOriginHostCi(allowedOrigins.contains)(routes)
  }

}
