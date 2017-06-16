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
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, IncomeTaxViewChangeStub}
import org.jsoup.Jsoup
import play.api.http.Status

class EstimatedTaxLiabilityControllerISpec extends ComponentSpecBase {

  "Calling the EstimatedTaxLiabilityController" when {
    "authorised with an active enrolment" should {

      "return the correct page with a valid total" in {
        AuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, "01234567", "2017-07-06 12:34:56.789", 1800.00)
        val res = IncomeTaxViewChangeFrontend.getEstimatedTaxLiability
        IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino)

        res.status shouldBe Status.OK

        val document = Jsoup.parse(res.body)
        document.title shouldBe "Your estimated tax amount"
        document.getElementById("estimate-amount").html shouldBe "£1,800.00"
      }
    }
    "unauthorised" should {
      "redirect to sign in" in {
        AuthStub.stubUnauthorised()
        val res = IncomeTaxViewChangeFrontend.getEstimatedTaxLiability

        res.status shouldBe Status.SEE_OTHER
        res.header("Location").getOrElse("") should include ("/gg/sign-in?continue")
       }
    }
  }
}
