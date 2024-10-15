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
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.PaymentHistoryRefunds
import models.financialDetails.Payment
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class PaymentHistoryControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val payments: List[Payment] = List(
    Payment(reference = Some("payment1"), amount = Some(100.00), outstandingAmount = None, method = Some("method"),
      documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"), dueDate = Some(LocalDate.parse("2018-04-25")),
      documentDate = LocalDate.parse("2018-04-25"), transactionId = Some("DOCID01"),
      mainType = Some("SA Balancing Charge")),
    Payment(reference = Some("mfa1"), amount = Some(-10000.00), outstandingAmount = None, method = Some("method"),
      documentDescription = Some("TRM New Charge"), lot = None, lotItem = None, dueDate = None,
      documentDate = LocalDate.parse("2018-04-25"), transactionId = Some("AY777777202206"),
      mainType = Some("ITSA Overpayment Relief")),
    Payment(reference = Some("cutover1"), amount = Some(-10000.00), outstandingAmount = None, method = Some("method"),
      documentDescription = None, lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2018-04-25")), documentDate = LocalDate.parse("2018-04-25"),
      transactionId = Some("AY777777202206"),
      mainType = Some("ITSA Cutover Credits")),
    Payment(reference = Some("bcc"), amount = Some(-10000.00), outstandingAmount = None, method = Some("method"),
      documentDescription = None, lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2018-04-25")), documentDate = LocalDate.parse("2018-04-25"),
      transactionId = Some("AY777777202203"),
      mainType = Some("SA Balancing Charge Credit"))
  )

  val currentTaxYearEnd: Int = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd: Int = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd: Int = currentTaxYearEnd - 2

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  s"GET ${controllers.routes.PaymentHistoryController.show().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      "the user is not authenticated" in {
        isAuthorisedUser(authorised = false)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, payments)
        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, payments)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }
  }


  s"return $OK with the payment history page" when {
    "the payment history feature switch is enabled" in {
      isAuthorisedUser(authorised = true)
      stubUserDetails()
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, payments)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleIndividual("paymentHistory.heading"),
        elementTextBySelector("#refundstatus")(""),
      )

      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, payments).detail)
    }

    "return payment from earlier tax year description when CutOverCreditsEnabled and credit is defined" in {
      enable(PaymentHistoryRefunds)
      isAuthorisedUser(authorised = true)
      stubUserDetails()
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, payments)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleIndividual("paymentHistory.paymentAndRefundHistory.heading"),
        elementTextBySelector("h1")(messagesAPI("paymentHistory.paymentAndRefundHistory.heading")),
        elementTextBySelector("#refundstatus")(messagesAPI("paymentHistory.check-refund-1") + " " +
          messagesAPI("paymentHistory.check-refund-2") + " " + messagesAPI("paymentHistory.check-refund-3")),
      )

      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, payments).detail)
    }


    "Show the user the payments history page" when {
      "The feature switch is disabled" in {
        disable(PaymentHistoryRefunds)
        isAuthorisedUser(authorised = true)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, payments)
        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory
        Then("The Payment History page is returned to the user")
        result should have(
          httpStatus(OK),
          pageTitleIndividual("paymentHistory.heading"),
          elementTextBySelector("h1")(messagesAPI("paymentHistory.heading"))
        )
      }
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getPaymentHistory)
    }
  }
}
