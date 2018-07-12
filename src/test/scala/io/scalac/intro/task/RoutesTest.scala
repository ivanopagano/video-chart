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

package io.scalac.intro.task

import org.scalatest.{ Matchers, OptionValues, WordSpec }
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import spray.json._
import io.scalac.intro.task.model.{ CommandValidation, Outcome }

class RoutesTest
    extends WordSpec
    with Matchers
    with OptionValues
    with ScalatestRouteTest
    with marshalling.JsonProtocols {

  val sut = HttpServer.route

  "The server" when {

    "receiving a wrong method" should {
      "behave accordingly" in {

        Get() ~> Route.seal(sut) ~> check {
          status shouldBe StatusCodes.MethodNotAllowed
        }

        Put() ~> Route.seal(sut) ~> check {
          status shouldBe StatusCodes.MethodNotAllowed
        }

        Head() ~> Route.seal(sut) ~> check {
          status shouldBe StatusCodes.MethodNotAllowed
        }

        Options() ~> Route.seal(sut) ~> check {
          status shouldBe StatusCodes.MethodNotAllowed
        }

      }

    }

    "receiving a correct POST request" should {
      "respond to the /register endpoint" in {

        val json = JsObject(
          "userName" -> JsString("David"),
          "email"    -> JsString("david@gmail.com"),
          "age"      -> JsNumber(28),
          "gender"   -> JsNumber(1)
        )

        Post("/register", json) ~> sut ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[Outcome.Confirmed] shouldBe an[Outcome.Confirmed]
        }

      }
    }

    "respond to the /action endpoint" in {

      val json = JsObject(
        "userId"   -> JsNumber(9696345L),
        "videoId"  -> JsNumber(4324556L),
        "actionId" -> JsNumber(3)
      )

      Post("/action", json) ~> sut ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[Outcome.Confirmed] shouldBe an[Outcome.Confirmed]
      }
    }
  }

  "receiving a POST request with error in the input" should {
    "respond with Bad Request for missing object in the /register endpoint" in {
      Post("/register") ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "respond with Bad Request for missing object in the /action endpoint" in {
      Post("/action") ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "respond with Bad Request for any missing field in the /register endpoint" in {

      val nogender = JsObject(
        "userName" -> JsString("David"),
        "email"    -> JsString("david@gmail.com"),
        "age"      -> JsNumber(28)
      )

      Post("/register", nogender) ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }

      val noage = JsObject(
        "userName" -> JsString("David"),
        "email"    -> JsString("david@gmail.com"),
        "gender"   -> JsNumber(1)
      )

      Post("/register", noage) ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }

      val noemail = JsObject(
        "userName" -> JsString("David"),
        "age"      -> JsNumber(28),
        "gender"   -> JsNumber(1)
      )

      Post("/register", noemail) ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }

      val noname = JsObject(
        "email"  -> JsString("david@gmail.com"),
        "age"    -> JsNumber(28),
        "gender" -> JsNumber(1)
      )

      Post("/register", noname) ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "respond with Bad Request for any missing field in the /action endpoint" in {

      val noaction = JsObject(
        "userId"  -> JsNumber(9696345L),
        "videoId" -> JsNumber(4324556L)
      )

      Post("/action", noaction) ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }

      val novideo = JsObject(
        "userId"   -> JsNumber(9696345L),
        "actionId" -> JsNumber(3)
      )

      Post("/action", novideo) ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }

      val nouser = JsObject(
        "videoId"  -> JsNumber(4324556L),
        "actionId" -> JsNumber(3)
      )

      Post("/action", nouser) ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "respond with Bad Request for incorrect fields in the /register endpoint, accumulating errors" in {

      val wrongGender = JsObject(
        "userName" -> JsString(""),
        "email"    -> JsString("david@gmail"),
        "age"      -> JsNumber(2),
        "gender"   -> JsNumber(4)
      )

      val expectedErrors = Vector(
        CommandValidation.EmptyUserName.detail,
        CommandValidation.InvalidEmail.detail,
        CommandValidation.InvalidAge.detail,
        CommandValidation.InvalidGender.detail
      ).map(JsString(_))

      Post("/register", wrongGender) ~> Route.seal(sut) ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
        val fields = entityAs[JsObject].fields
        val errors = fields.get("errors").value
        errors match {
          case JsArray(messages) =>
            messages should contain theSameElementsAs expectedErrors
          case _ => fail("wrong type of json content in errors message")
        }
      }
    }
  }

}
