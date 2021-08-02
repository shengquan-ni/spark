package org.apache.spark.logging

import java.util.concurrent.CompletableFuture

class FutureWrapper {

  private var future:CompletableFuture[Void] = _

  def get(): Unit ={
    if(future!=null){
      future.get()
    }
  }

  def reset(): Unit ={
    future = new CompletableFuture[Void]()
  }

  def set(): Unit ={
    if(future != null){
      future.complete(null)
    }
  }

}
