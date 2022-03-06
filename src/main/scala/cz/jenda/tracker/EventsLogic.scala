package cz.jenda.tracker

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.parser._
import monix.eval.Task
import net.sigusr.mqtt.api.Message
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.{LocalDateTime, ZoneOffset}

class EventsLogic(dao: Dao) {
  private val logger = Slf4jLogger.getLoggerFromClass[Task](classOf[EventsLogic])

  def saveEvent(m: Message): Task[Unit] = {
    val eventString = new String(m.payload.toArray)

    Task
      .fromEither(parse(eventString).flatMap(_.as[TrackerEvent]))
      .flatMap { ev =>
        import ev._
        import coordinates._

        val time = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.ofHours(1))
        val coords = Coordinates(0, trackerId, time, lat, lon, alt)

        dao.updateVisitedWaypoints(trackerId, visitedWaypoints) >>
          dao.save(coords).as(coords)
      }
      .tapEval(coords => logger.debug(s"Received new coordinates from tracker ID ${coords.trackerId}"))
      .onErrorHandleWith { ex =>
        logger.warn(ex)(s"Error while processing new event:\n$eventString")
      }
      .as(())
  }
}

final case class TrackerEvent(trackerId: Int, timestamp: Long, visitedWaypoints: Int, coordinates: TrackerCoordinates)

object TrackerEvent {
  implicit val cc: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val cd: Decoder[TrackerEvent] = deriveConfiguredDecoder
}

final case class TrackerCoordinates(lat: Double, lon: Double, alt: Double)

object TrackerCoordinates {
  implicit val cc: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val cd: Decoder[TrackerCoordinates] = deriveConfiguredDecoder
}
