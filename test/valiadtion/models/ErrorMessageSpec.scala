/*
 * Copyright 2025 HM Revenue & Customs
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

package valiadtion.models

import forms.validation.models.ErrorMessage
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import play.api.i18n.Messages

class ErrorMessageSpec extends AnyWordSpec with Matchers with MockitoSugar{

  case class TestErrorMessage(key: String, args: Seq[String] = Seq.empty) extends ErrorMessage {
    override def messageKey: String = key
    override def messageArgs: Seq[String] = args
  }


  "ErrorMessage.toText" should {
    "call Messages with the key and arguments" in {
      implicit val messages: Messages = mock[Messages]

      when(messages.apply("error.test", "arg1", "arg2")).thenReturn("Formatted message")

      val error = TestErrorMessage("error.test", Seq("arg1", "arg2"))

      val result = error.toText

      result shouldBe "Formatted message"
      verify(messages).apply("error.test", "arg1", "arg2")
    }

    "work with no args" in {
      implicit val messages: Messages = mock[Messages]
      when(messages.apply("error.simple")).thenReturn("Simple message")

      val error = TestErrorMessage("error.simple")

      error.toText shouldBe "Simple message"
      verify(messages).apply("error.simple")
    }
  }
}
