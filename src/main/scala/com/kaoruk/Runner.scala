package com.kaoruk
import akka.stream._
import akka.stream.scaladsl._

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

object Runner extends App {
  implicit val system = ActorSystem("KaoruStreams")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 10)

  source.scan(BigInt(1))((acc, next) => acc * next)
    .map(num => ByteString(s"$num\n"))
    .runWith(FileIO.toPath(Paths.get("factorials.txt")))
    .onComplete(_ => system.terminate())(system.dispatcher)
}
