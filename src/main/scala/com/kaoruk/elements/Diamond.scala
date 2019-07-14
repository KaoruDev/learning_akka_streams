package com.kaoruk.elements

case class Diamond(id: Int) extends Workable {
  override val workEffort = 0

  override def workOn(effort: Int): Workable = {
    this
  }
}
