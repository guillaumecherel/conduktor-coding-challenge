package ccc.state

import zio._

import scala.collection.immutable._

import ccc.Env
import ccc.errors._

sealed trait Topic

final case class NoTopic() extends Topic

final case class InitiatingOpeningTopic() extends Topic

final case class OpeningTopic(topicName: String) extends Topic

final case class OpenedTopic(
    topicName: String, 
    buffer: Vector[String], 
    isConsuming: Boolean,
    partitions: Vector[Int], 
    selectedPartitions: Vector[Int]
) extends Topic

final case class ChangingTopic() extends Topic

object Topic {

    def update(topic: Topic): ZIO[Env, TransitionFailure, Topic] = 
        // See explanation in function State.update
        topic match {
            case t : NoTopic =>
                userSelectingTopic(t)
                .catchSome { 
                    case TransitionNotTriggered() => nothing(t)
                }
            case t : InitiatingOpeningTopic =>
                openingTopic(t)
                .catchSome { 
                    case TransitionNotTriggered() => nothing(t)
                }
            case t : OpeningTopic =>
                userSelectingTopic(t)
                .orElse(actualizingTopic(t))
                .catchSome { 
                    case TransitionNotTriggered() => nothing(t)
                }
            case t : OpenedTopic =>
                userSelectingTopic(t)
                .orElse(updatingIsConsuming(t))
                .orElse(updatingPartitions(t))
                .orElse(updatingSelectedPartitions(t))
                .orElse(polling(t))
                .catchSome { 
                    case TransitionNotTriggered() => nothing(t)
                }
            case t : ChangingTopic =>
                closingTopic(t)
                .catchSome { 
                    case TransitionNotTriggered() => nothing(t)
                }
        }

    def userSelectingTopic(topic: NoTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            selectedTopic <- Env.ui(_.getSelectedTopic())
            _ <- ZIO.unless(selectedTopic.nonEmpty) {
                    ZIO.fail(TransitionNotTriggered())
                }
            _ <- ZIO.effectTotal {
                    println("Selecting new topic.")
                }
        } yield InitiatingOpeningTopic()

    def userSelectingTopic(topic: OpenedTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            selectedTopic <- Env.ui(_.getSelectedTopic())
            _ <- selectedTopic match {
                    case None => ZIO.fail(TransitionNotTriggered())
                    case Some(selectedTopicName) =>
                        ZIO.unless(selectedTopicName != topic.topicName)(
                            ZIO.fail(TransitionNotTriggered())
                        )
                }
            _ <- ZIO.effectTotal {
                    println("Changing topic.")
                }
        } yield ChangingTopic()

    def userSelectingTopic(topic: OpeningTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            selectedTopic <- Env.ui(_.getSelectedTopic())
            _ <- selectedTopic match {
                    case None => ZIO.fail(TransitionNotTriggered())
                    case Some(selectedTopicName) =>
                        ZIO.unless(selectedTopicName != topic.topicName)(
                            ZIO.fail(TransitionNotTriggered())
                        )
                }
            _ <- ZIO.effectTotal {
                println("Changing topic while opening one.")
            }
        } yield ChangingTopic()

    def openingTopic(topic: InitiatingOpeningTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            selectedTopic <- Env.ui(_.getSelectedTopic())
            newTopicName <- selectedTopic match  {
                case Some(topicName) => 
                    Env.kafka(_.openTopic(topicName)) *>
                    ZIO.succeed(topicName)
                case None => ZIO.fail(TransitionNotTriggered())
            }
            _ <- ZIO.effectTotal {
                println("Topic selected: " ++ newTopicName)
            }
        } yield OpeningTopic(newTopicName)

    def closingTopic(topic: ChangingTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            _ <- Env.kafka(_.closeTopic())
            _ <- Env.ui(_.setRecords(Vector.empty))
            _ <- ZIO.effectTotal {
                println("Closing topic.")
            }
        } yield InitiatingOpeningTopic()

    def actualizingTopic(topic: OpeningTopic): ZIO[Env, TransitionFailure, Topic] = 
        for {
            hasTopicOpened <- Env.kafka(_.hasTopicOpened())
            _ <- ZIO.unless(hasTopicOpened)(ZIO.fail(TransitionNotTriggered()))
            partitions <- Env.kafka(_.listPartitions())
            _ <- Env.ui(_.setPartitions(partitions, partitions))
            _ <- Env.ui(_.setSelectedPartitions(partitions))
            _ <- Env.kafka(_.assignPartitions(partitions))
            _ <- Env.kafka(_.seekToBeginning(partitions))
            _ <- ZIO.effectTotal {
                println("The topic " ++ topic.topicName ++ " is open.")
            }
        } yield OpenedTopic(
                topicName = topic.topicName, 
                buffer = Vector.empty, 
                isConsuming = true,
                partitions = partitions,
                selectedPartitions = partitions)

    def updatingIsConsuming(topic: OpenedTopic):
        ZIO[Env, TransitionFailure, Topic] =
        for {
            isConsuming <- Env.ui(_.getIsConsuming())
            _ <- ZIO.unless(isConsuming != topic.isConsuming)(
                    ZIO.fail(TransitionNotTriggered())
                )
            _ <- ZIO.effectTotal {
                    println(s"Updating setting isConsuming $isConsuming")
                }
        } yield topic.copy(isConsuming = isConsuming)

    def updatingPartitions(topic: OpenedTopic):
        ZIO[Env, TransitionFailure, Topic] =
        for {
            partitions <- Env.kafka(_.listPartitions())
            selectedPartitions <- Env.ui(_.getSelectedPartitions())
            _ <- ZIO.unless(partitions != topic.partitions)(
                    ZIO.fail(TransitionNotTriggered())
                )
            _ <- Env.ui(_.setPartitions(partitions, selectedPartitions))
            _ <- ZIO.effectTotal {
                    println(s"Updating partitions: partitions $partitions")
                }
        } yield topic.copy(partitions = partitions)

    def updatingSelectedPartitions(topic: OpenedTopic):
        ZIO[Env, TransitionFailure, Topic] =
        for {
            selectedPartitions <- Env.ui(_.getSelectedPartitions())
            _ <- ZIO.unless(selectedPartitions != topic.selectedPartitions)(
                ZIO.fail(TransitionNotTriggered()))
            _ <- Env.ui(_.setRecords(Vector.empty))
            _ <- Env.kafka(_.assignPartitions(selectedPartitions))
            _ <- Env.kafka(_.seekToBeginning(selectedPartitions))
            _ <- ZIO.effectTotal {
                    println(s"Updating selected partitions: selectedPartitions $selectedPartitions")
                }
        } yield topic.copy(selectedPartitions = selectedPartitions)

    def polling(topic: OpenedTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            _ <- ZIO.unless(topic.isConsuming)(
                    ZIO.fail(TransitionNotTriggered())
                )
            newRecords <- Env.kafka(_.poll())
            _ <- Env.ui(_.appendRecords(newRecords))
        } yield topic.copy(buffer = topic.buffer.concat(newRecords))

    def nothing(topic: Topic): ZIO[Env, TransitionFailure, Topic] =
        ZIO.succeed(topic)
}
