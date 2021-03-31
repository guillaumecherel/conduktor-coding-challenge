package ccc

import zio._

import scala.collection.immutable._
import scala.collection.JavaConverters

import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.TopicPartition 
import org.apache.kafka.common.errors._

import org.apache.kafka.common.KafkaException

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord

import java.util.Properties
import java.time.Duration

import java.util.{Set => JavaSet}

/**
  * The actual interface to kafka used in the application. This object's 
  * effectful methods (ZIO[Any, _, _]) mutate the internal variable state.
  */
object KafkaService extends KafkaInterface {

    trait KafkaState 
    case class Disconnected() extends KafkaState
    case class Connected(
        admin: Admin, 
        props: Properties,
        topics: KafkaFuture[Vector[String]]) 
        extends KafkaState
    case class TopicOpened(
        admin: Admin,
        props: Properties,
        consumer: KafkaConsumer[String, String],
        currentTopic: String,
        topics: Vector[String])
        extends KafkaState

    val PollDuration = Duration.ofMillis(100)

    var state: KafkaState = Disconnected()

    def makeProperties(
        bootstrapAddress: String,
        kafkaProperties: Vector[(String, String)]): Properties = {

        val defaultProps: Map[String, String] = Map(
            ("key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer"),
            ("value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer")
        )

        val propsMap: Map[String, String] = 
            (Map.from(defaultProps) ++ kafkaProperties)
            .updatedWith("bootstrap.servers") {
                case None => Some(s"$bootstrapAddress")
                case Some(list) => Some(list ++ s",$bootstrapAddress")
            }

        var props = new Properties()
        propsMap.foreach { 
            case (k,v) => props.setProperty(k, v)
        }

        props
    }

    val toScalaVector = new KafkaFuture.BaseFunction[JavaSet[String], Vector[String]]() {
        def apply(a: JavaSet[String]): Vector[String] = 
            JavaConverters.asScala(a).toVector
    }

    @Override
    def connect(
        bootstrapAddress: String,
        kafkaProperties: Vector[(String, String)]): ZIO[Any, TransitionFailure, Unit] =
        state match {
            case Disconnected() => 
                ZIO.effect {
                    val props = makeProperties(bootstrapAddress, 
                        kafkaProperties)

                    val admin = Admin.create(props) 

                    val topics = admin.listTopics().names()
                        .thenApply(toScalaVector)

                    state = Connected(admin, props, topics)
                }.mapError { _ match {
                    case e: KafkaException => ConnectionFailed("Could not " ++
                        s"connect to $bootstrapAddress. There might be an " ++
                        "error in the bootstrap address or the properties. " ++
                        "Please enter a bootstrap address in the form " ++ 
                        "ADDRESS:PORT.", e)
                    case e => OtherError(e) 
                }}
            case other => 
                disconnect() *> 
                connect(bootstrapAddress, kafkaProperties)
        }

    @Override
    def listTopics(): ZIO[Any, TransitionFailure, Vector[String]] = 
        state match {
            case Connected(_, _, topics) =>
                ZIO.when(topics.isCancelled())(
                    ZIO.fail(ResponseLost("Could not list topic."))) *>
                ZIO.unless(topics.isDone())(ZIO.fail(ResponseNotReady())) *>
                ZIO.effectTotal(topics.get())
            case TopicOpened(_, _, _, curTopic, topics) => ZIO.succeed(topics)
            case Disconnected() => ZIO.fail(TransitionNotTriggered())
        }

    @Override
    def disconnect(): ZIO[Any, TransitionFailure, Unit] = 
        state match {
            case Disconnected() => ZIO.succeed(())
            case Connected(admin, props, topics) => 
                ZIO.effectTotal {
                    admin.close()
                    state = Disconnected()
                }
            case TopicOpened(admin, props, consumer, curTopic, topics) =>
                closeTopic() *> disconnect()
        }

    @Override
    def isConnected(): ZIO[Any, TransitionFailure, Boolean] = 
        state match {
            case _: Disconnected => ZIO.succeed(false)
            case _: Connected => ZIO.succeed(true)
            case _: TopicOpened => ZIO.succeed(true)
        }

    @Override
    def openTopic(topicName: String): ZIO[Any, TransitionFailure, Unit] = 
        state match {
            case Connected(admin, props, topics) => {
                for {
                    theTopics <- listTopics()
                    consumer <- ZIO.effectTotal {
                        new KafkaConsumer[String, String](props)
                    }
                    _ <- ZIO.effectTotal {
                        state = TopicOpened(admin, props, consumer, topicName, 
                            theTopics)
                    }
                } yield ()
            }
            case TopicOpened(admin, props, consumer, curTopic, topics) => {
                for {
                    _ <- closeTopic() 
                    _ <- openTopic(topicName)
                } yield ()
            }
            case Disconnected() => ZIO.fail(TransitionNotTriggered())
        }

    def seekToBeginning(selectedPartitions: Vector[Int]): ZIO[Any, TransitionFailure, Unit] = 
        state match {
            case TopicOpened(admin, props, consumer, curTopic, topics) => {
                for {
                    _ <- ZIO.when(selectedPartitions.nonEmpty) {
                        ZIO.effect{
                            consumer.seekToBeginning(JavaConverters.asJava(
                                toTopicPartition(curTopic, selectedPartitions)))
                        }
                    }.mapError(e => OtherError(e))
                } yield()
            }
            case other => ZIO.fail(TransitionNotTriggered())
        }

    @Override
    def hasTopicOpened(): ZIO[Any, TransitionFailure, Boolean] = 
        state match {
            case _: Disconnected => ZIO.succeed(false)
            case _: Connected => ZIO.succeed(false)
            case _: TopicOpened => ZIO.succeed(true)
        }

    @Override
    def listPartitions(): ZIO[Any, TransitionFailure, Vector[Int]] = 
        state match {
            case _: Disconnected => ZIO.fail(TransitionNotTriggered())
            case _: Connected => ZIO.fail(TransitionNotTriggered())
            case TopicOpened(admin, props, consumer, curTopic, topics) => 
                ZIO.effect {
                    JavaConverters.asScala(consumer.partitionsFor(curTopic))
                        .map(_.partition())
                        .toVector
                }.mapError {
                    case e: AuthenticationException => 
                        ConnectionFailed("Authentication failed, please " ++
                            "check credentials. ", e)
                    case e: AuthorizationException =>
                        ConnectionFailed("You are not authorized " ++
                            "to access the list of partitions for the topic " ++ 
                            curTopic, e)
                    case other => OtherError(other)
                }
        }

    @Override
    def poll(): ZIO[Any, TransitionFailure, Vector[String]] =
        state match {
            case _: Disconnected => ZIO.fail(TransitionNotTriggered())
            case _: Connected => ZIO.fail(TransitionNotTriggered())
            case TopicOpened(admin, props, consumer, curTopic, topics) => 
                ZIO.effect{
                    SortedSet
                        .from(JavaConverters.asScala(
                            consumer.poll(PollDuration).iterator
                        ))(recordOrdering)
                        .map { record => 
                            val key = record.key()
                            val value = record.value()
                            val keySize = record.serializedKeySize()
                            val valSize = record.serializedValueSize()
                            val partition = record.partition()
                            val offset = record.offset()
                            val timestamp = record.timestamp()
                            s"Key ($keySize bytes): '$key'\n" ++
                            s"Value ($valSize bytes): '$value'\n" ++
                            s"Partition: $partition\tOffset: $offset\tTimestamp: $timestamp"
                        }
                        .toVector
                }
                .mapError {
                    // IllegalStateException thrown when the consumer is not 
                    // assigned to any partition. There is nothing to poll.
                    case e: IllegalStateException => TransitionNotTriggered()
                    case e: AuthenticationException => 
                        ConnectionFailed("Authentication failed, check " ++
                            " credentials.", e)
                    case e: AuthorizationException =>
                        ConnectionFailed("You are not authorized " ++
                            "to access the list of partitions for the topic " ++ 
                            curTopic, e)
                    case other => OtherError(other)
                }
        }

    val recordOrdering: Ordering[ConsumerRecord[String, String]] =
        Ordering.by { record: ConsumerRecord[String, String] => 
            (record.timestamp(), record.offset(), record.partition()) 
        }

    @Override
    def assignPartitions(partitions: Vector[Int]): ZIO[Any, TransitionFailure, Unit] = 
        state match {
            case _: Disconnected => ZIO.fail(TransitionNotTriggered())
            case _: Connected => ZIO.fail(TransitionNotTriggered())
            case TopicOpened(admin, props, consumer, curTopic, topics) => 
                ZIO.effect {
                    val topicPartitions = toTopicPartition(curTopic, partitions)
                    consumer.assign(JavaConverters.asJava(topicPartitions) )
                }.mapError {
                    e => OtherError(e)
                }
        }

    def toTopicPartition(topic: String, partitions: Vector[Int]): 
        Vector[TopicPartition] = {
        partitions.map { 
            i => new TopicPartition(topic, i)
        }
    }

    @Override
    def closeTopic(): ZIO[Any, TransitionFailure, Unit] =
        state match {
            case _: Disconnected => ZIO.fail(TransitionNotTriggered())
            case _: Connected => ZIO.fail(TransitionNotTriggered())
            case TopicOpened(admin, props, consumer, curTopic, topics) =>
                ZIO.effectTotal {
                    consumer.close()
                    val topics_ = KafkaFuture.completedFuture(topics)
                    state = Connected(admin, props, topics_ )
                }
    }
}

