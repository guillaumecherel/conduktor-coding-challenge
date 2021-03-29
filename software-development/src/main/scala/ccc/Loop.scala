package ccc

import zio._

object Loop {

    //def step(state: State): ZIO[Env, Throwable, State] = {
    //    for {
    //        newState <- State.update(state)
    //        //_ <- updateUI(newState)
    //    } yield newState
    //}

    //def updateState(state: State) = State.update(state)

    //def updateEnv(state: State): ZIO[Env, Throwable, Unit] = {
    //    updateKafka(state) *> updateUI(state)
    //}

    // def updateKafka(state: State): ZIO[Env, Throwable, Unit] = {
    //     state match {
    //         case Connecting(bootstrapAddress, kafkaProperties, false) => 
    //             Env.kafka(_.connect(bootstrapAddress, kafkaProperties))

    //         case Connecting(bootstrapAddress, kafkaProperties, true) => 
    //             ZIO.succeed(())

    //         case Connected(bootstrapAddress, kafkaProperties, topicNames, 
    //             topic) => 
    //             (topic match {
    //                 case NoTopic() =>
    //                     ZIO.succeed(())

    //                 case OpeningTopic(topicName, false) =>
    //                     Env.kafka(_.openTopic(topicName))

    //                 case OpeningTopic(topicName, true) =>
    //                     ZIO.succeed(())

    //                 case OpenedTopic(topicName, buffer, isConsuming, 
    //                     partitions, partitionsChanged, selectedPartitions, 
    //                     selectedPartitionsChanged) =>
    //                     if (selectedPartitionsChanged) {
    //                         Env.kafka(_.assignPartitions(selectedPartitions))
    //                     } else {
    //                         ZIO.succeed(())
    //                     }

    //             })

    //         case Disconnecting(false) => 
    //             Env.kafka(_.disconnect())

    //         case Disconnecting(true) => 
    //             ZIO.succeed(())

    //         case Disconnected() => 
    //             ZIO.succeed(())
    //     }
    // }

    // def updateSelectedPartition(oldPartition: Vector[Int], newPartition: Vector[Int]):
    //     ZIO[Env, Throwable, Unit] =
    //     (newPartition == oldPartition) match {
    //         case true => ZIO.succeed(())
    //         case false => Env.kafka(_.assignPartitions(newPartition))
    //     }

    //def updateUI(state: State): ZIO[Env, Throwable, Unit] =
    //    state match {
    //        case Connecting(bootstrapAddress, kafkaProperties, false) => 
    //            Env.ui(_.setRecords(Vector.empty)) *>
    //            Env.ui(_.setTopics(Vector.empty)) *>
    //            Env.ui(_.setIsConnected(false))

    //        case Connecting(bootstrapAddress, kafkaProperties, true) => 
    //            ZIO.succeed(())

    //        case Connected(bootstrapAddress, kafkaProperties, topicNames, 
    //            topic) => 
    //            Env.ui(_.setAskConnect(false)) *>
    //            Env.ui(_.setTopics(topicNames)) *>
    //            Env.ui(_.setIsConnected(true)) *>
    //            (topic match {
    //                case NoTopic() =>
    //                    Env.ui(_.setRecords(Vector.empty))

    //                case OpeningTopic(topicName, false) =>
    //                    Env.ui(_.setRecords(Vector.empty))

    //                case OpeningTopic(topicName, true) =>
    //                    ZIO.succeed(())

    //                case OpenedTopic(topicName, buffer, isConsuming, partitions, 
    //                    partitionsChanged, selectedPartitions, 
    //                    selectedPartitionsChanged) =>
    //                    Env.ui(_.setRecords(buffer)) *>
    //                    (if (partitionsChanged) {
    //                        Env.ui(_.setPartitions(partitions, selectedPartitions))
    //                    } else if (selectedPartitionsChanged) {
    //                        Env.ui(_.setSelectedPartitions(selectedPartitions))
    //                    } else {
    //                        ZIO.succeed(())
    //                    })
    //            })

    //        case Disconnecting(false) => 
    //            Env.ui(_.setIsConnected(false)) *>
    //            Env.ui(_.setRecords(Vector.empty)) *>
    //            Env.ui(_.setTopics(Vector.empty))

    //        case Disconnecting(true) => 
    //            ZIO.succeed(())

    //        case Disconnected() => 
    //            ZIO.succeed(())

    //    }
} 
