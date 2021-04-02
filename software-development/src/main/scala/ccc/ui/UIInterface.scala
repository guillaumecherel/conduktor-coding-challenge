package ccc.ui

import zio._

import ccc.errors._

/**
  * An abstract interface to the user interface.
  */
trait UIInterface {
    def getBootstrapAddress(): ZIO[Any, TransitionFailure, String]
    def getSelectedPartitions(): ZIO[Any, TransitionFailure, Vector[Int]]
    def getKafkaProperties(): ZIO[Any, TransitionFailure, Vector[(String, String)]]
    def getAskConnect(): ZIO[Any, TransitionFailure, Boolean]
    def getSelectedTopic(): ZIO[Any, TransitionFailure, Option[String]]
    def getIsConsuming(): ZIO[Any, TransitionFailure, Boolean]
    def setRecords(records: Vector[String]): ZIO[Any, TransitionFailure, Unit]
    def appendRecords(records: Vector[String]): ZIO[Any, TransitionFailure, Unit]
    def setAlert(msg: String): ZIO[Any, TransitionFailure, Unit]
    def setInfo(msg: String): ZIO[Any, TransitionFailure, Unit]
    def setTopics(topics: Vector[String]): ZIO[Any, TransitionFailure, Unit]
    def setIsConnected(bool: Boolean):  ZIO[Any, TransitionFailure, Unit]
    def setIsConsuming(bool: Boolean):  ZIO[Any, TransitionFailure, Unit]
    def setAskConnect(bool: Boolean): ZIO[Any, TransitionFailure, Unit]
    def setPartitions(partitions: Vector[Int], selectedPartitions: Vector[Int]): ZIO[Any, TransitionFailure, Unit]
    def setSelectedPartitions(selectedPartitions: Vector[Int]): ZIO[Any, TransitionFailure, Unit]
}
