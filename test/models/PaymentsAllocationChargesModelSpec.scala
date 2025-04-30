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

import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsSuccess, Json}
import services.DateService
import testConstants.BaseTestConstants.app
import testConstants.PaymentAllocationsTestConstants._
import testUtils.UnitSpec

class PaymentsAllocationChargesModelSpec extends UnitSpec with Matchers {

  implicit val dateService: DateService = app.injector.instanceOf[DateService]

  "PaymentDetails" should {

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialDetailsWithDocumentDetailsModel](paymentAllocationChargesModel) shouldBe validWrittenPaymentAllocationChargesJson
    }

    "find a correspond document detail amount to financial detail" in {
      val result = Json.fromJson[FinancialDetailsWithDocumentDetailsModel](validPaymentAllocationChargesJson)

      result.get.filteredDocumentDetails.head.originalAmount shouldBe -300
    }

    "contain only payments" in {
      val result = Json.fromJson[FinancialDetailsWithDocumentDetailsModel](validPaymentAllocationChargesJson)

      result.get.financialDetails.head.payments.length shouldBe 1

    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[FinancialDetailsWithDocumentDetailsModel](validPaymentAllocationChargesJson) shouldBe JsSuccess(paymentAllocationChargesModel)
    }
  }
}
