package com.kaoruk.elements

import java.util.concurrent.ThreadLocalRandom

case class FlawedDiamond(id: Int, workEffort: Int = ThreadLocalRandom.current().nextInt(20)) extends Workable {
  override def workOn(effort: Int): Workable = {
    val newEffort = workEffort - effort

    if (newEffort <= 0) {
      Diamond(id)
    } else {
      FlawedDiamond(id, newEffort)
    }
  }
}
