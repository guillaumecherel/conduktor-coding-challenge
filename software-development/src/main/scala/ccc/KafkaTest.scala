package ccc

import zio._
import zio.console._

case class KafkaTest() extends KafkaInterface {

    @Override
    def connect(bootstrapAddress: String,
        adminProperties: Vector[(String, String)]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.bootstrapAddress = bootstrapAddress.nonEmpty match {
                case false => None
                case true => Some(bootstrapAddress)
            } 

            ()
        }

    @Override
    def isConnected(): ZIO[Env, Throwable, Boolean] =
        ZIO.succeed(bootstrapAddress.nonEmpty)

    @Override
    def disconnect(): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.bootstrapAddress = None
            this.topicList = Vector.empty
            this.selectedTopic = None
            this.selectedPartitions = Vector.empty
        }

    @Override
    def openTopic(topicName: String): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.selectedTopic = topicName.nonEmpty match {
                case false => None
                case true => Option(topicName) 
            }
        }

    @Override
    def hasTopicOpened(): ZIO[Env, Throwable, Boolean] = 
        ZIO.succeed(selectedTopic.nonEmpty)

    @Override
    def closeTopic(): ZIO[Env, Throwable, Unit] =
        ZIO.succeed(())

    @Override
    def listPartitions(): ZIO[Env, Throwable, Vector[Int]] =
        ZIO.succeed(this.partitions)

    @Override
    def listTopics(): ZIO[Env, Throwable, Vector[String]] =
        ZIO.succeed(this.topicList)

    @Override
    def poll(): ZIO[Env, Throwable, Vector[String]] =
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
    def assignPartitions(partition: Vector[Int]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.selectedPartitions = partition
        }

    var bootstrapAddress: Option[String] = None
    var topicList: Vector[String] = Vector("topic1", "topic2", "topic3")
    var selectedTopic: Option[String] = None
    var partitions: Vector[Int] = Vector(0, 1, 2)
    var selectedPartitions: Vector[Int] = Vector()
    var recordsPartitions: Vector[(String, Int)] = Vector(("a", 0), ("b", 1), ("c", 2))
}
