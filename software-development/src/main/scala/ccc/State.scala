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

    def step (connection: State): ZIO[Env, Throwable, State] = 
        update(connection).mapError {
            case OtherError(cause) => cause
            case other => new RuntimeException("Unhandled exception: " ++ other.toString())
        }

    def update(connection: State): ZIO[Env, TransitionFailure, State] = 
        connection match {
            case c : Disconnected =>
                userAsksConnect(c)
                .catchSome { 
                    case TransitionNotTriggered() => nothing(c)
                }
            case c : InitiatingConnection => 
                connecting(c)
                .catchSome { 
                    case ConnectionFailed(msg) => connectionFailed(c, msg) 
                    case TransitionNotTriggered() => nothing(c)
                }
            case c : Connecting => 
                userAsksConnect(c)
                .orElse(connectionActualized(c))
                .catchSome { 
                    case ConnectionFailed(msg) => connectionFailed(c, msg) 
                    case TransitionNotTriggered() => nothing(c)
                }
            case c : Connected =>
                userAsksConnect(c)
                .orElse(updateTopic(c))
                .catchSome { 
                    case TransitionNotTriggered() => nothing(c)
                }
            case c : ChangingBootstrap => 
                disconnecting(c) 
                .catchSome { 
                    case TransitionNotTriggered() => nothing(c)
                }
        }

    def userAsksConnect(connection: Disconnected): 
    ZIO[Env, TransitionFailure, State] =
        for {
            curAskConnect <- Env.ui(_.getAskConnect()).orDie
            _ <- ZIO.unless(curAskConnect)(ZIO.fail(TransitionNotTriggered()))
            _ <- Env.ui(_.setAskConnect(false)).orDie
            _ <- Env.ui(_.setAlert("")).orDie
        } yield {
            println("Initiating Connection")
            InitiatingConnection()
        }

    def userAsksConnect(connection: Connected): 
    ZIO[Env, TransitionFailure, State] =
        for {
            curAskConnect <- Env.ui(_.getAskConnect()).orDie
            _ <- ZIO.when(!curAskConnect)(ZIO.fail(TransitionNotTriggered()))
            _ <- Env.ui(_.setAskConnect(false)).orDie
        } yield {
            println("Changing bootstrap.")
            ChangingBootstrap()
        }

    def userAsksConnect(connection: Connecting): 
    ZIO[Env, TransitionFailure, State] =
        for {
            curAskConnect <- Env.ui(_.getAskConnect()).orDie
            _ <- ZIO.when(!curAskConnect)(ZIO.fail(TransitionNotTriggered()))
            _ <- Env.ui(_.setAskConnect(false)).orDie
        } yield {
            println("Changing bootstrap while connecting.")
            ChangingBootstrap()
        }

    def disconnecting(connection: Connected):
    ZIO[Env, TransitionFailure, State] =
        for {
            _ <- Env.kafka(_.disconnect()).orDie
            _ <- Env.ui(_.setRecords(Vector.empty)).orDie
            _ <- Env.ui(_.setTopics(Vector.empty)).orDie
            _ <- Env.ui(_.setIsConnected(false)).orDie
            _ <- Env.ui(_.setPartitions(Vector.empty, Vector.empty)).orDie
        } yield {
            println("Disconnecting.")
            InitiatingConnection()
        }

    def disconnecting(connection: ChangingBootstrap):
    ZIO[Env, TransitionFailure, State] =
        for {
            _ <- Env.kafka(_.disconnect()).orDie
            _ <- Env.ui(_.setRecords(Vector.empty)).orDie
            _ <- Env.ui(_.setTopics(Vector.empty)).orDie
            _ <- Env.ui(_.setIsConnected(false)).orDie
            _ <- Env.ui(_.setPartitions(Vector.empty, Vector.empty)).orDie
        } yield {
            println("Disconnecting.")
            InitiatingConnection()
        }

    def connecting(connection: InitiatingConnection): 
    ZIO[Env, TransitionFailure, State] =
        for {
            bootstrapAddress <- Env.ui(_.getBootstrapAddress()).orDie
            _ <- ZIO.unless(bootstrapAddress != null && 
                bootstrapAddress.nonEmpty)(ZIO.fail(ConnectionFailed("Please enter a bootstrap address.")))
            kafkaProperties <- Env.ui(_.getKafkaProperties()).orDie
            _ <- Env.kafka(_.connect(bootstrapAddress, kafkaProperties)).orDie
            _ <- Env.ui(_.setRecords(Vector.empty)).orDie
            _ <- Env.ui(_.setTopics(Vector.empty)).orDie
            _ <- Env.ui(_.setIsConnected(false)).orDie
        } yield {
            println("Connecting to " ++ bootstrapAddress.toString())
            Connecting(bootstrapAddress, kafkaProperties)
        }

    def connectionActualized(connection: Connecting): 
    ZIO[Env, TransitionFailure, State] =
        for {
            isConnected <- Env.kafka(_.isConnected()).orDie
            _ <- ZIO.unless(isConnected)(ZIO.fail(TransitionNotTriggered()))
            topicNames <- Env.kafka(_.listTopics()).orDie
            _ <- Env.ui(_.setIsConnected(isConnected)).orDie
            _ <- Env.ui(_.setTopics(topicNames)).orDie
        } yield {
            println("Is Connected, topics:" ++ topicNames.toString())
            Connected(connection.bootstrapAddress, 
                connection.kafkaProperties, topicNames, NoTopic())
        }

    def updateTopic(connection: Connected): ZIO[Env, TransitionFailure, State] = 
        for {
            newTopic <- Topic.update(connection.topic)
        } yield {
            connection.copy(topic = newTopic)
        }

    def connectionFailed(connection: State, msg: String): 
    ZIO[Env, TransitionFailure, State] =
        for {
            _ <- Env.ui(_.setAlert(s"Connection failed. $msg")).orDie
        } yield {
            println("Connection failed.")
            Disconnected()
        }

    def nothing(connection: State): ZIO[Env, TransitionFailure, State] = 
        ZIO.succeed(connection)
   
}

