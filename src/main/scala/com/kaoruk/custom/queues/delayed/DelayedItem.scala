package com.kaoruk.custom.queues.delayed

import java.util.concurrent.{Delayed, TimeUnit}

case class DelayedItem[T](item: T, delayedBy: Long) extends Delayed {

  private val readyAt = System.currentTimeMillis() + delayedBy

  override def getDelay(unit: TimeUnit): Long = {
    unit.convert(readyAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
  }

  override def compareTo(o: Delayed): Int = {
    getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS) match {
      case 0 => 0
      case n if n < 0 => -1
      case n => 1
    }
  }
}
