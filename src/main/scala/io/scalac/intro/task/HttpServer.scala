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
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import io.scalac.intro.task.model._
import io.scalac.intro.task.marshalling._
import scala.concurrent.duration._

object HttpServer extends App with JsonProtocols {

  implicit val system = ActorSystem("xite")
  implicit val mat    = ActorMaterializer()

  val route =
    post {
      path("register") {
        entity(as[Command.RegisterUser]) { usr =>
          complete(Outcome.Confirmed(UserId(1L), VideoId(1L)))
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
  val _ = scala.io.StdIn.readLine()

  //bring an execution context in scope
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val done =
  binding.flatMap(
    _.terminate(5 seconds)
  ) andThen {
    case _ =>
      system.terminate()
  }

}
