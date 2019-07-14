package com.kaoruk

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Merge, Source}

object SimpleQueueRunner extends App {
  implicit val system = ActorSystem("SimpleQueueRunner")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val (queue, source) = Source.queue[String](100, OverflowStrategy.backpressure).preMaterialize()
  val source2 = Source(1 to 10).map(_.toString)
//  val (queue2, source2) = Source.queue[Int](100, OverflowStrategy.backpressure).preMaterialize()

  Source.combine(source, source2)(Merge(_))
    .via(Flow[String].map(elm => {
      if (!elm.startsWith("x")) {
        queue.offer(s"x$elm")
        ""
      } else {
        elm
      }
    }))
    .filter(_.startsWith("x"))
    .runForeach(println)
    .onComplete(_ => system.terminate())

  Thread.sleep(3000)
  println("SHUTTING DOWN")
  queue.complete()
}
