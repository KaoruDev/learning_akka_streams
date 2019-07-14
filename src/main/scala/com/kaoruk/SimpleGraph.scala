package com.kaoruk

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, UniformFanOutShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}
import scala.collection.immutable
import scala.concurrent.Future

import com.kaoruk.elements.{Coal, Diamond, Workable}

import java.util.concurrent.ThreadLocalRandom

object SimpleGraph extends App {
  implicit val system = ActorSystem("SourceMatRunner")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val in: Source[Coal, NotUsed] = Source(1 to 10).map(i => Coal(i))
    val out = Sink.foreach(println)

    val bcast = builder.add(Broadcast[Workable](2))
    val merge = builder.add(Merge[Workable](2))

    val f1, f2, f3, f4 = Flow[Workable].map(item => item.workOn(ThreadLocalRandom.current().nextInt(5)))

    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
    bcast ~> f4 ~> merge
    ClosedShape
  })

  val findDiamonds = Sink.last[Workable]
  val findDiamonds2 = Sink.last[Workable]
  val work = Flow[Workable].map(_.workOn(ThreadLocalRandom.current().nextInt(10)))

  // How to return materialized views of multiple sinks
  val doubleDiamond: RunnableGraph[(Future[Workable], Future[Workable])] = RunnableGraph
    .fromGraph(GraphDSL.create(findDiamonds, findDiamonds2)(Keep.both) { implicit builder => (finder, finder2) =>
      import GraphDSL.Implicits._
      val coalMine: Source[Coal, NotUsed] = Source(1 to 10).map(i => Coal(i))
      val broadcast = builder.add(Broadcast[Coal](2))

      coalMine ~> broadcast ~> work ~> finder
      broadcast ~> work ~> work ~> finder2
      ClosedShape
    })

  val (finderOne, finderTwo) = doubleDiamond.run()
  Future.sequence(List(finderOne, finderTwo)).onComplete(result => {
    println(s"finder1's harvest ${result.get(0)}")
    println(s"finder2's harvest ${result.get(1)}")
    system.terminate()
  })

  val sinks: Seq[Sink[String, Future[String]]] = Seq("a", "b", "c")
    .map(prefix => Flow[String].filter(str => str.startsWith(prefix)).toMat(Sink.head[String])(Keep.right))

  val g: RunnableGraph[Seq[Future[String]]] = RunnableGraph.fromGraph(GraphDSL.create(sinks) {
    implicit b => sinkList =>
      import GraphDSL.Implicits._
      val broadcast = b.add(Broadcast[String](sinkList.size))

      Source(List("ax", "bx", "cx")) ~> broadcast
      sinkList.foreach(sink => broadcast ~> sink)

      ClosedShape
  })
}
