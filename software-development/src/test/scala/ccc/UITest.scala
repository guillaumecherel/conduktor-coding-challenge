package ccc

import zio._

case class UITest() extends UIInterface {

    @Override
    def getBootstrapAddress(): ZIO[Env, TransitionFailure, String] =
        ZIO.succeed(bootstrapAddress)

    @Override
    def getKafkaProperties(): ZIO[Env, TransitionFailure, Vector[(String, String)]] =
        ZIO.succeed(kafkaProperties)
        
    @Override
    def getAskConnect(): ZIO[Env, TransitionFailure, Boolean] =
        ZIO.succeed(askConnect)

    @Override
    def getSelectedTopic(): ZIO[Env, TransitionFailure, Option[String]] =
        ZIO.succeed(selectedTopic)

    @Override
    def getIsConsuming(): ZIO[Env, TransitionFailure, Boolean] =
        ZIO.succeed(isConsuming)

    @Override
    def getSelectedPartitions(): ZIO[Env, TransitionFailure, Vector[Int]] =
        ZIO.succeed(this.selectedPartitions)

    @Override
    def setAlert(msg: String): ZIO[Env, TransitionFailure, Unit] = 
        ZIO.effectTotal {
            println("Alert: " ++ msg)
        }

    @Override
    def setAskConnect(bool: Boolean): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.askConnect = bool
        }

    @Override
    def clearRecords(): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.records = Vector.empty
        }

    @Override
    def setPartitions(partitions: Vector[Int], selectedPartitions: Vector[Int]): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.partitions = partitions
            this.selectedPartitions = selectedPartitions
        }

    @Override
    def setSelectedPartitions(selectedPartitions: Vector[Int]): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.selectedPartitions = selectedPartitions
        }

    @Override
    def clearTopics(): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.topics = Vector.empty
        }

    @Override
    def setRecords(records: Vector[String]): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.records = records
        }

    @Override
    def appendRecords(records: Vector[String]): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.records = this.records.concat(records)
        }

    @Override
    def setTopics(topics: Vector[String]): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.topics = topics
        }

    @Override
    def setIsConsuming(bool: Boolean): ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.isConsuming = bool
        }

    @Override
    def setIsConnected(bool: Boolean):  ZIO[Env, TransitionFailure, Unit] =
        ZIO.effectTotal {
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
