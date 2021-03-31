package ccc

import zio._

trait TransitionFailure

final case class TransitionNotTriggered() extends TransitionFailure
final case class ConnectionFailed(msg: String) extends TransitionFailure
final case class ResponseNotReady() extends TransitionFailure
final case class ResponseLost(msg: String) extends TransitionFailure
final case class OtherError(cause: Throwable) extends TransitionFailure

