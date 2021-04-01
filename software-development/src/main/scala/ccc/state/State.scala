package ccc.state

import zio._

import java.util.Calendar

import ccc.Env
import ccc.errors._

/**
 * The application main state.
 */
sealed trait State

final case class Disconnected() extends State

final case class InitiatingConnection() extends State

final case class Connecting(
    bootstrapAddress: String,
    kafkaProperties: Vector[(String, String)],
    timestamp: Long
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
        }

    def update(connection: State): ZIO[Env, TransitionFailure, State] = 
        // The application is modelled as a finite state machine where 
        // the states are the possible values of type State and the 
        // transition are functions with return type 
        // ZIO[Env, TransitionFailure, State]. A transition is triggered
        // (succeeds) or not (fails) depending on some conditions. A failure
        // with the error TransitionNotTriggered tells that the conditions
        // weren't met (for example, the "userAsksConnect" transition is 
        // triggered only when the user asks for connection by clicking 
        // the "connect" button). Some transitions are triggered by previous
        // transition failures in a catchSome clause (such as the transition 
        // "connectionFailed").
        connection match {
            case c : Disconnected =>
                userAsksConnect(c)
                .catchSome { 
                    case TransitionNotTriggered() => nothing(c)
                }
            case c : InitiatingConnection => 
                connecting(c)
                .catchSome { 
                    case ConnectionFailed(msg, cause) => 
                        connectionFailed(c, msg, cause) 
                    case TransitionNotTriggered() => nothing(c)
                }
            case c : Connecting => 
                userAsksConnect(c)
                .orElse(connectionActualized(c))
                .catchSome { 
                    case ConnectionFailed(msg, cause) => 
                        connectionFailed(c, msg, cause) 
                    case TransitionNotTriggered() => nothing(c)
                    case ResponseNotReady() => nothing(c) 
                    case ResponseLost(msg) => connectionFailed(c, msg, new Throwable()) 
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
            curAskConnect <- Env.ui(_.getAskConnect())
            _ <- ZIO.unless(curAskConnect)(ZIO.fail(TransitionNotTriggered()))
            _ <- Env.ui(_.setAskConnect(false))
            _ <- Env.ui(_.setAlert(""))
            _ <- ZIO.effectTotal {
                println("Initiating Connection")
            }
        } yield InitiatingConnection()

    def userAsksConnect(connection: Connected): 
    ZIO[Env, TransitionFailure, State] =
        for {
            curAskConnect <- Env.ui(_.getAskConnect())
            _ <- ZIO.when(!curAskConnect)(ZIO.fail(TransitionNotTriggered()))
            _ <- Env.ui(_.setAskConnect(false))
            _ <- ZIO.effectTotal {
                println("Changing bootstrap.")
            }
        } yield ChangingBootstrap()

    def userAsksConnect(connection: Connecting): 
    ZIO[Env, TransitionFailure, State] =
        for {
            curAskConnect <- Env.ui(_.getAskConnect())
            _ <- ZIO.when(!curAskConnect)(ZIO.fail(TransitionNotTriggered()))
            _ <- Env.ui(_.setAskConnect(false))
            _ <- ZIO.effectTotal {
                println("Changing bootstrap while connecting.")
            }
        } yield ChangingBootstrap()

    def disconnecting(connection: Connected):
    ZIO[Env, TransitionFailure, State] =
        for {
            _ <- Env.kafka(_.disconnect())
            _ <- Env.ui(_.setRecords(Vector.empty))
            _ <- Env.ui(_.setTopics(Vector.empty))
            _ <- Env.ui(_.setIsConnected(false))
            _ <- Env.ui(_.setPartitions(Vector.empty, Vector.empty))
            _ <- ZIO.effectTotal {
                println("Disconnecting.")
            }
        } yield InitiatingConnection()

    def disconnecting(connection: ChangingBootstrap):
    ZIO[Env, TransitionFailure, State] =
        for {
            _ <- Env.kafka(_.disconnect())
            _ <- Env.ui(_.setRecords(Vector.empty))
            _ <- Env.ui(_.setTopics(Vector.empty))
            _ <- Env.ui(_.setIsConnected(false))
            _ <- Env.ui(_.setPartitions(Vector.empty, Vector.empty))
            _ <- ZIO.effectTotal {
                println("Disconnecting.")
            }
        } yield InitiatingConnection()

    def connecting(connection: InitiatingConnection): 
    ZIO[Env, TransitionFailure, State] =
        for {
            bootstrapAddress <- Env.ui(_.getBootstrapAddress())
            _ <- ZIO.unless(
                    bootstrapAddress != null && bootstrapAddress.nonEmpty
                )(
                    ZIO.fail(ConnectionFailed(
                        "Please enter a bootstrap address.", 
                        new Throwable()
                    ))
                )
            kafkaProperties <- Env.ui(_.getKafkaProperties())
            _ <- Env.kafka(_.connect(bootstrapAddress, kafkaProperties))
            _ <- Env.ui(_.setRecords(Vector.empty))
            _ <- Env.ui(_.setTopics(Vector.empty))
            _ <- Env.ui(_.setIsConnected(false))
            now <- ZIO.effectTotal {
                    (Calendar.getInstance.getTimeInMillis / 1000).toLong
                }
            _ <- ZIO.effectTotal {
                    println("Connecting to " ++ bootstrapAddress.toString())
                }
        } yield Connecting(bootstrapAddress, kafkaProperties, now)

    def connectionActualized(connection: Connecting): 
    ZIO[Env, TransitionFailure, State] =
        for {
            now <- ZIO.effectTotal {
                    (Calendar.getInstance.getTimeInMillis / 1000).toLong
                }
            elapsedSeconds <- ZIO.succeed(now - connection.timestamp)
            _ <- Env.ui { _.setInfo(
                    "Connecting to " ++ 
                    connection.bootstrapAddress.toString() ++
                    (if (elapsedSeconds < 5) { 
                        s" ($elapsedSeconds s)"
                    } else {
                        s" ($elapsedSeconds s… is the server up?)"
                    })
                )}
            isConnected <- Env.kafka(_.isConnected())
            _ <- ZIO.unless(isConnected)(ZIO.fail(TransitionNotTriggered()))
            topicNames <- Env.kafka(_.listTopics())
            _ <- Env.ui(_.setIsConnected(isConnected))
            _ <- Env.ui(_.setTopics(topicNames))
            _ <- Env.ui(_.setInfo("Connected to  " ++ 
                connection.bootstrapAddress))
            _ <- ZIO.effectTotal {
                println("Is Connected, topics:" ++ topicNames.toString())
            }
        } yield Connected(connection.bootstrapAddress, 
            connection.kafkaProperties, topicNames, NoTopic())

    def updateTopic(connection: Connected): ZIO[Env, TransitionFailure, State] = 
        for {
            newTopic <- Topic.update(connection.topic)
        } yield connection.copy(topic = newTopic)

    def connectionFailed(connection: State, msg: String, cause: Throwable): 
    ZIO[Env, TransitionFailure, State] =
        for {
            _ <- Env.ui(_.setAlert(s"Connection failed. $msg"))
            _ <- ZIO.effectTotal {
                println("Connection failed.")
                cause.printStackTrace()
            }
        } yield Disconnected()

    def nothing(connection: State): ZIO[Env, TransitionFailure, State] = 
        ZIO.succeed(connection)
   
}

