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
    sql"""insert into tracker.coordinates (tracker_id, time, visited_waypoints, lat, lon, alt)
         values ($trackerId, $time, $visitedWaypoints, $lat, $lon, $alt)""".update.run.transact(doobieTransactor).as(())
  }

  def listFor(trackerId: Int): Task[List[Coordinates]] = {
    sql"""select * from coordinates where  tracker_id = $trackerId""".query[Coordinates].stream.compile.toList.transact(doobieTransactor)
  }
}

case class Coordinates(id: Int, trackerId: Int, time: LocalDateTime, visitedWaypoints: Int, lat: Double, lon: Double, alt: Double)

object Coordinates {
  implicit val encoder: Encoder[Coordinates] = deriveEncoder
}
