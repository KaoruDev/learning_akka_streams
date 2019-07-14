package com.kaoruk

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, scaladsl}
import akka.stream.scaladsl.{Flow, Merge, Source}
import scala.concurrent.Future

import com.kaoruk.elements.{Coal, Diamond, FlawedDiamond, Workable}

object CombingSourcesSimpleRunner extends App {
  implicit val system = ActorSystem("CombineSourcesSimple")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val sourceOne: Source[Coal, NotUsed] = Source(0 to 10).map(Coal(_))
  val sourceTwo: Source[FlawedDiamond, NotUsed] = Source(20 to 30).map(FlawedDiamond(_))

  val mergedSource: Source[Workable, NotUsed] =
    Source.combine(sourceOne, sourceTwo)(scaladsl.Merge(_))

  mergedSource.runForeach(println).onComplete(_ => system.terminate())
}
