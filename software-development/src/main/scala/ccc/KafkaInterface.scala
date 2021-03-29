package ccc

import zio._

trait KafkaInterface {
    def isConnected(): ZIO[Env, Throwable, Boolean]
    def hasTopicOpened(): ZIO[Env, Throwable, Boolean]
    def listPartitions(): ZIO[Env, Throwable, Vector[Int]]
    def listTopics(): ZIO[Env, Throwable, Vector[String]]
    def poll(): ZIO[Env, Throwable, Vector[String]]
    def connect(bootstrapAddress: String,
        adminProperties: Vector[(String, String)]): ZIO[Env, Throwable, Unit]
    def disconnect(): ZIO[Env, Throwable, Unit]
    def openTopic(topicName: String): ZIO[Env, Throwable, Unit]
    def assignPartitions(partition: Vector[Int]): ZIO[Env, Throwable, Unit]
    def closeTopic(): ZIO[Env, Throwable, Unit]
}
