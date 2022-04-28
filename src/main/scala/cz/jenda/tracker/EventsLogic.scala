package cz.jenda.tracker

import com.avast.clients.rabbitmq.api.{Delivery, DeliveryResult, StreamedDelivery}
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import monix.eval.Task
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.{LocalDateTime, ZoneOffset}

class EventsLogic(dao: Dao) {
  private val logger = Slf4jLogger.getLoggerFromClass[Task](classOf[EventsLogic])

  def saveEvent(ev: StreamedDelivery[Task, TrackerEvent]): Task[Unit] = {
    ev.handleWith {
      case Delivery.Ok(ev, _, _) =>
        import ev._
        import coordinates._

        val time = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.ofHours(1))
        val coords = Coordinates(0, trackerId, time, lat, lon, alt, battery)

        (dao.updateVisitedWaypoints(trackerId, visitedWaypoints) >>
          dao.save(coords).as(coords))
          .flatMap(coords => logger.debug(s"Received new coordinates from tracker ID ${coords.trackerId}"))
          .as(DeliveryResult.Ack)

      case Delivery.MalformedContent(body, _, _, ce) =>
        logger.warn(ce)(s"Error while processing new event:\n$body").as(DeliveryResult.Reject)
    }
  }
}

final case class TrackerEvent(trackerId: Int, timestamp: Long, visitedWaypoints: Int, battery: Float, coordinates: TrackerCoordinates)

object TrackerEvent {
  implicit val cc: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val cd: Decoder[TrackerEvent] = deriveConfiguredDecoder
}

final case class TrackerCoordinates(lat: Double, lon: Double, alt: Double)

object TrackerCoordinates {
  implicit val cc: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val cd: Decoder[TrackerCoordinates] = deriveConfiguredDecoder
}
