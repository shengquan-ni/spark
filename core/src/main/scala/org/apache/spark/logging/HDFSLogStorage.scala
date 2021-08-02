package org.apache.spark.logging

import java.io.{DataInputStream, DataOutputStream}

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.util.RecoveryUtils


class HDFSLogStorage(logName: String, hdfsIP: String) extends FileLogStorage(logName) {

  lazy val hdfs: FileSystem = RecoveryUtils.getHDFS(hdfsIP)

  private lazy val path = new Path(s"./logs/$logName.log")

  override def getInputStream: DataInputStream = hdfs.open(path)

  override def getOutputStream: DataOutputStream = {
    if (fileExists) {
      hdfs.append(path)
    } else {
      hdfs.create(path)
    }
  }

  override def fileExists: Boolean = hdfs.exists(path)

  override def createDirectories(): Unit = hdfs.mkdirs(path.getParent)

  override def deleteFile(): Unit = hdfs.delete(path, false)
}
