package ccc

import zio._

trait UIInterface {
    def getBootstrapAddress(): ZIO[Env, Throwable, String]
    def getSelectedPartitions(): ZIO[Env, Throwable, Vector[Int]]
    def getKafkaProperties(): ZIO[Env, Throwable, Vector[(String, String)]]
    def getAskConnect(): ZIO[Env, Throwable, Boolean]
    def getSelectedTopic(): ZIO[Env, Throwable, Option[String]]
    def getIsConsuming(): ZIO[Env, Throwable, Boolean]
    def setRecords(records: Vector[String]): ZIO[Env, Throwable, Unit]
    def appendRecords(records: Vector[String]): ZIO[Env, Throwable, Unit]
    def setAlert(msg: String): ZIO[Env, Throwable, Unit]
    def setTopics(topics: Vector[String]): ZIO[Env, Throwable, Unit]
    def setIsConnected(bool: Boolean):  ZIO[Env, Throwable, Unit]
    def setIsConsuming(bool: Boolean):  ZIO[Env, Throwable, Unit]
    def setAskConnect(bool: Boolean): ZIO[Env, Throwable, Unit]
    def setPartitions(partitions: Vector[Int], selectedPartitions: Vector[Int]): ZIO[Env, Throwable, Unit]
    def setSelectedPartitions(selectedPartitions: Vector[Int]): ZIO[Env, Throwable, Unit]
}
