package com.kaoruk
import akka.stream._
import akka.stream.scaladsl._

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
import scala.collection.mutable.{ListBuffer => MutList}

object Runner extends App {
  implicit val system = ActorSystem("KaoruStreams")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
  val tasks: MutList[Future[_]] = MutList()

  val failing = source.map(Utils.kaBoomOn(10))
    .runForeach(i => println(s"failing: $i"))

  val failingSource = Source(1 to 100).map(Utils.kaBoomOn(20))
    .map(i => println(s"failingSource: $i"))
    .toMat(Sink.ignore)(Keep.right)

  failing.recoverWith({
    case exception: Exception =>
      println(s"failing failed with $exception, continuing to failingSource")
      failingSource.run()
  }).onComplete(result => println(s"failing result: $result"))

  tasks += failing

  tasks += factorials
    .map(_.toString)
    .runWith(Utils.lineSink("factorial2.txt"))

//  tasks += factorials
//      .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
//      .throttle(1, 1.second)
//      .runForeach(println)

  Future.sequence(tasks)
    .onComplete(_ => system.terminate())
}

object Utils {

  /**
    *  Keep.right means to keep the right type parameter of Sink, this case, Future[IOResult]
    */
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  def kaBoomOn(diff: Int): Int => Int = {
    num => {
      if (num % diff == 0) {
        throw new Exception("KABOOM")
      }
      num
    }
  }
}
