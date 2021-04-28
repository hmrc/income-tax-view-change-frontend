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
import audit.models.{PaymentHistoryRequestAuditModel, PaymentHistoryResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.{API5, FeatureSwitching, PaymentHistory}
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.financialDetails.Payment
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest

class PaymentHistoryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(API5)
    enable(PaymentHistory)
  }

  val paymentsFull: Seq[Payment] = Seq(
    Payment(
      reference = Some("reference"),
      amount = Some(100.00),
      method = Some("method"),
      lot = Some("lot"),
      lotItem = Some("lotItem"),
      date = Some("2018-04-25")
    )
  )

  val currentTaxYearEnd: Int = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd: Int = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd: Int = currentTaxYearEnd - 2

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None,
    paymentHistoryBusinessAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  s"GET ${controllers.routes.PaymentHistoryController.viewPaymentHistory().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        isAuthorisedUser(authorised = false)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentsFull)
        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentsFull)

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
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentsFull)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentsFull)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

      Then(s"A not found page is returned to the user")
      result should have(
        httpStatus(NOT_FOUND),
        pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
      )
    }
  }

  s"return $OK with the payment history page" when {
    "the payment history feature switch is enabled" in {
      isAuthorisedUser(authorised = true)
      stubUserDetails()
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentsFull)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentsFull)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("Payment history - Business Tax account - GOV.UK")
      )

      verifyAuditContainsDetail(PaymentHistoryRequestAuditModel(testUser).detail)
      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, paymentsFull ++ paymentsFull).detail)
    }
  }
}
