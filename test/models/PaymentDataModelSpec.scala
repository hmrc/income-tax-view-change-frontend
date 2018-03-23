/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.PaymentDataTestConstants._
import models.core.PaymentDataModel
import org.scalatest.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class PaymentDataModelSpec extends UnitSpec with Matchers {

  "The Payment model" should {
    "be formatted to JSON correctly" in {
      Json.toJson[PaymentDataModel](testPaymentDataModel) shouldBe testPaymentDataJson
    }

    "be able to parse a JSON input as a String into the model" in {
      Json.parse(testPaymentDataJson.toString()).as[PaymentDataModel] shouldBe testPaymentDataModel
    }

    "have the correct taxType" in {
      testPaymentDataModel.taxType shouldBe testTaxType
    }
    "have the correct taxReference" in {
      testPaymentDataModel.taxReference shouldBe testMtditid
    }
    "have the correct amountInPence" in {
      testPaymentDataModel.amountInPence shouldBe testAmountInPence
    }
    "have the correct returnUrl" in {
      testPaymentDataModel.returnUrl shouldBe testPaymentRedirectUrl
    }
  }
}
