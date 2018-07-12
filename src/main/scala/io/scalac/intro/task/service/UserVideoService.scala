/*
 * Copyright (c) 2018 Ivano Pagano
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.scalac.intro.task.service

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import cats.data.Validated
import cats.syntax.validated._
import io.scalac.intro.task.model._
import io.scalac.intro.task.model.Command._
import scala.concurrent.duration._
import scala.concurrent.Future

class ActorVideoService(implicit system: ActorSystem) extends UserVideoService {
  import ActorVideoService._

  type RegistrationResponse = Validated[Outcome.RegistrationError, Outcome.Confirmed]
  type ActionResponse       = CommandValidation.Verified[Outcome.Confirmed]

  lazy val registry: ActorRef = system.actorOf(Props[UserRegistry], "user-registry")

  implicit val timeout = Timeout(1 second)

  override def register(cmd: RegisterUser): Future[RegistrationResponse] =
    (registry ? RegisterMessage(cmd)).mapTo[RegistrationResponse]

  override def act(action: Action): Future[ActionResponse] =
    (registry ? ActionMessage(action)).mapTo[ActionResponse]
}

object ActorVideoService {

  final case class RegisterMessage(reg: RegisterUser) extends Serializable
  final case class ActionMessage(act: Action)         extends Serializable
  final case class CurrentVideo(replyTo: ActorRef)    extends Serializable

}

class UserRegistry extends Actor {
  import ActorVideoService._
  import scala.collection.immutable.HashMap

  var sequence = 0L
  var registry = HashMap.empty[UserId, ActorRef]

  def newId(): UserId = {
    sequence += 1
    UserId(sequence)
  }

  override def receive = {
    case RegisterMessage(RegisterUser(_, email, _, _)) =>
      //lookup by name or spawn and register the new handler
      val handler =
        context.children
          .find(_.path.name == email)
          .getOrElse {
            val userId     = newId()
            val newHandler = context.actorOf(ActionHandler.props(userId), email)
            registry = registry + (userId -> newHandler)
            newHandler
          }

      handler ! CurrentVideo(replyTo = sender)

    case msg @ ActionMessage(Action(uid, vid, action)) =>
      registry.get(uid) match {
        case Some(handler) =>
          handler forward msg
        case None =>
          sender ! CommandValidation.NonExistingUserId.invalidNel
      }

  }

}

class ActionHandler(id: UserId) extends Actor with ActorLogging {
  import ActorVideoService._

  var currentVideo = 1L
  var nextVideo    = currentVideo + 1

  override def receive = {
    case CurrentVideo(replyTo) =>
      replyTo ! Outcome.Confirmed(id, VideoId(currentVideo)).valid[Outcome.RegistrationError]

    case ActionMessage(Action(_, video, _)) if video.id != currentVideo =>
      sender ! CommandValidation.InvalidAction.invalidNel

    case ActionMessage(Action(_, video, action)) =>
      sender ! Outcome.Confirmed(id, VideoId(nextVideo)).valid
      currentVideo = nextVideo
      nextVideo += 1
      log.info("User {} did {} video {}", id.id, action, video.id)
  }

}

object ActionHandler {

  def props(id: UserId) = Props(new ActionHandler(id))

}
