package ccc

import zio._

trait KafkaInterface {
    def isConnected(): ZIO[Env, TransitionFailure, Boolean]
    def hasTopicOpened(): ZIO[Env, TransitionFailure, Boolean]
    def listPartitions(): ZIO[Env, TransitionFailure, Vector[Int]]
    def listTopics(): ZIO[Env, TransitionFailure, Vector[String]]
    def poll(): ZIO[Env, TransitionFailure, Vector[String]]
    def connect(
        bootstrapAddress: String,
        kafkaProperties: Vector[(String, String)]): ZIO[Any, TransitionFailure, Unit]
    def disconnect(): ZIO[Env, TransitionFailure, Unit]
    def openTopic(topicName: String): ZIO[Env, TransitionFailure, Unit]
    def seekToBeginning(selectedPartitions: Vector[Int]): ZIO[Any, TransitionFailure, Unit]
    def assignPartitions(partition: Vector[Int]): ZIO[Env, TransitionFailure, Unit]
    def closeTopic(): ZIO[Env, TransitionFailure, Unit]
}
