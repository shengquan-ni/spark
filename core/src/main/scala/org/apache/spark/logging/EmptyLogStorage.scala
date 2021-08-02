package org.apache.spark.logging

class EmptyLogStorage(logName: String) extends AbstractLogStorage(logName)  {
  override def write(record: AbstractLogStorage.LogRecord): Unit = {}

  override def getStepCursor: Long = 0

  override def commit(): Unit = {}

  override def getLogs: Iterable[AbstractLogStorage.LogRecord] = Iterable.empty

  override def getTimerOutputs: Array[Long] = Array.empty

  override def clear(): Unit = {}

  override def release(): Unit = {}

  override def truncateLog(): Unit = {}
}
