package com.kaoruk

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import scala.concurrent.Future

object SlowRunner extends App {
  implicit val system = ActorSystem("SlowRunner")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val source = Source.lazily(() => Source(1 to 100).map(i => {
    if (i % 10 == 0) {
      Thread.sleep(i * i)
    }
    println(s"${Thread.currentThread().getName} PRODUCING $i")
    i
  }))

  source.mapAsync(4)(i => {
    Future {
      Thread.sleep(i * i)
      println(s"${Thread.currentThread().getName} - ${Thread.currentThread().getId} ====== CONSUMING $i")
    }
  })
    .to(Sink.ignore)
    .run()
    .onComplete(_ => system.terminate())
}
