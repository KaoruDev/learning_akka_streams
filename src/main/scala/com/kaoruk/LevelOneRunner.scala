package com.kaoruk

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import scala.concurrent.Promise
import scala.concurrent.duration._

object LevelOneRunner extends App {
  implicit val system = ActorSystem("KaoruSlowStreams")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]
  val throttler = Flow[Int].throttle(1, 500.millisecond)
  val sink = Sink.head[Int]

  val r1: RunnableGraph[Promise[Option[Int]]] = source.via(throttler).to(sink)
}
