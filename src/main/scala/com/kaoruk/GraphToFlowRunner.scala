package com.kaoruk

import akka.{Done, NotUsed}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, Zip}
import scala.concurrent.Future

object GraphToFlowRunner extends App {
  implicit val system = ActorSystem("SourceMatRunner")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val pairUpWithToString =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      // prepare graph elements
      val broadcast = b.add(Broadcast[Int](2))
      val zip = b.add(Zip[Int, String]())

      // connect the graph
      broadcast.out(0).map(_ + 5) ~> zip.in0
      broadcast.out(1).map(_.toString) ~> zip.in1

      // expose ports
      FlowShape(broadcast.in, zip.out)
    })

  val sink: Sink[Tuple2[Int, String], Future[Done]] = Sink.foreach(println)

  pairUpWithToString.runWith(Source(List(1)), sink)
}
