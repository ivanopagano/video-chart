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

package io.scalac.intro.task.model

import cats.data._
import cats.implicits._

object CommandValidation {

  //the list of possible validation failures

  sealed trait InvalidInput {
    def detail: String
  }
  final case object EmptyUserName extends InvalidInput {
    val detail = "username must be non-empty"
  }
  final case object InvalidEmail extends InvalidInput {
    val detail = "email is not valid"
  }
  final case object InvalidGender extends InvalidInput {
    val detail = "gender is not valid"
  }
  final case object InvalidAge extends InvalidInput {
    val detail = "age is not valid"
  }
  final case object InvalidAction extends InvalidInput {
    val detail = "action is not valid"
  }
  final case object NonExistingUserId extends InvalidInput {
    val detail = "userId does not exists"
  }
  final case object NonLastVideo extends InvalidInput {
    val detail = "video does not correspond to the last given"
  }

  type Verified[A] = ValidatedNel[InvalidInput, A]

  //verification methods

  //we're simplifying a bit, assuming that no null input is sent to verification

  private def in(low: Int, high: Int) = (input: Int) => input >= low && input <= high

  private val ageRange    = in(5, 120)
  private val genderRange = in(1, 2)
  private val actionRange = in(1, 3)

  protected def verifyUserName(name: String): Verified[String] =
    if (name.trim.nonEmpty) name.trim.validNel else EmptyUserName.invalidNel

  //non RFC-compliant (!)
  protected def verifyEmail(email: String): Verified[String] =
    if (email.trim.toUpperCase.matches("""^[A-Z0-9\._%\+\-]+@[A-Z0-9\.\-]+\.[A-Z]{2,6}$"""))
      email.validNel
    else
      InvalidEmail.invalidNel

  protected def verifyAge(age: Int): Verified[Int] =
    if (ageRange(age)) age.validNel else InvalidAge.invalidNel

  protected def verifyGender(gender: Gender): Verified[Gender] = gender match {
    case Unknown => InvalidGender.invalidNel
    case _       => gender.validNel
  }

  def verifyUserRegistration(userName: String,
                             email: String,
                             age: Int,
                             gender: Gender): Verified[Command.RegisterUser] =
    (verifyUserName(userName), verifyEmail(email), verifyAge(age), verifyGender(gender))
      .mapN(Command.RegisterUser)

  def verifyUserRegistration(userCommand: Command.RegisterUser): Verified[Command.RegisterUser] =
    verifyUserRegistration(userCommand.userName,
                           userCommand.email,
                           userCommand.age,
                           userCommand.gender)

}
