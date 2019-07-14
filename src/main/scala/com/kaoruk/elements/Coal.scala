package com.kaoruk.elements

import java.util.concurrent.ThreadLocalRandom

case class Coal(id: Int, workEffort: Int = ThreadLocalRandom.current().nextInt(10)) extends Workable {
  override def workOn(effort: Int): Workable = {
    val newEffort = workEffort - effort

    if (newEffort <= 0) {
      FlawedDiamond(id)
    } else {
      Coal(id, newEffort)
    }
  }
}
