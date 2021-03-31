package ccc

import zio._

trait UIInterface {
    def getBootstrapAddress(): ZIO[Env, TransitionFailure, String]
    def getSelectedPartitions(): ZIO[Env, TransitionFailure, Vector[Int]]
    def getKafkaProperties(): ZIO[Env, TransitionFailure, Vector[(String, String)]]
    def getAskConnect(): ZIO[Env, TransitionFailure, Boolean]
    def getSelectedTopic(): ZIO[Env, TransitionFailure, Option[String]]
    def getIsConsuming(): ZIO[Env, TransitionFailure, Boolean]
    def setRecords(records: Vector[String]): ZIO[Env, TransitionFailure, Unit]
    def appendRecords(records: Vector[String]): ZIO[Env, TransitionFailure, Unit]
    def setAlert(msg: String): ZIO[Env, TransitionFailure, Unit]
    def setTopics(topics: Vector[String]): ZIO[Env, TransitionFailure, Unit]
    def setIsConnected(bool: Boolean):  ZIO[Env, TransitionFailure, Unit]
    def setIsConsuming(bool: Boolean):  ZIO[Env, TransitionFailure, Unit]
    def setAskConnect(bool: Boolean): ZIO[Env, TransitionFailure, Unit]
    def setPartitions(partitions: Vector[Int], selectedPartitions: Vector[Int]): ZIO[Env, TransitionFailure, Unit]
    def setSelectedPartitions(selectedPartitions: Vector[Int]): ZIO[Env, TransitionFailure, Unit]
}
