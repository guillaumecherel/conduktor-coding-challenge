package ccc

import zio._

sealed trait State

final case class Disconnected() extends State

final case class InitiatingConnection() extends State

final case class Connecting(
    bootstrapAddress: String,
    kafkaProperties: Vector[(String, String)]
) extends State

final case class Connected(
    bootstrapAddress: String,
    kafkaProperties: Vector[(String, String)],
    topicNames: Vector[String],
    topic: Topic
) extends State

final case class ChangingBootstrap() extends State

object State {

    def update(connection: State): ZIO[Env, Throwable, State] = 
        connection match {
            case _ : Disconnected =>
                userAsksConnect(connection)
                .orElse(nothing(connection))
            case _ : InitiatingConnection => 
                connecting(connection)
                .orElse(nothing(connection))
            case _ : Connecting => 
                userAsksConnect(connection)
                .orElseOptional(connectionActualized(connection))
                .orElse(nothing(connection))
            case _ : Connected =>
                userAsksConnect(connection)
                .orElseOptional(updateTopic(connection))
                .orElse(nothing(connection))
            case _ : ChangingBootstrap => 
                disconnecting(connection)
                .orElse(nothing(connection))
        }

    def userAsksConnect(connection: State): 
    ZIO[Env, Option[Throwable], State] =
        (connection match {
            case Disconnected() =>
                for {
                    curAskConnect <- Env.ui(_.getAskConnect())
                    _ <- Env.ui(_.setAskConnect(false))
                } yield Option.when(curAskConnect) {
                    println("Initiating Connection")
                    InitiatingConnection()
                }
            case c: Connected =>
                for {
                    curAskConnect <- Env.ui(_.getAskConnect())
                    _ <- Env.ui(_.setAskConnect(false))
                } yield Option.when(curAskConnect) {
                    println("Changing bootstrap.")
                    ChangingBootstrap()
                }
            case c: Connecting =>
                for {
                    curAskConnect <- Env.ui(_.getAskConnect())
                    _ <- Env.ui(_.setAskConnect(false))
                } yield Option.when(curAskConnect) {
                    println("Changing bootstrap while connecting.")
                    ChangingBootstrap()
                }
            case other => ZIO.succeed(None)
        }).some

    def disconnecting(connection: State):
    ZIO[Env, Option[Throwable], State] =
        (connection match {
            case c: Connected =>
                for {
                    _ <- Env.kafka(_.disconnect())
                    _ <- Env.ui(_.setRecords(Vector.empty))
                    _ <- Env.ui(_.setTopics(Vector.empty))
                } yield Some {
                    println("Disconnecting.")
                    InitiatingConnection()
                }
            case other => ZIO.succeed(None)
        }).some

    def connecting(connection: State): ZIO[Env, Option[Throwable], State] =
        (connection match {
            case _: InitiatingConnection =>
                for {
                    bootstrapAddress <- Env.ui(_.getBootstrapAddress())
                    kafkaProperties <- Env.ui(_.getKafkaProperties())
                    _ <- Env.kafka(_.connect(bootstrapAddress, kafkaProperties))
                    _ <- Env.ui(_.setRecords(Vector.empty))
                    _ <- Env.ui(_.setTopics(Vector.empty))
                    _ <- Env.ui(_.setIsConnected(false))
                } yield {
                        println("Connecting to " ++ bootstrapAddress.toString())
                        Some(Connecting(bootstrapAddress, kafkaProperties))
                }
            case other => ZIO.succeed(None)
        }).some

    def connectionActualized(connection: State): 
    ZIO[Env, Option[Throwable], State] =
        (connection match {
            case Connecting(bootstrapAddress, kafkaProperties) =>
                for {
                    isConnected <- Env.kafka(_.isConnected())
                    topicNames <- isConnected match {
                        case false => ZIO.succeed(Vector.empty)
                        case true => Env.kafka(_.listTopics())
                    }
                    _ <- Env.ui(_.setIsConnected(isConnected))
                    _ <- Env.ui(_.setTopics(topicNames))
                } yield Option.when(isConnected){
                    println("Is Connected, topics:" ++ topicNames.toString())
                    Connected(bootstrapAddress, kafkaProperties,
                        topicNames, NoTopic())
                }
            case other => ZIO.succeed(None)
        }).some

    def updateTopic(connection: State): ZIO[Env, Option[Throwable], State] = 
        (connection match {
            case c : Connected => 
                for {
                    newTopic <- Topic.update(c.topic)
                    } yield Some {
                        c.copy(topic = newTopic)
                    }
            case other => ZIO.succeed(None)
        }).some

    def nothing(connection: State): ZIO[Env, Throwable, State] = 
        ZIO.succeed(connection)
   
}

