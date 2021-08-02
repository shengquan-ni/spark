package org.apache.spark.logging

import java.util
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, LinkedBlockingQueue}

import com.google.common.collect.Queues
import org.apache.spark.logging.AbstractLogStorage.{ControlRecord, DPCursor, LogRecord, ShutdownWriter, TimerOutput, TruncateLog, UpdateStepCursor}
import org.apache.spark.logging.AsyncLogWriter.{CursorUpdate, NetworkOutputElem, OutputElem, ShutdownOutput}

import scala.collection.mutable

object AsyncLogWriter{
  sealed trait NetworkOutputElem
  case object ShutdownOutput extends NetworkOutputElem
  case class CursorUpdate(cursor:Long) extends NetworkOutputElem
  sealed trait OutputElem extends NetworkOutputElem{
    val cursor:Long
  }
}

class AsyncLogWriter(val storage:AbstractLogStorage){
  private var persistedStepCursor = storage.getStepCursor
  private var cursorUpdated = false
  val logRecordQueue: LinkedBlockingQueue[LogRecord] = Queues.newLinkedBlockingQueue()
  private val shutdownFuture = new CompletableFuture[Void]()

  def addLogRecord(logRecord: LogRecord): Unit = {
    logRecordQueue.put(logRecord)
  }

  def takeCheckpoint():Unit = {
    logRecordQueue.put(TruncateLog)
  }

  def shutdown(): CompletableFuture[Void] ={
    logRecordQueue.put(ShutdownWriter)
    shutdownFuture
  }

  private val loggingExecutor: ExecutorService = Executors.newSingleThreadExecutor
    loggingExecutor.submit(new Runnable() {
      def run(): Unit = {
        try {
          //Thread.currentThread().setPriority(Thread.MAX_PRIORITY)
          var isEnded = false
          val buffer = new util.ArrayList[LogRecord]()
          while (!isEnded) {
            logRecordQueue.drainTo(buffer)
            //if(storage.name == "exampleJob-0f02-0")System.out.println("drained = "+buffer.size())
            if (buffer.isEmpty) {
              // instead of using Thread.sleep(200),
              // we wait until 1 record has been pushed into the queue
              // then write this record and commit
              // during this process, we accumulate log records in the queue
              val logRecord = logRecordQueue.take()
              if (logRecord == ShutdownWriter) {
                isEnded = true
              }else{
                cursorUpdated = false //invalidate flag
                writeLogRecord(logRecord)
                persistStepCursor()
                storage.commit()
              }
            } else {
              if (buffer.get(buffer.size() - 1) == ShutdownWriter) {
                buffer.remove(buffer.size() - 1)
                isEnded = true
              }
              cursorUpdated = false // invalidate flag
              batchWrite(buffer)
              persistStepCursor()
              //println(s"writing ${buffer.size} logs at a time")
              storage.commit()
              buffer.clear()
            }
            //if(storage.name == "exampleJob-0f02-0") System.out.println("buffer size = "+logRecordQueue.size())
          }
          logRecordQueue.clear()
          storage.release()
          shutdownFuture.complete(null)
          loggingExecutor.shutdownNow()
        } catch {
          case e: Throwable =>
            e.printStackTrace()
        }
      }
    })

  private def batchWrite(buffer: util.ArrayList[LogRecord]): Unit = {
    buffer.toArray(Array[LogRecord]()).foreach(x => writeLogRecord(x))
  }

  private def writeLogRecord(record: LogRecord): Unit = {
    record match {
      case clr: ControlRecord =>
        storage.write(clr)
      case cursor: DPCursor =>
        persistedStepCursor = cursor.idx
        storage.write(cursor)
      case UpdateStepCursor(cursor) =>
        //only write the last step cursor of batched log entries
        if(cursor > persistedStepCursor){
          cursorUpdated = true
          persistedStepCursor = cursor
        }
      case t:TimerOutput =>
        storage.write(t)
      case TruncateLog =>
        storage.truncateLog()
      case ShutdownWriter =>
      //skip
    }
  }

  private def persistStepCursor(): Unit ={
    if(cursorUpdated){
      storage.write(UpdateStepCursor(persistedStepCursor))
    }
  }

}
