package com.kaoruk

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Merge, Source}
import akka.stream.{ActorMaterializer, Attributes, Outlet, OverflowStrategy, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, OutHandler}
import scala.concurrent.Future

import com.kaoruk.custom.queues.delayed.DelayedQueueController.{Dropped, Enqueued}
import com.kaoruk.custom.queues.delayed.DelayedQueueSource
import com.kaoruk.elements.{Coal, RetryEnvelope, Workable}
import grizzled.slf4j.Logger

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

object DelayedQueueRunner extends App {
  implicit val system = ActorSystem("DelayedQueueRunner")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()
  val maxAttempts = 10

  val logger = Logger(getClass)

  case class NotGoodEnough(item: Workable, attemptsLeft: Int) extends Exception

  val (retryQueue, source) = Source.fromGraph(DelayedQueueSource[RetryEnvelope[Workable]]()).preMaterialize()
//  val (retryQueue, source) = Source.queue[RetryEnvelope[Workable]](1000, OverflowStrategy.dropNew).preMaterialize()

  val kafka: Source[RetryEnvelope[Workable], NotUsed] = Source(1 to 10)
    .map(Coal(_))
    .map(coal => {
      logger.info(s"Wraping coal with id: ${coal.id}")
      RetryEnvelope(coal, 0)
    })

  val diamondCount = new AtomicInteger(0)
  val droppedCount = new AtomicInteger(0)

  val retrySource = source.map(env => {
    logger.info(s"Retrying on ${env.item}")
    env
  })


  Source.combine(retrySource, kafka)(Merge(_))
    .mapAsyncUnordered(4) { envelope =>
      Future {
        val effort = ThreadLocalRandom.current().nextInt(5)
        val newWorkable = envelope.item.workOn(effort)

        if (newWorkable.workEffort > 0 && envelope.attempts < maxAttempts) {
          retryQueue.enqueue(RetryEnvelope(newWorkable, envelope.attempts - 1), envelope.attempts * envelope.attempts * 25) match {
            case Dropped =>
              droppedCount.incrementAndGet()
              logger.error(s"DROPPPED unable to enqueue work item: $newWorkable")
            case Enqueued =>
          }
        } else if (envelope.attempts == 0 && newWorkable.workEffort < maxAttempts) {
          droppedCount.incrementAndGet()
          logger.info(s"EXHAUSTED retry attempts on $newWorkable")
        }

        newWorkable
      }
    }
    .filter(item => item.workEffort == 0)
    .runForeach(result => {
      diamondCount.incrementAndGet()
      logger.info(s"============== DIAMOND!: $result")
    })
    .onComplete(_ => {
      logger.info(s"Diamond count: ${diamondCount.get()}, dropped count: ${droppedCount.get()} " +
        s"= total: ${droppedCount.get + diamondCount.get}")
      system.terminate()
    })

  Thread.sleep(5000)
  logger.info("WRAPPING IT UP!")
  retryQueue.complete().foreach(droppedCount.addAndGet)
}

