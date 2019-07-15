package com.kaoruk.elements

case class RetryEnvelope[T](item: T, attempts: Int)
