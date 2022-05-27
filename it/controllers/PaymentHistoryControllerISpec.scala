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

import audit.models.PaymentHistoryResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CutOverCredits, R7bTxmEvents}
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.financialDetails.Payment
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.PaymentHistoryMessages.paymentHistoryTitle

class PaymentHistoryControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(R7bTxmEvents)
  }

  val paymentsFull: Seq[Payment] = Seq(
    Payment(reference = Some("reference"), amount = Some(100.00), method = Some("method"), lot = Some("lot"), lotItem = Some("lotItem"), date = Some("2018-04-25"), Some("DOCID01"))
  )

  val paymentsFull2: Seq[Payment] = Seq(
    Payment(reference = Some("reference2"), amount = Some(200.00), method = Some("method2"), lot = Some("lot2"), lotItem = Some("lotItem2"), date = Some("2018-12-12"), Some("DOCID02"))
  )

  val paymentsnotFull: List[Payment] = List(
    Payment(reference = Some("reference"), amount = Some(-10000.00), method = Some("method"), lot = None, lotItem = None, date = Some("2018-04-25"), Some("AY777777202206"))
  )

  val currentTaxYearEnd: Int = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd: Int = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd: Int = currentTaxYearEnd - 2

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  s"GET ${controllers.routes.PaymentHistoryController.show().url}" should {
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


  s"return $OK with the payment history page" when {
    "the payment history feature switch is enabled" in {
      isAuthorisedUser(authorised = true)
      stubUserDetails()
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentsFull)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleIndividual(paymentHistoryTitle)
      )

      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, paymentsFull, CutOverCreditsEnabled = false, R7bTxmEvents = false).detail)
    }

    "return payment from earlier tax year description when CutOverCreditsEnabled and credit is defined" in {
      enable(R7bTxmEvents)
      enable(CutOverCredits)
      isAuthorisedUser(authorised = true)
      stubUserDetails()
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentsnotFull)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleIndividual(paymentHistoryTitle)
      )

      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, paymentsnotFull, CutOverCreditsEnabled = true, R7bTxmEvents = true).detail)
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getPaymentHistory)
    }
  }
}
