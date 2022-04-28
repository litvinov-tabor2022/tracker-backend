package cz.jenda.tracker

import doobie.hikari
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import monix.eval.Task

import java.time.LocalDateTime

class Dao(doobieTransactor: hikari.HikariTransactor[Task]) {
  def save(nc: Coordinates): Task[Unit] = {
    import nc._
    sql"""insert into tracker.coordinates (tracker_id, time, lat, lon, alt, battery)
         values ($trackerId, $time, $lat, $lon, $alt, $battery)""".update.run.transact(doobieTransactor).as(())
  }

  def listTrackers(): Task[List[Tracker]] = {
    sql"""select * from trackers""".query[Tracker].stream.compile.toList.transact(doobieTransactor)
  }

  def getTracker(id: Int): Task[Option[Tracker]] = {
    sql"""select * from trackers where id = $id""".query[Tracker].option.transact(doobieTransactor)
  }

  def updateVisitedWaypoints(trackerId: Int, visitedWaypoints: Int): Task[Unit] = {
    sql"""update trackers set visited_waypoints = $visitedWaypoints where id = $trackerId""".update.run.transact(doobieTransactor).as(())
  }

  def listCoordinatesFor(trackerId: Int): fs2.Stream[Task, Coordinates] = {
    sql"""select * from coordinates where tracker_id = $trackerId""".query[Coordinates].stream.transact(doobieTransactor)
  }

  def listWaypointsFor(trackerId: Int): Task[List[Waypoint]] = {
    sql"""select * from waypoints where tracker_id = $trackerId order by seq_id"""
      .query[Waypoint]
      .stream
      .compile
      .toList
      .transact(doobieTransactor)
  }
}

final case class Coordinates(id: Int, trackerId: Int, time: LocalDateTime, lat: Double, lon: Double, alt: Double, battery: Double)

object Coordinates {
  implicit val encoder: Encoder[Coordinates] = deriveEncoder
}

final case class Tracker(id: Int, name: String, visitedWaypoints: Int)

object Tracker {
  implicit val encoder: Encoder[Tracker] = deriveEncoder
}

final case class Waypoint(id: Int, trackerId: Int, seqId: Int, name: String, lat: Double, lon: Double)

object Waypoint {
  implicit val encoder: Encoder[Waypoint] = deriveEncoder
}
