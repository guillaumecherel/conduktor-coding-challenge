package ccc

import zio._

import scala.collection.immutable._

trait Event
// final case class UserAsksConnect(
//     bootstrapAddress: String,
//     kafkaProperties: Vector[(String, String)]) extends Event
// 
// object Event {
// 
//     def now(): ZIO[Env, Throwable, Vector[Event]] = 
//         ZIO.collectAll(Vector( usersAsksConnect() )) >>=
//             { _.collect{ case Some(x) => x }}
//             
//     def userAsksConnect(): ZIO[Env, Throwable, Option[Event]] =
//         for {
//             curAskConnect <- Env.ui(_.getAskConnect())
//             bootstrapAddress <- Env.ui(_.getBootstrapAddress())
//             kafkaProperties <- Env.ui(_.getKafkaProperties())
//         } yield UserAsksConnect(bootstrapAddress, kafkaProperties)
// }
