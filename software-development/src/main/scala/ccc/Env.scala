package ccc

import zio._

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
        ZIO.accessM((env:Env) => f(env.ui))

    def kafka[A](f: KafkaInterface => ZIO[Env, TransitionFailure, A]): ZIO[Env, TransitionFailure, A] =
        ZIO.accessM((env:Env) => f(env.kafkaInterface))
}
 
