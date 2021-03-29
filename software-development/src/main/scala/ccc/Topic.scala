package ccc

import zio._

import scala.collection.immutable._

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
                .orElse(updatingSettings(t))
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
            selectedTopic <- Env.ui(_.getSelectedTopic()).orDie
            _ <- ZIO.unless(selectedTopic.nonEmpty)(ZIO.fail(TransitionNotTriggered()))
        } yield {
            println("Selecting new topic.")
            InitiatingOpeningTopic()
        }

    def userSelectingTopic(topic: OpenedTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            selectedTopic <- Env.ui(_.getSelectedTopic()).orDie
            _ <- selectedTopic match {
                case None => ZIO.fail(TransitionNotTriggered())
                case Some(selectedTopicName) =>
                    ZIO.unless(selectedTopicName != topic.topicName)(
                    ZIO.fail(TransitionNotTriggered()))
            }
        } yield {
            println("Changing topic.")
            ChangingTopic()
        }

    def userSelectingTopic(topic: OpeningTopic): ZIO[Env, TransitionFailure, Topic] =
            for {
                selectedTopic <- Env.ui(_.getSelectedTopic()).orDie
                _ <- selectedTopic match {
                    case None => ZIO.fail(TransitionNotTriggered())
                    case Some(selectedTopicName) =>
                        ZIO.unless(selectedTopicName != topic.topicName)(
                        ZIO.fail(TransitionNotTriggered()))
                }
            } yield {
                println("Changing topic while opening one.")
                ChangingTopic()
            }

    def openingTopic(topic: InitiatingOpeningTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            selectedTopic <- Env.ui(_.getSelectedTopic()).orDie
            newTopicName <- selectedTopic match  {
                case Some(topicName) => 
                    Env.kafka(_.openTopic(topicName)).orDie *>
                    ZIO.succeed(topicName)
                case None => ZIO.fail(TransitionNotTriggered())
            }
        } yield {
            println("Topic selected: " ++ newTopicName)
            OpeningTopic(newTopicName)
        }

    def closingTopic(topic: ChangingTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            _ <- Env.kafka(_.closeTopic()).orDie
            _ <- Env.ui(_.setRecords(Vector.empty)).orDie
        } yield {
            println("Closing topic.")
            InitiatingOpeningTopic()
        }

    def actualizingTopic(topic: OpeningTopic): ZIO[Env, TransitionFailure, Topic] = 
        for {
            hasTopicOpened <- Env.kafka(_.hasTopicOpened()).orDie
            _ <- ZIO.unless(hasTopicOpened)(ZIO.fail(TransitionNotTriggered()))
            partitions <- Env.kafka(_.listPartitions()).orDie
            _ <- Env.ui(_.setPartitions(partitions, partitions)).orDie
            _ <- Env.kafka(_.assignPartitions(partitions)).orDie
        } yield {
                println("The topic " ++ topic.topicName ++ " is open.")
                OpenedTopic(
                    topicName = topic.topicName, 
                    buffer = Vector.empty, 
                    isConsuming = true,
                    partitions = partitions,
                    selectedPartitions = partitions
                )
        }

    def updatingSettings(topic: OpenedTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            isConsuming <- Env.ui(_.getIsConsuming()).orDie
            selectedPartitions <- Env.ui(_.getSelectedPartitions()).orDie
            _ <- (selectedPartitions != topic.selectedPartitions) match {
                case true => Env.kafka(_.assignPartitions(selectedPartitions)).orDie
                case false => ZIO.succeed(())
            }
            partitions <- Env.kafka(_.listPartitions()).orDie
            _ <- (partitions != topic.partitions) match {
                case true => Env.ui(_.setPartitions(partitions, selectedPartitions)).orDie
                case false => ZIO.succeed(())
            }
            _ <- ZIO.unless(isConsuming != topic.isConsuming ||
                partitions != topic.partitions ||
                selectedPartitions != topic.selectedPartitions)(
                ZIO.fail(TransitionNotTriggered()))
        } yield {
            println(s"Updating settings: isConsuming $isConsuming, partitions $partitions, selectedPartitions $selectedPartitions")
            topic.copy(
                isConsuming = isConsuming,
                partitions = partitions,
                selectedPartitions = selectedPartitions
            )
        }

    def polling(topic: OpenedTopic): ZIO[Env, TransitionFailure, Topic] =
        for {
            _ <- ZIO.unless(topic.isConsuming)(ZIO.fail(TransitionNotTriggered()))
            newRecords <- Env.kafka(_.poll()).orDie
            _ <- Env.ui(_.appendRecords(newRecords)).orDie
        } yield {
            topic.copy(buffer = topic.buffer.concat(newRecords))
        }

    def nothing(topic: Topic): ZIO[Env, TransitionFailure, Topic] =
        ZIO.succeed(topic)
}
