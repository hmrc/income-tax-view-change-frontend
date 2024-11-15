/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import helpers.ComponentSpecBase

class WelshMessagesSpec extends ComponentSpecBase {

  val realMessages: Map[String, Map[String, String]] = messagesAPI.messages

  val englishMessages: Map[String, String] = realMessages("default")
  val welshMessages: Map[String, String] = realMessages("cy")

  "all English messages should have a Welsh translation" in {
    val missingWelshKeys = englishMessages.keySet.diff(welshMessages.keySet)

    if (missingWelshKeys.nonEmpty) {
      val failureText = missingWelshKeys.foldLeft(s"There are ${missingWelshKeys.size} missing Welsh translations:") {
        case (failureString, key) =>
          failureString + s"\n$key:${englishMessages(key)}"
      }
      fail(failureText)
    }
  }

  "all Welsh messages should have an English translation" in {
    val missingEnglishKeys = welshMessages.keySet.diff(englishMessages.keySet)

    if (missingEnglishKeys.nonEmpty) {
      val failureText = missingEnglishKeys.foldLeft(s"There are ${missingEnglishKeys.size} missing English translations:") {
        case (failureString, key) =>
          failureString + s"\n$key:${welshMessages(key)}"
      }
      fail(failureText)
    }
  }
}

