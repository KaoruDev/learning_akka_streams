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

  val source = Source.lazily[Int, NotUsed](() => Source(1 to 100).map(i => {
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
 * Answer: Source.lazily's Mat is Future[M], where M is the Materialized value of the Source `lazily` takes in.
 * Thus our future is `Future[NotUsed]` which is the Materialized value of `Source.map(1 to 100)` meaning, we get the
 * future as soon as we finish processing the first value
 *
 * **ASSUMPTIONS**
 * system.terminate stops the stream when Future[NotUsed] completes, which is as soon as the Source produces it's first
 * value. system.terminate seems to also prevent new tasks from being scheduled
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


