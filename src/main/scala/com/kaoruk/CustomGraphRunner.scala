package com.kaoruk

import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}

import com.kaoruk.elements.Workable

//object CustomGraphRunner extends App {
//  case class Envelope(item: Workable, attemptsLeft: Int = 5)
//
//  class NumbersSource extends GraphStageWithMaterializedValue[SourceShape[Envelope], DelayedQueueController] {
//    val out: Outlet[Envelope] = Outlet("NumbersSource")
//    override val shape: SourceShape[Envelope] = SourceShape(out)
//
//    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, DelayedQueueController) = {
//      new GraphStageLogic(shape) {
//        // All state MUST be inside the GraphStageLogic,
//        // never inside the enclosing GraphStage.
//        // This state is safe to access and modify from all the
//        // callbacks that are provided by GraphStageLogic and the
//        // registered handlers.
//        private var counter = 1
//
//        setHandler(out, new OutHandler {
//          override def onPull(): Unit = {
//            push(out, counter)
//            counter += 1
//          }
//        })
//      }
//    }
//
//    // This is where the actual (possibly stateful) logic will live
//    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
//      new GraphStageLogic(shape) {
//        // All state MUST be inside the GraphStageLogic,
//        // never inside the enclosing GraphStage.
//        // This state is safe to access and modify from all the
//        // callbacks that are provided by GraphStageLogic and the
//        // registered handlers.
//        private var counter = 1
//
//        setHandler(out, new OutHandler {
//          override def onPull(): Unit = {
//            push(out, counter)
//            counter += 1
//          }
//        })
//      }
//    }
//  }
//
//  trait DelayedQueueController {
//    def offer: Unit
//    def complete: Unit
//  }
//}
