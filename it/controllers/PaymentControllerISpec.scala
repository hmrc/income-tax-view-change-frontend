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

package controllers

import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.PaymentJourneyModel
import play.api.libs.json.{JsValue, Json}

class PaymentControllerISpec extends ComponentSpecBase {

  val url: String = "/pay-api/mtd-income-tax/sa/journey/start"

  val submissionJson: JsValue = Json.parse(
    s"""
       |{
       | "utr": "saUtr",
       | "amountInPence": 10000,
       | "returnUrl": "${appConfig.paymentRedirectUrl}",
       | "backUrl": "${appConfig.paymentRedirectUrl}"
       |}
    """.stripMargin
  )

  "Calling .paymentHandoff" should {

    "redirect the user correctly" when {

      "the payments api responds with a 200 and valid json" in {
        IncomeTaxViewChangeStub.stubPayApiResponse(url, 201, Json.toJson(PaymentJourneyModel("id", "redirect-url")))
        val res = IncomeTaxViewChangeFrontend.getPay(10000)
        IncomeTaxViewChangeStub.verifyStubPayApi(url, submissionJson)
        res.status shouldBe 303
        res.header("Location") shouldBe Some("redirect-url")
      }
    }

    "return an internal server error" when {

      "the payments api responds with a 500" in {
        IncomeTaxViewChangeStub.stubPayApiResponse(url, 500, Json.toJson(PaymentJourneyModel("id", "redirect-url")))
        val res = IncomeTaxViewChangeFrontend.getPay(10000)
        IncomeTaxViewChangeStub.verifyStubPayApi(url, submissionJson)
        res.status shouldBe 500
      }
    }
  }
}
