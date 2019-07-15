package com.kaoruk.custom.queues.delayed

import akka.stream.stage.{GraphStageLogic, OutHandler}
import akka.stream.{Outlet, SourceShape}
import scala.concurrent.{ExecutionContext, Future, Promise}

import com.kaoruk.custom.queues.delayed.DelayedQueueController._
import grizzled.slf4j.Logger

import java.util.concurrent.{Semaphore, TimeUnit}

private [delayed] case class DelayedGraphLogic[T](shape: SourceShape[T], out: Outlet[T], maxInFlight: Int) extends GraphStageLogic(shape) with DelayedQueueController[T] {
  private val logger = Logger(getClass)
  private val inFlight = new Semaphore(maxInFlight)
  private val scheduler = DelayedQueueScheduler.buildSchedulerPool(maxInFlight)
  private val queue = new java.util.LinkedList[T]()

  setHandler(out, new OutHandler {
    override def onPull(): Unit = {
      Option(queue.poll())
        .foreach(el => {
          push(out, el)
          inFlight.release()
        })
    }
  })

  private val controller = getAsyncCallback[Command[T]] {
    case Complete(promise) =>
      complete(out)
      promise.success(queue.size)
      scheduler.shutdown() // Not the optimal way to shutdown a threadpool, but since this is demo, meh
    case Enqueue(item) =>
      queue.add(item)
      if (isAvailable(out)) {
        push(out, queue.poll())
        inFlight.release()
      }
  }

  override def complete(): Future[Int] = {
    val promise = Promise[Int]()
    controller.invokeWithFeedback(Complete(promise))
    promise.future
  }

  override def enqueue(item: T, delayBy: Long)(implicit ec: ExecutionContext): Result = {
    if (inFlight.tryAcquire()) {
      scheduler.schedule(new Runnable {
        override def run(): Unit = {
          controller.invoke(Enqueue(item))
        }
      }, delayBy, TimeUnit.MILLISECONDS)
      Enqueued
    } else {
      // If dropping the new element is not desired, we'll need to tell the controller to change behavior
      // then we'll need to return a future
      Dropped
    }
  }
}
