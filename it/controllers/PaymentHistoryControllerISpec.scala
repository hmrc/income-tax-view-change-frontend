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

package controllers

import assets.BaseIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.PaymentHistoryTestConstraints.getCurrentTaxYearEnd
import config.featureswitch.{FeatureSwitching, PaymentHistory}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse


class PaymentHistoryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(PaymentHistory)
  }

  val paymentFullJson: JsValue = Json.arr(Json.obj(
    "reference" -> "reference",
    "amount" -> 100.00,
    "method" -> "method",
    "lot" -> "lot",
    "lotItem" -> "lotItem",
    "date" -> "2018-04-25"
    )
  )

  val currentTaxYearEnd = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd = currentTaxYearEnd-1
  val twoPreviousTaxYearEnd = currentTaxYearEnd-2


  s"GET ${controllers.routes.PaymentHistoryController.viewPaymentHistory().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        isAuthorisedUser(authorised = false)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentFullJson)
        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentFullJson)


        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }

  s"return $NOT_FOUND" when {
    "the payment history feature switch is disabled" in {
      disable(PaymentHistory)
      isAuthorisedUser(authorised = true)
      stubUserDetails()
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentFullJson)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentFullJson)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

      Then(s"A not found page is returned to the user")
      result should have(
        httpStatus(NOT_FOUND),
        pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
      )
    }
  }

  s"return $OK with the enter client utr page" when {
    "the payment history feature switch is enabled" in {
      isAuthorisedUser(authorised = true)
      stubUserDetails()
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentFullJson)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentFullJson)


      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("Payment history - Business Tax account - GOV.UK")
      )
    }
  }
}
