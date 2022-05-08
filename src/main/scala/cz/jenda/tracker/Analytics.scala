package cz.jenda.tracker

import cz.jenda.tracker.Analytics.{DF, WarningThreshold}
import monix.eval.Task

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime, ZoneOffset}

class Analytics(dao: Dao) {
  def analyze(trackId: Int): fs2.Stream[Task, String] = {
    fs2.Stream.eval {
      dao.getTrack(trackId).map {
        case Some(track) =>
          fs2
            .Stream(s"Track $trackId (${track.name}):\n\n")
            .append {
              dao
                .analyzeTrack(trackId)
                .collect {
                  case tl if tl.prevTime.exists(pt => Duration.between(pt, tl.time).compareTo(WarningThreshold) >= 0) =>
                    val prevTime = tl.prevTime.getOrElse(sys.error("Should not get here"))
                    val delay = Duration.between(prevTime, tl.time)

                    s"""Time: ${DF.format(prevTime)} - ${DF.format(tl.time)}    (${toTimestamp(prevTime)} - ${toTimestamp(tl.time)})
                       |Lag: $delay
                       |
                       |""".stripMargin
                }
            }

        case None => fs2.Stream("Track not found")
      }
    }.flatten
  }

  private def toTimestamp(time: LocalDateTime): Long = {
    time.toInstant(ZoneOffset.ofHours(1)).getEpochSecond
  }
}

object Analytics {
  final val WarningThreshold: Duration = Duration.ofMinutes(2)
  final val ErrorThreshold: Duration = Duration.ofMinutes(5)

  private val DF: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
}
