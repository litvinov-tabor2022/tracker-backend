package cz.jenda.tracker

import fs2.text.utf8Encode
import monix.eval.Task

import java.time.LocalDateTime

object GpxGenerator {
  def createGpx(tracker: Tracker, waypoints: List[Waypoint]): fs2.Pipe[Task, Coordinates, Byte] = { in =>
    val coords = in.map(trackPoint)

    fs2.Stream.eval(Task(LocalDateTime.now()).map(header(tracker.name, _, waypoints, tracker.visitedWaypoints))).through(utf8Encode) ++
      coords.through(utf8Encode) ++
      fs2.Stream.eval(Task.now(footer)).through(utf8Encode)
  }

  private def trackPoint(coords: Coordinates): String = {
    s"""      <trkpt lat="${coords.lat}" lon="${coords.lon}">
       |        <ele>${coords.alt}</ele>
       |        <time>${coords.time}Z</time>
       |      </trkpt>
       |""".stripMargin
  }

  private def header(trackerName: String, now: LocalDateTime, waypoints: List[Waypoint], waypointsVisited: Int): String = {
    s"""<?xml version="1.0"?>
       |<gpx xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.topografix.com/GPX/1/0" version="1.0">
       |  <time>${now.toString}Z</time>
       |""".stripMargin +
      waypoints.zipWithIndex.map { case (w, i) => wayPoint(w, (i + 1) <= waypointsVisited) }.mkString +
      s"""  <trk>
         |    <name>Trasa: '$trackerName'</name>
         |    <trkseg>
         |""".stripMargin
  }

  private def wayPoint(coords: Waypoint, isVisited: Boolean): String = {
    s"""  <wpt lat="${coords.lat}" lon="${coords.lon}" visited="$isVisited">
       |    <name>${coords.name}</name>
       |  </wpt>
       |""".stripMargin
  }

  private val footer =
    """     </trkseg>
      |  </trk>
      |</gpx>
      |""".stripMargin
}
