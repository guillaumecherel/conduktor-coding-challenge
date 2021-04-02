package ccc.ui

import zio._

/**
  * A fake user interface interface for testing purpose.
  *
  */
case class UITest() extends UIInterface {

    @Override
    def getBootstrapAddress(): ZIO[Any, Nothing, String] =
        ZIO.succeed(bootstrapAddress)

    @Override
    def getKafkaProperties(): ZIO[Any, Nothing, Vector[(String, String)]] =
        ZIO.succeed(kafkaProperties)
        
    @Override
    def getAskConnect(): ZIO[Any, Nothing, Boolean] =
        ZIO.succeed(askConnect)

    @Override
    def getSelectedTopic(): ZIO[Any, Nothing, Option[String]] =
        ZIO.succeed(selectedTopic)

    @Override
    def getIsConsuming(): ZIO[Any, Nothing, Boolean] =
        ZIO.succeed(isConsuming)

    @Override
    def getSelectedPartitions(): ZIO[Any, Nothing, Vector[Int]] =
        ZIO.succeed(this.selectedPartitions)

    @Override
    def setInfo(msg: String): ZIO[Any, Nothing, Unit] = 
        ZIO.effectTotal {
            println("Info: " ++ msg)
        }

    @Override
    def setAlert(msg: String): ZIO[Any, Nothing, Unit] = 
        ZIO.effectTotal {
            println("Alert: " ++ msg)
        }

    @Override
    def setAskConnect(bool: Boolean): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.askConnect = bool
        }

    @Override
    def clearRecords(): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.records = Vector.empty
        }

    @Override
    def setPartitions(partitions: Vector[Int], selectedPartitions: Vector[Int]): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.partitions = partitions
            this.selectedPartitions = selectedPartitions
        }

    @Override
    def setSelectedPartitions(selectedPartitions: Vector[Int]): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.selectedPartitions = selectedPartitions
        }

    @Override
    def clearTopics(): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.topics = Vector.empty
        }

    @Override
    def setRecords(records: Vector[String]): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.records = records
        }

    @Override
    def appendRecords(records: Vector[String]): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.records = this.records.concat(records)
        }

    @Override
    def setTopics(topics: Vector[String]): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.topics = topics
        }

    @Override
    def setIsConsuming(bool: Boolean): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.isConsuming = bool
        }

    @Override
    def setIsConnected(bool: Boolean):  ZIO[Any, Nothing, Unit] =
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
