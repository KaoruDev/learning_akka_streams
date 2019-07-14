package com.kaoruk

import akka.actor.ActorSystem
import akka.stream
import akka.stream.javadsl.Source
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, ZipWith}
import akka.stream.{ActorMaterializer, ClosedShape, UniformFanInShape}
import scala.concurrent.{Await, Future}

object PartialGraphRunner extends {
  implicit val system = ActorSystem("SourceMatRunner")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val pickMaxOfThree = GraphDSL.create() { implicit b => {
    //    val zip1 = b.add(ZipWith[Int, Int, Int](math.max _))
    //    val zip2 = b.add(ZipWith[Int, Int, Int](math.max _))
    //    zip1.out ~> zip2.in0
    val zip = b.add(ZipWith[Int, Int, Int, Int]((a, b, c) => List(a,b,c).max))

    UniformFanInShape(zip.out, zip.in0, zip.in1, zip.in2)
  }}

  // This needs to be passed into `GraphDSL.create` so that `create` will know what the Mat of the returned graph is
  val resultSink = Sink.head[Int]

  val g = RunnableGraph.fromGraph(GraphDSL.create(resultSink) { implicit b => sink =>
    import GraphDSL.Implicits._

    // importing the partial graph will return its shape (inlets & outlets)
    val pm3 = b.add(pickMaxOfThree)

    Source.single(1) ~> pm3.in(0)
    Source.single(2) ~> pm3.in(1)
    Source.single(3) ~> pm3.in(2)
    pm3.out ~> sink.in
    ClosedShape
  })

  g.run().onComplete(_ => system.terminate())
}
