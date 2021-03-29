package ccc

import zio._

case class UITest() extends UIInterface {

    @Override
    def getBootstrapAddress(): ZIO[Env, Throwable, String] =
        ZIO.succeed(bootstrapAddress)

    @Override
    def getKafkaProperties(): ZIO[Env, Throwable, Vector[(String, String)]] =
        ZIO.succeed(kafkaProperties)
        
    @Override
    def getAskConnect(): ZIO[Env, Throwable, Boolean] =
        ZIO.succeed(askConnect)

    @Override
    def getSelectedTopic(): ZIO[Env, Throwable, Option[String]] =
        ZIO.succeed(selectedTopic)

    @Override
    def getIsConsuming(): ZIO[Env, Throwable, Boolean] =
        ZIO.succeed(isConsuming)

    @Override
    def getSelectedPartitions(): ZIO[Env, Throwable, Vector[Int]] =
        ZIO.succeed(this.selectedPartitions)

    @Override
    def setAskConnect(bool: Boolean): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.askConnect = bool
        }

    @Override
    def clearRecords(): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.records = Vector.empty
        }

    @Override
    def setPartitions(partitions: Vector[Int], selectedPartitions: Vector[Int]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.partitions = partitions
            this.selectedPartitions = selectedPartitions
        }

    @Override
    def setSelectedPartitions(selectedPartitions: Vector[Int]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.selectedPartitions = selectedPartitions
        }

    @Override
    def clearTopics(): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.topics = Vector.empty
        }

    @Override
    def setRecords(records: Vector[String]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.records = records
        }

    @Override
    def appendRecords(records: Vector[String]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.records = this.records.concat(records)
        }

    @Override
    def setTopics(topics: Vector[String]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.topics = topics
        }

    @Override
    def setIsConsuming(bool: Boolean): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.isConsuming = bool
        }

    @Override
    def setIsConnected(bool: Boolean):  ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.isConnected = bool
        } 

    var partitions: Vector[Int] = Vector(0,1,2)
    var selectedPartitions: Vector[Int] = Vector(0,1,2)
    var bootstrapAddress: String = ""
    var kafkaProperties: Vector[(String, String)] = Vector.empty
    var askConnect: Boolean = false
    var records: Vector[String] = Vector.empty
    var topics: Vector[String] = Vector.empty
    var isConsuming: Boolean = false
    var isConnected: Boolean = false
    var selectedTopic: Option[String] = None
}
