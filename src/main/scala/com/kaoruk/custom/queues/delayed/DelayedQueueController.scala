package com.kaoruk.custom.queues.delayed

import scala.concurrent.{ExecutionContext, Future, Promise}

import com.kaoruk.custom.queues.delayed.DelayedQueueController.Result

trait DelayedQueueController[T] {
  def complete(): Future[Int]
  def enqueue(item: T, delayBy: Long)(implicit ec: ExecutionContext): Result
}

object DelayedQueueController {
  sealed trait Command[+T]
  case class Complete(promise: Promise[Int]) extends Command[Nothing]
  case class Enqueue[+T](item: T) extends Command[T]

  sealed trait Result
  case object Enqueued extends Result
  case object Dropped extends Result
}
