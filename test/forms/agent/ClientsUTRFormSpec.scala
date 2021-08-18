/*
 * Copyright 2021 HM Revenue & Customs
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

package forms.agent

import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.{Form, FormError}

class ClientsUTRFormSpec extends WordSpec with MustMatchers {

  def form(optValue: Option[String]): Form[String] = ClientsUTRForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map(ClientsUTRForm.utr -> value))
  )

  "ClientsUTRForm" must {

    "return a valid utr" when {
      "bound with a valid utr" in {
        form(Some("1234567890")).value mustBe Some("1234567890")
      }
      "bound with a valid utr that contains spaces" in {
        form(Some("1 2 3 4 5 6 7 8 9 0")).value mustBe Some("1234567890")
      }
    }

    "return an error that the utr is empty" when {
      "the input is blank" in {
        form(Some("")).errors mustBe Seq(FormError(ClientsUTRForm.utr, ClientsUTRForm.utrEmptyError))
      }
      "there is no input" in {
        form(None).errors mustBe Seq(FormError(ClientsUTRForm.utr, ClientsUTRForm.utrEmptyError))
      }
    }

    "return an error that the utr is not the correct length" when {
      List(
        "the input contains more than 10 digits" -> "12345678901",
        "the input contains less than 10 digits" -> "123456789",
      ) foreach {
        case (test, value) => test in {
          form(Some(value)).errors mustBe Seq(FormError(ClientsUTRForm.utr, ClientsUTRForm.utrLengthIncorrect))
        }
      }
    }

    "return an error that the utr has non numeric characters" when {
      "the input contains non numeric characters" in {
        form(Some("123456789a")).errors mustBe Seq(FormError(ClientsUTRForm.utr, ClientsUTRForm.utrNonNumeric))
      }
    }
  }

}
