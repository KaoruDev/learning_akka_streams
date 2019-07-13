package com.kaoruk

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import scala.concurrent.Future
import scala.util.Success

import java.util.concurrent.ThreadLocalRandom

object AsyncRunner extends App {
  implicit val system = ActorSystem("SlowRunner")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val source = Source.lazily[Int, NotUsed](() => Source(1 to 10).map(i => {
    if (i % 10 == 0) {
      Thread.sleep(i * i)
    }
    println(s"${Thread.currentThread().getName} PRODUCING $i")
    i
  }))
    .mapAsync[String](4)(i => {
      Future {
        Thread.sleep(ThreadLocalRandom.current().nextInt(i * i * 10))
        println(s"${Thread.currentThread().getName}} ====== CONSUMING $i")
        "Foobar"
      }
    })

 /*
 ** This will cause an premature shutdown. Idk why or how. But it appears to me that the future returned by
 * .run() is connected to the Source, somehow, as it returns a Future[NotUsed], produced by the Source.
 *
 * We never materialize the source
 */
  def prematureShutdown() = {
    source.to(Sink.ignore)
      .run()
      .onComplete(result => {
        println(result)
        system.terminate()
      })
  }

  def correctShutdown() = {
    source.toMat(Sink.head)(Keep.right)
      .run()
      .onComplete(result => {
        println(result)
        system.terminate()
      })
  }

  prematureShutdown()
//  correctShutdown()
}


