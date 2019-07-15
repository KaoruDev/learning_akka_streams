package com.kaoruk.custom.queues.delayed

import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue}
import akka.stream.{Attributes, Outlet, SourceShape}
import scala.reflect.ClassTag

import grizzled.slf4j.Logger

class DelayedQueueSource[T] extends GraphStageWithMaterializedValue[SourceShape[T], DelayedQueueController[T]] {
  private val logger = Logger(getClass)

  // Define the (sole) output port of this stage
  val out: Outlet[T] = Outlet("DelayedQueueRunner")
  // Define the shape of this stage, which is SourceShape with the port we defined above
  override val shape: SourceShape[T] = SourceShape(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, DelayedQueueController[T]) = {
    val logic = new DelayedGraphLogic[T](shape, out, 10)
    (logic, logic)
  }
}

object DelayedQueueSource {
  def apply[T: ClassTag](): DelayedQueueSource[T] = {
    new DelayedQueueSource[T]()
  }
}

