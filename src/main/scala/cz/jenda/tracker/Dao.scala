package cz.jenda.tracker

import doobie.hikari
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import doobie.util.Get
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import monix.eval.Task

import java.time.{Duration, LocalDateTime}

class Dao(doobieTransactor: hikari.HikariTransactor[Task]) {

  def save(nc: Coordinates): Task[Unit] = {
    import nc._
    sql"""insert into coordinates (track_id, time, lat, lon, alt, battery)
         values ($trackId, $time, $lat, $lon, $alt, $battery)""".update.run.transact(doobieTransactor).void
  }

  def listTracks(): Task[List[Track]] = {
    sql"""select * from tracks""".query[Track].stream.compile.toList.transact(doobieTransactor)
  }

  def listTrackers(): Task[List[Tracker]] = {
    sql"""select trackers.*, tr.* from trackers join current_tracks as ct on ct.tracker_id = trackers.id join tracks as tr on ct.track_id = tr.id""".stripMargin.query[Tracker].stream.compile.toList.transact(doobieTransactor)
  }

  def getTrack(id: Int): Task[Option[Track]] = {
    sql"""select * from tracks where id = $id""".query[Track].option.transact(doobieTransactor)
  }

  def getTracker(id: Int): Task[Option[Tracker]] = {
    sql"""select trackers.*, tr.* from trackers join current_tracks as ct on ct.tracker_id = trackers.id join tracks as tr on ct.track_id = tr.id where trackers.id = $id""".query[Tracker].option.transact(doobieTransactor)
  }

  def getCurrentTrack(trackerId: Int): Task[Option[CurrentTrackRelation]] = {
    sql"""select * from current_tracks where tracker_id = $trackerId""".query[CurrentTrackRelation].option.transact(doobieTransactor)
  }

  def assignCurrentTrack(trackerId: Int, trackId: Int): Task[Unit] = {
    sql"""update current_tracks set track_id = $trackId where tracker_id = $trackerId""".update.run.transact(doobieTransactor).void
  }

  def createTrack(trackerId: Int, name: String): Task[Track] = {
    sql"""insert into tracks ( tracker_id, name) values ($trackerId, $name)""".update.run.transact(doobieTransactor).void >> {
      sql"""select * from tracks where name = $name"""
        .query[Track]
        .option
        .transact(doobieTransactor)
        .map(_.getOrElse(throw new IllegalStateException("The track must exist!")))
    }
  }

  def updateVisitedWaypoints(trackId: Int, visitedWaypoints: Int): Task[Unit] = {
    sql"""update tracks set visited_waypoints = $visitedWaypoints where id = $trackId""".update.run.transact(doobieTransactor).void
  }

  def listCoordinatesFor(trackId: Int): fs2.Stream[Task, Coordinates] = {
    sql"""select * from coordinates where track_id = $trackId order by time""".query[Coordinates].stream.transact(doobieTransactor)
  }

  def lastCoordinatesFor(trackId: Int): Task[Option[Coordinates]] = {
    sql"""select * from coordinates where track_id = $trackId order by time desc limit 1"""
      .query[Coordinates]
      .option
      .transact(doobieTransactor)
  }

  def listWaypointsFor(trackId: Int): Task[List[Waypoint]] = {
    sql"""select * from waypoints where track_id = $trackId order by seq_id"""
      .query[Waypoint]
      .stream
      .compile
      .toList
      .transact(doobieTransactor)
  }

  def analyzeTrack(trackId: Int): fs2.Stream[Task, TrackLag] = {
    sql"""SELECT time, (LAG(time) OVER (ORDER BY time)) as prevTime FROM coordinates WHERE track_id = $trackId ORDER BY time"""
      .query[TrackLag]
      .stream
      .transact(doobieTransactor)
  }
}

object Dao {
  implicit val durationRead: Get[Duration] = Get[Long].map(Duration.ofSeconds)
}

final case class Coordinates(id: Int, trackId: Int, time: LocalDateTime, lat: Double, lon: Double, alt: Double, battery: Float)

object Coordinates {
  implicit val encoder: Encoder[Coordinates] = deriveEncoder
}

final case class Tracker(id: Int, name: String, track: Option[Track])

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

final case class TrackLag(time: LocalDateTime, prevTime: Option[LocalDateTime])

object TrackLag {

  implicit val encoder: Encoder[TrackLag] = deriveEncoder
}
