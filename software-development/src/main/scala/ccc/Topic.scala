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

    def update(topic: Topic): ZIO[Env, Throwable, Topic] = 
        topic match {
            case _ : NoTopic =>
                userSelectingTopic(topic)
                .orElse(nothing(topic))
            case _ : InitiatingOpeningTopic =>
                openingTopic(topic)
                .orElse(nothing(topic))
            case _ : OpeningTopic =>
                userSelectingTopic(topic)
                .orElseOptional(actualizingTopic(topic))
                .orElse(nothing(topic))
            case _ : OpenedTopic =>
                userSelectingTopic(topic)
                .orElseOptional(updatingSettings(topic))
                .orElseOptional(polling(topic))
                .orElse(nothing(topic))
            case _ : ChangingTopic =>
                closingTopic(topic)
                .orElse(nothing(topic))
        }

    def userSelectingTopic(topic: Topic): ZIO[Env, Option[Throwable], Topic] =
        (topic match {
            case _: NoTopic => 
                for {
                    selectedTopic <- Env.ui(_.getSelectedTopic())
                } yield selectedTopic.map { _ =>
                    println("Selecting new topic.")
                    InitiatingOpeningTopic()
                }
            case t: OpenedTopic =>
                for {
                    selectedTopic <- Env.ui(_.getSelectedTopic())
                } yield selectedTopic.flatMap { selectedTopicName =>
                    if (selectedTopicName == t.topicName) {
                        None
                    } else {
                        println("Changing topic.")
                        Some(ChangingTopic())
                    }
                }
            case t: OpeningTopic =>
                for {
                    selectedTopic <- Env.ui(_.getSelectedTopic())
                } yield selectedTopic.flatMap { selectedTopicName =>
                    if (selectedTopicName == t.topicName) {
                        None
                    } else {
                        println("Changing topic while opening one.")
                        Some(ChangingTopic())
                    }
                }
            case other => ZIO.succeed(None)
        }).some

    def openingTopic(topic: Topic): ZIO[Env, Option[Throwable], Topic] =
        (topic match {
            case _ : InitiatingOpeningTopic => 
                for {
                    selectedTopic <- Env.ui(_.getSelectedTopic())
                    _ <- selectedTopic match  {
                        case Some(topicName) => Env.kafka(_.openTopic(topicName))
                        case None => ZIO.succeed(())
                    }
                } yield selectedTopic.map { newTopicName =>
                    println("Topic selected: " ++ newTopicName)
                    OpeningTopic(newTopicName)
                }
            case other => ZIO.succeed(None)
        }).some

    def closingTopic(topic: Topic): ZIO[Env, Option[Throwable], Topic] =
        (topic match {
            case _: ChangingTopic =>
                for {
                    _ <- Env.kafka(_.closeTopic())
                    _ <- Env.ui(_.setRecords(Vector.empty))
                } yield Some {
                    println("Closing topic.")
                    InitiatingOpeningTopic()
                }
            case other => ZIO.succeed(None)
        }).some

    def actualizingTopic(topic: Topic): ZIO[Env, Option[Throwable], Topic] = 
        (topic match {
            case OpeningTopic(topicName) =>
                for {
                    hasTopicOpened <- Env.kafka(_.hasTopicOpened())
                    partitions <- Env.kafka(_.listPartitions())
                    _ <- Env.ui(_.setPartitions(partitions, partitions))
                    _ <- Env.kafka(_.assignPartitions(partitions))
                } yield Option.when(hasTopicOpened) {
                        println("The topic " ++ topicName ++ " is open.")
                        OpenedTopic(
                            topicName = topicName, 
                            buffer = Vector.empty, 
                            isConsuming = true,
                            partitions = partitions,
                            selectedPartitions = partitions
                        )
                }
            case other => ZIO.succeed(None)
        }).some

    def updatingSettings(topic: Topic): ZIO[Env, Option[Throwable], Topic] =
        (topic match {
            case t : OpenedTopic =>
                for {
                    isConsuming <- Env.ui(_.getIsConsuming())
                    selectedPartitions <- Env.ui(_.getSelectedPartitions())
                    _ <- (selectedPartitions != t.selectedPartitions) match {
                        case true => Env.kafka(_.assignPartitions(selectedPartitions))
                        case false => ZIO.succeed(())
                    }
                    partitions <- Env.kafka(_.listPartitions())
                    _ <- (partitions != t.partitions) match {
                        case true => Env.ui(_.setPartitions(partitions, selectedPartitions))
                        case false => ZIO.succeed(())
                    }
                } yield Option.when(
                    isConsuming != t.isConsuming ||
                    partitions != t.partitions ||
                    selectedPartitions != t.selectedPartitions) {
                    println(s"Updating settings: isConsuming $isConsuming, partitions $partitions, selectedPartitions $selectedPartitions")
                    t.copy(
                        isConsuming = isConsuming,
                        partitions = partitions,
                        selectedPartitions = selectedPartitions
                    )
                }
            case other => ZIO.succeed(None)
        }).some

    def polling(topic: Topic): ZIO[Env, Option[Throwable], Topic] =
        (topic match {
            case t @ OpenedTopic(_, _, true, _, _) =>
                for {
                    newRecords <- Env.kafka(_.poll())
                    _ <- Env.ui(_.appendRecords(newRecords))
                } yield Some {
                    println("Receiving new records: " ++ newRecords.toString())
                    t.copy(buffer = t.buffer.concat(newRecords))
                }
            case other => ZIO.succeed(None)
        }).some


    def nothing(topic: Topic): ZIO[Env, Throwable, Topic] =
        ZIO.succeed(topic)
}
