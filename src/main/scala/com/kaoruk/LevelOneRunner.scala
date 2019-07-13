package com.kaoruk

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.Success

object LevelOneRunner extends App {
  implicit val system = ActorSystem("KaoruSlowStreams")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  case class Coal()
  case class FlawedDiamond()
  case class Diamond()

  val source: Source[Coal, Promise[Option[Coal]]] = Source.maybe[Coal]
  val flow: Flow[Coal, FlawedDiamond, NotUsed] = Flow[Coal]
    .throttle(1, 500.millisecond)
    .map(i => FlawedDiamond())
  val sink: Sink[FlawedDiamond, Future[Diamond]] = Sink.fold(Diamond())((_, _) => Diamond())

  // A runnable graph that will throttle each element for 500, will not start until Promise is completed
  // Note .to will always keep left
  val r1: RunnableGraph[Promise[Option[Coal]]] = source.via(flow).to(sink)
  val r11: Source[FlawedDiamond, Promise[Option[Coal]]] = source.via(flow)

  // ================ Source[SourceIn] + Flow[SourceIn, InFlow] = Source[InFlow]
  // These returns the flow's right, to will just pass in the above left.
  val r2: RunnableGraph[NotUsed] = source.viaMat(flow)(Keep.right).to(sink)

  // By adding a flow, we replace the source's in with that of the flow
  val r21: Source[FlawedDiamond, NotUsed] = source.viaMat(flow)(Keep.right)

  // .to(sink) will pass the source's Mat
  val r22: RunnableGraph[NotUsed] = r21.to(sink)

  // In order to get the sink's Mat, you'll need to use .toMat()(Keep.right) instead
  val r23: RunnableGraph[Future[Diamond]] = r21.toMat(sink)(Keep.right)

  // Source.via(flow) will keep the source's Mat
  val r3: RunnableGraph[Future[Diamond]] = source.via(flow).toMat(sink)(Keep.right)

  // Source.runWith will run the graph and return the sink's Mat
  val r4: Future[Diamond] = source.via(flow).runWith(sink)
  // to keep the sources's Mat, use .toMat to explicitly get it or .to
  val r41: RunnableGraph[Promise[Option[Coal]]] = source.via(flow).toMat(sink)(Keep.left)
  val r42: RunnableGraph[Promise[Option[Coal]]] = source.via(flow).to(sink)

  /*
  FlowShape[I, O] + a SinkShape[O] = a SinkShape[I]
  using Sink.runWith(source) will return the source's Mat
  To get the Sink's Mat, do Source.toMat(sink)(Keep.right), see r52b
   */
  val r5: Promise[Option[Coal]] = flow.to[Future[Diamond]](sink).runWith(source)

  val r51: Sink[Coal, NotUsed] = flow.to[Future[Diamond]](sink)
  val r51b: Sink[Coal, Future[Diamond]] = flow.toMat(sink)(Keep.right)
  val r52: Promise[Option[Coal]] = r51.runWith(source)
  val r52b: RunnableGraph[Future[Diamond]] = source.toMat(r51b)(Keep.right)

  // Flows also have a runWith API, but flows need both a source and sink to run, thus why runWith expects both.
  val r6: (Promise[Option[Coal]], Future[Diamond]) = flow.runWith(source, sink)

  // Source.viaMat(flow) expects a flow and returns back a source.
  val r7: RunnableGraph[(Promise[Option[Coal]], NotUsed)] = source.viaMat(flow)(Keep.both).to(sink)

  val r71: Source[FlawedDiamond, (Promise[Option[Coal]], NotUsed)] = source.viaMat(flow)(Keep.both)
  val r72: RunnableGraph[(Promise[Option[Coal]], NotUsed)] = r71.to(sink)


  val r8: RunnableGraph[(Promise[Option[Coal]], Future[Diamond])] = source
    .via[FlawedDiamond, NotUsed](flow)
    .toMat[Future[Diamond], (Promise[Option[Coal]], Future[Diamond])](sink)(Keep.both)

  val r81: Source[FlawedDiamond, Promise[Option[Coal]]] = source.via(flow)
  val r82: RunnableGraph[(Promise[Option[Coal]], Future[Diamond])] =
    r81.toMat[Future[Diamond], (Promise[Option[Coal]], Future[Diamond])](sink)(Keep.both)


  val r9: RunnableGraph[((Promise[Option[Coal]], NotUsed), Future[Diamond])] =
    source.viaMat(flow)(Keep.both).toMat(sink)(Keep.both)

  val r91: Source[FlawedDiamond, (Promise[Option[Coal]], NotUsed)] =
    source.viaMat[FlawedDiamond, NotUsed, (Promise[Option[Coal]], NotUsed)](flow)(Keep.both)

  val r92: RunnableGraph[((Promise[Option[Coal]], NotUsed), Future[Diamond])] =
    r91.toMat[Future[Diamond], ((Promise[Option[Coal]], NotUsed), Future[Diamond])](sink)(Keep.both)

  // We can make our own custom combine, which means we don't have to deal with Tuples, if we create our own
  val r93: RunnableGraph[(Future[Diamond], Promise[Option[Coal]], NotUsed)] = source
    .viaMat(flow)(Keep.both)
    .toMat(sink)((c, f) => (f, c._1, c._2))

  // Also possible to call mapMaterializedValue on a RunnableGraph
  val r111: RunnableGraph[(Future[Diamond], Promise[Option[Coal]], NotUsed)] = r92.mapMaterializedValue({
    case ((promise, notUsed), diamondGen) => (diamondGen, promise, notUsed)
  })

  // From GraphDSL -- don't understand this too much atm
  val r120: RunnableGraph[(Promise[Option[Coal]], NotUsed, Future[Diamond])] = RunnableGraph.fromGraph(
    GraphDSL.create(source, flow, sink)((_, _, _)){ implicit builder => (src, f, dest) => {
      import GraphDSL.Implicits._

      src ~> f ~> dest
      ClosedShape
    }}
  )
}
