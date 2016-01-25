package com.scaledaction.sentimentanalysis.ingest.frontend

import akka.stream.actor.ActorPublisher

/**
 * A simple ActorPublisher[Status], that receives Status from the event bus
 * and forwards to the Source
 *
 * TODO: This is a tiny example, so edge cases are not covered (yet).
 */
class StatusPublisherActor extends ActorPublisher[Tweet] {

  val sub = context.system.eventStream.subscribe(self, classOf[Tweet])

  override def receive: Receive = {
    case s: Tweet => {
      if (isActive && totalDemand > 0) onNext(s)
    }
    case _ =>
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

}