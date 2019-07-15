package com.kaoruk.custom.queues.delayed

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ScheduledThreadPoolExecutor, ThreadFactory}

private [delayed] object DelayedQueueScheduler {
  def buildSchedulerPool(maxThreads: Int): ScheduledThreadPoolExecutor = {
    new ScheduledThreadPoolExecutor(maxThreads, new DelayedQueueSchedulerThreadFactory())
  }
}

private class DelayedQueueSchedulerThreadFactory extends ThreadFactory {
  private val threadGroup = {
    Option(System.getSecurityManager)
        .map(_.getThreadGroup)
        .getOrElse(Thread.currentThread().getThreadGroup)
  }

  private val threadCount = new AtomicInteger(0)
  private val threadName = getClass.getCanonicalName

  override def newThread(r: Runnable): Thread = {
    new Thread(threadGroup, r, s"$threadName-${threadCount.incrementAndGet}", 0)
  }
}
