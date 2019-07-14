package com.kaoruk

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Merge, Source, SourceQueue}
import scala.concurrent.Future

import com.kaoruk.elements.{Coal, Diamond, Workable}
import grizzled.slf4j.Logger

import java.util.concurrent.{Semaphore, ThreadLocalRandom}
import java.util.concurrent.atomic.AtomicInteger

object RetryPrototypeRunner extends App {
  implicit val system = ActorSystem("CombineSourcesSimple")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()
  val maxAttempts = 5

  val logger = Logger(getClass)

  case class RetryEnvelope(item: Workable, attempt: Int = maxAttempts)
  case class NotGoodEnough(item: Workable, attemptsLeft: Int) extends Exception

  val (retryQueue, source) = Source.queue[RetryEnvelope](1000, OverflowStrategy.dropNew).preMaterialize()

  val kafka: Source[RetryEnvelope, NotUsed] = Source(1 to 50)
    .map(Coal(_))
    .map(coal => {
      logger.info(s"Wraping coal with id: ${coal.id}")
      RetryEnvelope(coal)
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

        if (newWorkable.workEffort > 0 && envelope.attempt > 0) {
          retryQueue.offer(RetryEnvelope(newWorkable, envelope.attempt - 1)).map({
            case QueueClosed | Dropped =>
              logger.error(s"Warning unable to enqueue work item: $newWorkable")
            case Failure(exception) =>
              logger.info(s"Encountered exception when trying to enqueue item: $exception")
            case Enqueued =>
            case something => logger.error(s"Recieved unexpected states: $something")
          }).recover({
            case e => logger.error(s"Encoutered an exception when trying to offer item", e)
          })
        } else if (envelope.attempt == 0 && newWorkable.workEffort > 0) {
          droppedCount.incrementAndGet()
          logger.info(s"EXHAUSTED retry attempts on $newWorkable")
        }

        newWorkable
      }
    }
//    .map(envelope => {
//      val newWorkable = envelope.item
//      if (newWorkable.workEffort > 0 && envelope.attempt > 0) {
//        retryQueue.offer(RetryEnvelope(newWorkable, envelope.attempt - 1)).map({
//          case QueueClosed | Dropped =>
//            logger.error(s"Warning unable to enqueue work item: $newWorkable")
//          case Failure(exception) =>
//            logger.info(s"Encountered exception when trying to enqueue item: $exception")
//          case Enqueued =>
//          case something => logger.error(s"Recieved unexpected states: $something")
//        }).recover({
//          case e => logger.error(s"Encoutered an exception when trying to offer item", e)
//        })
//      } else if (envelope.attempt == 0 && newWorkable.workEffort > 0) {
//        droppedCount.incrementAndGet()
//        logger.info(s"EXHAUSTED retry attempts on $newWorkable")
//      }
//      newWorkable
//    })
    .filter(item => item.workEffort == 0)
    .runForeach(result => {
      diamondCount.incrementAndGet()
      logger.info(s"============== DIAMOND!: $result")
    })
    .onComplete(_ => system.terminate())

  Thread.sleep(5000)
  logger.info("WRAPPING IT UP!")
  retryQueue.complete()
  logger.info(s"Diamond count: ${diamondCount.get()}, dropped count: ${droppedCount.get()} " +
    s"= total: ${droppedCount.get + diamondCount.get}")
}
