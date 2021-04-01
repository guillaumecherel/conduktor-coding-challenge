package ccc.kafka

import zio._
import zio.console._

/**
  * An interface to a fake kafka service for testing purpose.
  *
  */
case class KafkaTest() extends KafkaInterface {

    @Override
    def connect(bootstrapAddress: String,
        kafkaProperties: Vector[(String, String)]): ZIO[Any, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.bootstrapAddress = bootstrapAddress.nonEmpty match {
                case false => None
                case true => Some(bootstrapAddress)
            } 
        }

    @Override
    def isConnected(): ZIO[Any, TransitionFailure, Boolean] =
        ZIO.succeed(bootstrapAddress.nonEmpty)

    @Override
    def disconnect(): ZIO[Any, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.bootstrapAddress = None
            this.selectedTopic = None
            this.selectedPartitions = Vector.empty
        }

    @Override
    def openTopic(topicName: String): ZIO[Any, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.selectedTopic = topicName.nonEmpty match {
                case false => None
                case true => Option(topicName) 
            }
        }

    @Override
    def hasTopicOpened(): ZIO[Any, TransitionFailure, Boolean] = 
        ZIO.succeed(selectedTopic.nonEmpty)

    @Override
    def closeTopic(): ZIO[Any, TransitionFailure, Unit] =
        ZIO.succeed(())

    @Override
    def listPartitions(): ZIO[Any, TransitionFailure, Vector[Int]] =
        ZIO.succeed(this.partitions)

    @Override
    def listTopics(): ZIO[Any, TransitionFailure, Vector[String]] =
        ZIO.succeed(this.topicList)

    def seekToBeginning(selectedPartitions: Vector[Int]): ZIO[Any, TransitionFailure, Unit] = 
        ZIO.succeed(())

    @Override
    def poll(): ZIO[Any, TransitionFailure, Vector[String]] =
        ZIO.succeed {
            this.selectedTopic match {
                case None => throw new RuntimeException("No topic selected")
                case Some("topic1") => 
                    recordsPartitions
                    .filter { case (rec, par) => 
                        this.selectedPartitions.contains(par) 
                    }
                    .map {_._1}
                case Some(_) => Vector.empty
            }
        }

    @Override
    def assignPartitions(partition: Vector[Int]): ZIO[Any, TransitionFailure, Unit] =
        ZIO.effectTotal {
            this.selectedPartitions = partition
        }

    var bootstrapAddress: Option[String] = None
    var topicList: Vector[String] = Vector("topic1", "topic2", "topic3")
    var selectedTopic: Option[String] = None
    var partitions: Vector[Int] = Vector(0, 1, 2)
    var selectedPartitions: Vector[Int] = Vector()
    var recordsPartitions: Vector[(String, Int)] = Vector(("a", 0), ("b", 1), ("c", 2))
}
