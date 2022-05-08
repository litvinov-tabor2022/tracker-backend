package cz.jenda.tracker

import cats.effect.concurrent.Ref
import cz.jenda.tracker.Subscription.ForCoordinates
import monix.eval.Task
import org.typelevel.log4cats.slf4j.Slf4jLogger

class Subscriptions {
  private val logger = Slf4jLogger.getLoggerFromClass[Task](classOf[Subscriptions])

  private val subscriptions: Ref[Task, List[Subscription]] = Ref.unsafe(Nil)

  def subscribe(s: Subscription): Task[Unit] = {
    // TODO timing-out subscriptions
    subscriptions.modify(l => (s :: l, ()))
  }

  def newCoordinates(c: Coordinates): Task[Unit] = {
    subscriptions.get
      .map(_.collect { case ForCoordinates(trackId, enqueue) if trackId == c.trackId => enqueue })
      .flatMap { l =>
        Task
          .parSequenceUnordered(l.map {
            _.apply(c).onErrorHandleWith(e => logger.warn(e)("Could not send subscription update"))
          })
          .void
      }
  }
}

sealed trait Subscription

object Subscription {
  case class ForCoordinates(trackId: Int, enqueue: Coordinates => Task[Unit]) extends Subscription
}
