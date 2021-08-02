package org.apache.spark.logging

import org.apache.spark.logging.AbstractLogStorage.{ControlRecord, DPCursor}
import org.apache.spark.logging.MailResolver.Mail
import org.apache.spark.util.RecoveryUtils

import scala.collection.mutable

class DPLogManager(logWriter: AsyncLogWriter, mailResolver: MailResolver, val stepCursor: StepCursor) extends AbstractLogManager  {

  val controlQueue = new mutable.Queue[Mail]()
  val currentSender = "anywhere"
  var currentSeq = 0L
  val orderingManager = new FIFOManager[Mail, String]((s, m) => {
      controlQueue.enqueue(m)
  })
  private var checkpointLock:AnyRef = _
  def setCheckpointLock(obj:AnyRef): Unit = {
    checkpointLock = obj
  }

  def getCheckpointLock:AnyRef = checkpointLock


  // For recovery, only need to replay control messages, and then it's done
  logWriter.storage.getLogs.foreach {
    case ctrl: ControlRecord =>
      orderingManager.handleMessage(currentSender, currentSeq, Mail(ctrl.controlName, ctrl.controlArgs))
      currentSeq += 1
    case other =>
    //skip
  }

  //reset seq num for incoming control msgs
  currentSeq = 0L

  private val correlatedSeq = logWriter.storage.getLogs
    .collect {
      case DPCursor(idx) => idx
    }
    .to[mutable.Queue]


  def inputControl(mail:Mail): Unit ={
    orderingManager.handleMessage(currentSender, currentSeq, mail)
    currentSeq += 1
    if(stepCursor.isRecoveryCompleted){
      while(controlQueue.nonEmpty){
        val mail = controlQueue.dequeue()
        stepCursor.advance()
        persistCurrentControl(mail)
        mailResolver.call(mail)
      }
    }
  }

  def recoverControl(): Unit ={
    while(correlatedSeq.nonEmpty && correlatedSeq.head == stepCursor.getCursor+1){
      correlatedSeq.dequeue()
      val mail = controlQueue.dequeue()
      mailResolver.call(mail)
      stepCursor.advance()
    }
  }

  def persistCurrentControl(mail:Mail): Unit = {
    logWriter.addLogRecord(ControlRecord(mail.name, mail.args))
    logWriter.addLogRecord(DPCursor(stepCursor.getCursor))
  }
}
