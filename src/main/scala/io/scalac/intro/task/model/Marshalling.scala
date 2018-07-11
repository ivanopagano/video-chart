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

package io.scalac.intro.task.marshalling

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import io.scalac.intro.task.model._

trait JsonProtocols extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object GenderJsonFormat extends NumberEnumerationJsonProtocol[Gender] {
    override lazy val toModel = {
      case 1 => Male
      case 2 => Female
      case _ => Unknown
    }
    override lazy val fromModel = {
      case Male    => 1
      case Female  => 2
      case Unknown => serializationError("gender is not specified")
    }
  }

  implicit object ActionIdJsonFormat extends NumberEnumerationJsonProtocol[ActionId] {
    override lazy val toModel = {
      case 1 => Like
      case 2 => Skip
      case 3 => Play
    }
    override lazy val fromModel = {
      case Like => 1
      case Skip => 2
      case Play => 3
    }
  }

  implicit object UserIdJsonFormat extends JsonFormat[UserId] {
    override def read(json: JsValue) = json match {
      case JsNumber(id) if id.isValidLong =>
        UserId(id.toLong)
      case json =>
        deserializationError("Invalid json value for userId $json")
    }
    override def write(userId: UserId) = JsNumber(userId.id)
  }

  implicit object VideoIdJsonFormat extends JsonFormat[VideoId] {
    override def read(json: JsValue) = json match {
      case JsNumber(id) if id.isValidLong =>
        VideoId(id.toLong)
      case json =>
        deserializationError("Invalid json value for videoId $json")
    }
    override def write(videoId: VideoId) = JsNumber(videoId.id)
  }

  implicit val userJsonFormat    = jsonFormat4(Command.RegisterUser)
  implicit val actionJsonFormat  = jsonFormat3(Command.Action)
  implicit val outcomeJsonFormat = jsonFormat2(Outcome.Confirmed)
}

/** handle common conversion of an enumerated type to an integer-based value as json */
trait NumberEnumerationJsonProtocol[T] extends JsonFormat[T] {
  import JsonWriter._

  def toModel: PartialFunction[Int, T]
  def fromModel: T => Int

  private val reading: JsonReader[T] = {
    case JsNumber(decimal) if decimal.isValidInt && toModel.isDefinedAt(decimal.toInt) =>
      toModel(decimal.toInt)
    case json =>
      deserializationError(
        s"Invalid value for enumerated field, a json number within a restricted range was expected, instead of $json"
      )
  }

  private val writing: JsonWriter[T] = fromModel andThen JsNumber.apply

  override def read(json: JsValue) = reading.read(json)
  override def write(model: T)     = writing.write(model)
}
