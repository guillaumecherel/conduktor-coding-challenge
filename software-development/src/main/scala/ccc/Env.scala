package ccc

import zio._

final case class Env(
    ui: UIInterface, 
    kafkaInterface: KafkaInterface)

object Env {

    def ui[A](f: UIInterface => ZIO[Env, Throwable, A]): ZIO[Env, Throwable, A] =
        ZIO.accessM((env:Env) => f(env.ui))

    def kafka[A](f: KafkaInterface => ZIO[Env, Throwable, A]): ZIO[Env, Throwable, A] =
        ZIO.accessM((env:Env) => f(env.kafkaInterface))
}
 
