package org.apache.spark.logging

import org.apache.spark.logging.MailResolver.Mail

import scala.collection.mutable

object MailResolver{
  case class Mail(name:String, args:Array[Any])
}


class MailResolver {
  private val consumerHandlers = new mutable.HashMap[String, Array[Any] => Unit]()
  private val runnableHandlers = new mutable.HashMap[String, () => Unit]()

  def bind(fn:String, callback: Array[Any] => Unit): Unit ={
    consumerHandlers(fn) = callback
  }

  def bind(fn:String, callback:() => Unit): Unit ={
    runnableHandlers(fn) = callback
  }

  def canHandle(fn:String): Boolean ={
    consumerHandlers.contains(fn) || runnableHandlers.contains(fn)
  }

  def call(mail:Mail): Unit ={
    if(consumerHandlers.contains(mail.name)){
      consumerHandlers(mail.name)(mail.args)
    }else if(runnableHandlers.contains(mail.name)){
      runnableHandlers(mail.name)()
    }
  }
}
