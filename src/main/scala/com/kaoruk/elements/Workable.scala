package com.kaoruk.elements

trait Workable {
  val workEffort: Int
  def workOn(effort: Int): Workable
}
