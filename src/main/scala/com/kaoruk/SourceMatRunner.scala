package com.kaoruk

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import scala.concurrent.Promise
import scala.util.Success

import com.kaoruk.elements.Coal

object SourceMatRunner extends App {
  implicit val system = ActorSystem("SourceMatRunner")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  Source.actorRefWithAck()

  val source: Source[Coal, Promise[Option[Coal]]] = Source.maybe[Coal]

  val (promise, otherSource): (Promise[Option[Coal]], Source[Coal, NotUsed]) = source.preMaterialize()
  val (promise2, otherSource2): (Promise[Option[Coal]], Source[Coal, NotUsed]) = source.preMaterialize()

  otherSource2.runForeach(_ => println("THIS NEVER GETS CALLED BECAUSE MY FUTURE DOES NOT COMPLETE, SAD FACE"))

  otherSource.runForeach(println)
      .onComplete(_ => system.terminate())

  println("Completing future")

  promise.complete(Success(Some(Coal(1))))
}
