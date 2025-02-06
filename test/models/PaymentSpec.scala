/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import testConstants.FinancialDetailsTestConstants._
import models.financialDetails._
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsObject, JsSuccess, Json}

import java.time.LocalDate

class PaymentSpec extends AnyWordSpecLike with Matchers {

  val paymentFull: Payment = Payment(
    reference = Some("reference"),
    amount = Some(100.00),
    outstandingAmount = Some(50.00),
    method = Some("method"),
    documentDescription = Some("docDescription"),
    lot = Some("lot"),
    lotItem = Some("lotItem"),
    dueDate = Some(LocalDate.parse("2022-08-16")),
    documentDate = LocalDate.parse("2022-08-16"),
    Some("DOCID01")
  )

  val paymentFullJson: JsObject = Json.obj(
    "reference"           -> "reference",
    "amount"              -> 100.00,
    "outstandingAmount"   -> 50.00,
    "method"              -> "method",
    "documentDescription" -> "docDescription",
    "lot"                 -> "lot",
    "lotItem"             -> "lotItem",
    "dueDate"             -> "2022-08-16",
    "documentDate"        -> "2022-08-16",
    "transactionId"       -> "DOCID01"
  )

  val paymentMinimal: Payment =
    Payment(None, None, None, None, None, None, None, None, LocalDate.parse("2018-08-16"), None)

  val paymentMinimalJson: JsObject = Json.obj("documentDate" -> "2018-08-16")

  "Payment" should {

    "read from json" when {
      "the json is complete" in {
        Json.fromJson[Payment](paymentFullJson) shouldBe JsSuccess(paymentFull)
      }
      "the json is minimal" in {
        Json.fromJson[Payment](paymentMinimalJson) shouldBe JsSuccess(paymentMinimal)
      }
    }

    "write to json" when {
      "the payment model is full" in {
        Json.toJson(paymentFull) shouldBe paymentFullJson
      }
      "the payment model is minimal" in {
        Json.toJson(paymentMinimal) shouldBe paymentMinimalJson
      }
    }

  }

}
