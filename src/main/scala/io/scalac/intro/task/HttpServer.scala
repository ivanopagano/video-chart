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
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import io.scalac.intro.task.marshalling.JsonProtocols
import io.scalac.intro.task.model._
import io.scalac.intro.task.service.{ ActorVideoService, Playlist, UserVideoService }
import cats.data.Validated.{ Invalid, Valid }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object HttpServer extends App with Playlist {

  implicit val system = ActorSystem("xite")
  implicit val mat    = ActorMaterializer()

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = new ActorVideoService

  val binding = Http().bindAndHandle(Routing.route(service), "localhost", 8085)

  println("Server running, press enter to stop...")
  scala.io.StdIn.readLine()

  binding.flatMap(
    _.terminate(5 seconds)
  ) andThen {
    case _ =>
      system.terminate()
  }

}

object Routing extends JsonProtocols {

  def route(videoService: UserVideoService)(implicit ec: ExecutionContext): Route =
    post {
      path("register") {
        entity(as[Command.RegisterUser]) { usr =>
          complete {
            CommandValidation.verifyUserRegistration(usr) match {
              case Valid(userData) =>
                videoService.register(userData) map { res =>
                  res.leftMap(err => InternalServerError -> registrationFailure(err)).toEither
                }
              case Invalid(reasons) =>
                BadRequest -> collectErrors(reasons)
            }
          }
        }
      } ~
      path("action") {
        entity(as[Command.Action]) { action =>
          complete {
            videoService.act(action) map { res =>
              res.leftMap(errs => BadRequest -> collectErrors(errs)).toEither
            }
          }
        }
      }
    }

}
