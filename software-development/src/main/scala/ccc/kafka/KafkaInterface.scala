package ccc.kafka

import zio._

import ccc.errors._

/**
  * An abstract interface to a kafka service.
  */
trait KafkaInterface {
    def isConnected(): ZIO[Any, TransitionFailure, Boolean]
    def hasTopicOpened(): ZIO[Any, TransitionFailure, Boolean]
    def listPartitions(): ZIO[Any, TransitionFailure, Vector[Int]]
    def listTopics(): ZIO[Any, TransitionFailure, Vector[String]]
    def poll(): ZIO[Any, TransitionFailure, Vector[String]]
    def connect(
        bootstrapAddress: String,
        kafkaProperties: Vector[(String, String)]): ZIO[Any, TransitionFailure, Unit]
    def disconnect(): ZIO[Any, TransitionFailure, Unit]
    def openTopic(topicName: String): ZIO[Any, TransitionFailure, Unit]
    def seekToBeginning(selectedPartitions: Vector[Int]): ZIO[Any, TransitionFailure, Unit]
    def assignPartitions(partition: Vector[Int]): ZIO[Any, TransitionFailure, Unit]
    def closeTopic(): ZIO[Any, TransitionFailure, Unit]
}
