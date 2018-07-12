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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import io.scalac.intro.task.marshalling.JsonProtocols
import io.scalac.intro.task.model._
import io.scalac.intro.task.model.CommandValidation.InvalidInput
import scala.concurrent.duration._
import spray.json._
import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }

object HttpServer extends App with JsonProtocols {

  implicit val system = ActorSystem("xite")
  implicit val mat    = ActorMaterializer()

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def collectErrors(list: NonEmptyList[InvalidInput]): JsObject =
    JsObject(
      "errors" -> JsArray(list.map(err => JsString(err.detail)).toList: _*)
    )

  def route =
    post {
      path("register") {
        entity(as[Command.RegisterUser]) { usr =>
          complete {
            CommandValidation.verifyUserRegistration(usr) match {
              case Valid(userData) =>
                Outcome.Confirmed(UserId(1L), VideoId(1L))
              case Invalid(reasons) =>
                BadRequest -> collectErrors(reasons)
            }

          }
        }
      } ~
      path("action") {
        entity(as[Command.Action]) { action =>
          complete(Outcome.Confirmed(UserId(1L), VideoId(1L)))
        }
      }
    }

  val binding = Http().bindAndHandle(route, "localhost", 8085)

  println("Server running, press enter to stop...")
  scala.io.StdIn.readLine()

  binding.flatMap(
    _.terminate(5 seconds)
  ) andThen {
    case _ =>
      system.terminate()
  }

}
