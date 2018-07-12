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
import akka.pattern.ask
import akka.util.Timeout
import akka.testkit.{ TestKit }
import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import io.scalac.intro.task.service._
import io.scalac.intro.task.model._
import io.scalac.intro.task.service.ActorVideoService.RegisterMessage
import scala.concurrent.duration._

class ActorVideoServiceTest
    extends TestKit(ActorSystem("VideoServiceTest"))
    with WordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A UserRegistry actor " should {

    "register a new user" in withRegistryActor { registry =>
      implicit val timeout = Timeout(100 millis)

      val cmd    = Command.RegisterUser("name", "email@host.org", 18, Male)
      val answer = registry ? RegisterMessage(cmd)

      val response = answer.futureValue

      response match {
        case Valid(Outcome.Confirmed(UserId(uid), VideoId(vid))) =>
        case Invalid(error) =>
          fail(s"unexpectd registration error: $error")
      }

    }

    "return the response when a user register with an existing email (idempotent calls)" in withRegistryActor {
      registry =>
        implicit val timeout = Timeout(100 millis)

        val cmd    = Command.RegisterUser("name", "email@host.org", 18, Male)
        val answer = registry ? RegisterMessage(cmd)

        val response = answer.futureValue

        response shouldBe an[Valid[Outcome.Confirmed]]

        (registry ? RegisterMessage(cmd)).futureValue shouldEqual response

    }

  }

  private def withRegistryActor(testBody: ActorRef => Any): Any = {
    val actor = system.actorOf(Props[UserRegistry])

    try {
      testBody(actor)
    } finally {
      system stop actor
    }
  }

}
