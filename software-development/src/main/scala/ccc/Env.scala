package ccc

import zio._

import ccc.ui.UIInterface
import ccc.kafka.KafkaInterface

import ccc.errors._

/**
  * The application environment.
  *
  * @param ui Interface to the user interface 
  * @param kafkaInterface Interface to the kafka service
  */
final case class Env(
    ui: UIInterface, 
    kafkaInterface: KafkaInterface)

object Env {

    def ui[A](f: UIInterface => ZIO[Env, TransitionFailure, A]): ZIO[Env, TransitionFailure, A] =
        ZIO.accessM[Env](env => f(env.ui))

    def kafka[A](f: KafkaInterface => ZIO[Env, TransitionFailure, A]): ZIO[Env, TransitionFailure, A] =
        ZIO.accessM[Env](env => f(env.kafkaInterface))

    def close(): ZIO[Env, Nothing, Unit] = 
        ZIO.accessM[Env](env => env.kafkaInterface.close())
}
 
