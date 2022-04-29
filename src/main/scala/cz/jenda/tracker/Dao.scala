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
    sql"""insert into coordinates (track_id, time, lat, lon, alt, battery)
         values ($trackId, $time, $lat, $lon, $alt, $battery)""".update.run.transact(doobieTransactor).as(())
  }

  def listTracks(): Task[List[Track]] = {
    sql"""select * from tracks""".query[Track].stream.compile.toList.transact(doobieTransactor)
  }

  def getTrack(id: Int): Task[Option[Track]] = {
    sql"""select * from tracks where id = $id""".query[Track].option.transact(doobieTransactor)
  }

  def getCurrentTrack(trackerId: Int): Task[Option[CurrentTrackRelation]] = {
    // TODO swap!!
    sql"""select * from current_tracks where track_id = $trackerId""".query[CurrentTrackRelation].option.transact(doobieTransactor)
  }

  def updateVisitedWaypoints(trackId: Int, visitedWaypoints: Int): Task[Unit] = {
    sql"""update tracks set visited_waypoints = $visitedWaypoints where id = $trackId""".update.run.transact(doobieTransactor).as(())
  }

  def listCoordinatesFor(trackId: Int): fs2.Stream[Task, Coordinates] = {
    sql"""select * from coordinates where track_id = $trackId order by time""".query[Coordinates].stream.transact(doobieTransactor)
  }

  def listWaypointsFor(trackId: Int): Task[List[Waypoint]] = {
    sql"""select * from waypoints where track_id = $trackId order by seq_id"""
      .query[Waypoint]
      .stream
      .compile
      .toList
      .transact(doobieTransactor)
  }
}

final case class Coordinates(id: Int, trackId: Int, time: LocalDateTime, lat: Double, lon: Double, alt: Double, battery: Float)

object Coordinates {
  implicit val encoder: Encoder[Coordinates] = deriveEncoder
}

final case class Tracker(id: Int, name: String)

object Tracker {
  implicit val encoder: Encoder[Tracker] = deriveEncoder
}

final case class Track(id: Int, trackerId: Int, name: String, visitedWaypoints: Int)

object Track {
  implicit val encoder: Encoder[Track] = deriveEncoder
}

final case class CurrentTrackRelation(trackerId: Int, trackId: Int)

object CurrentTrackRelation {
  implicit val encoder: Encoder[CurrentTrackRelation] = deriveEncoder
}

final case class Waypoint(id: Int, trackId: Int, seqId: Int, name: String, lat: Double, lon: Double)

object Waypoint {
  implicit val encoder: Encoder[Waypoint] = deriveEncoder
}
