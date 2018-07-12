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

import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import org.scalatest.concurrent.ScalaFutures
import akka.actor._
import akka.testkit.{ ImplicitSender, TestKit }
import cats.data.Validated.Valid
import cats.syntax.validated._
import io.scalac.intro.task.model._
import io.scalac.intro.task.service.ActorVideoService._

class ActorVideoServiceTest
    extends TestKit(ActorSystem("VideoServiceTest"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A UserRegistry actor" should {

    "register a new user" in withRegistryActor { registry =>
      val cmd = Command.RegisterUser("name", "email@host.org", 18, Male)
      registry ! RegisterMessage(cmd)

      expectMsgClass(classOf[Valid[Outcome.Confirmed]])

    }

    "return the same response when a user registers again an existing email (idempotent calls)" in withRegistryActor {
      registry =>
        val cmd = Command.RegisterUser("name", "email@host.org", 18, Male)
        registry ! RegisterMessage(cmd)

        val first = expectMsgClass(classOf[Valid[Outcome.Confirmed]])

        registry ! RegisterMessage(cmd)

        val second = expectMsgClass(classOf[Valid[Outcome.Confirmed]])

        second shouldEqual first

    }

    "reject an action for non-matching user id" in withRegistryActor { registry =>
      val action   = Command.Action(UserId(1), VideoId(1), Like)
      val expected = CommandValidation.NonExistingUserId.invalidNel

      registry ! ActionMessage(action)

      expectMsg(expected)
    }

  }

  "An ActionHandler actor" should {

    "respond to a current video request with the correct user id" in withActionHandlerActor(
      UserId(1L)
    ) { handler =>
      handler ! CurrentVideo(testActor)

      val confirm = expectMsgClass(classOf[Valid[Outcome.Confirmed]])

      confirm.a.userId shouldEqual UserId(1L)
    }

    "respond to consecutive video requests with the same message" in withActionHandlerActor(
      UserId(1L)
    ) { handler =>
      handler ! CurrentVideo(testActor)

      val confirm = expectMsgClass(classOf[Valid[Outcome.Confirmed]])

      handler ! CurrentVideo(testActor)

      expectMsg(confirm)
    }

    "reject video actions where the video id doesn't match the current video" in withActionHandlerActor(
      UserId(1L)
    ) { handler =>
      handler ! CurrentVideo(testActor)

      val currentVideo = expectMsgClass(classOf[Valid[Outcome.Confirmed]]).a.videoId

      val action   = Command.Action(UserId(1), VideoId(currentVideo.id + 1), Like)
      val expected = CommandValidation.InvalidAction.invalidNel
      handler ! ActionMessage(action)

      expectMsg(expected)
    }

  }

  private def withRegistryActor(testBody: ActorRef => Any): Any =
    withActor(Props[UserRegistry], testBody)

  private def withActionHandlerActor(id: UserId)(testBody: ActorRef => Any): Any =
    withActor(ActionHandler.props(id), testBody)

  private def withActor(props: Props, testBody: ActorRef => Any): Any = {
    val actor = system.actorOf(props)
    try {
      testBody(actor)
    } finally {
      system stop actor
    }
  }

}
