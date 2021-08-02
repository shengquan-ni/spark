package org.apache.spark.logging

import org.apache.spark.logging.AbstractLogStorage.LogRecord

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object MemoryStorage{
  val globalMap = new mutable.HashMap[String, MemoryStorage]()
}

class MemoryStorage(logName: String) extends AbstractLogStorage(logName)  {
  import MemoryStorage._
  var logs = new ArrayBuffer[LogRecord]()
  var loggedTimeroutputs = new ArrayBuffer[Long]()
  var loggedStepCursor:Long = 0

  if(globalMap.contains(logName)){
    logs = globalMap(logName).logs
    loggedTimeroutputs = globalMap(logName).loggedTimeroutputs
    loggedStepCursor = globalMap(logName).loggedStepCursor
  }
  globalMap(logName) = this

  override def write(record: LogRecord): Unit = {
    record match{
      case AbstractLogStorage.DPCursor(idx) =>
        logs.append(record)
        loggedStepCursor = idx
      case AbstractLogStorage.UpdateStepCursor(step) =>
        loggedStepCursor = step
      case AbstractLogStorage.TimerOutput(out) =>
        loggedTimeroutputs.append(out)
      case AbstractLogStorage.ControlRecord(controlName, controlArgs) =>
        logs.append(record)
    }
  }

  override def getStepCursor: Long = loggedStepCursor

  override def commit(): Unit = {}

  override def getLogs: Iterable[LogRecord] = logs

  override def clear(): Unit = {
    logs = new ArrayBuffer[LogRecord]()
    loggedTimeroutputs.clear()
    loggedStepCursor = 0
  }

  override def release(): Unit = {}

  override def getTimerOutputs: Array[Long] = loggedTimeroutputs.toArray

  override def truncateLog(): Unit = {
    clear()
  }
}
