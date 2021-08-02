package org.apache.spark.logging

abstract class AbstractLogManager {
  private var _isEnabled = false

  def enable():Unit = _isEnabled = true

  def disable():Unit = _isEnabled = false

  def isEnabled:Boolean = _isEnabled
}
