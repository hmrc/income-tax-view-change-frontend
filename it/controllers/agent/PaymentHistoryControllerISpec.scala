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

package controllers.agent

import audit.models.PaymentHistoryResponseAuditModel
import auth.MtdItUser
import com.github.tomakehurst.wiremock.client.WireMock
import config.featureswitch.{CutOverCredits, MFACreditsAndDebits, PaymentHistoryRefunds}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.AccountingPeriodModel
import models.financialDetails.Payment
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status._
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate


class PaymentHistoryControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
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

  val testArn: String = "1"
  val currentTaxYearEnd: Int = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd: Int = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd: Int = currentTaxYearEnd - 2

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.getYear.toString),
    businesses = List(BusinessDetailsModel(
      "testId",
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      None,
      None,
      Some(getCurrentTaxYearEnd),
      None
    )),
    properties = Nil
  )

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSourceDetailsModel,
    None, Some("1234567890"), None, Some(Agent), Some(testArn)
  )(FakeRequest())

  s"GET ${controllers.routes.PaymentHistoryController.showAgent.url}" should {
    s"SEE_OTHER to " when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsModel
        )

        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, payments)

        val result = IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetailsWithConfirmation)

        Then("The user is redirected to")
        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }

  s"return $OK with the enter client utr page" when {
    s"return $OK" in {
      disable(CutOverCredits)
      disable(MFACreditsAndDebits)
      stubAuthorisedAgentUser(authorised = true)
      disable(PaymentHistoryRefunds)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetailsModel
      )

      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, payments)

      val result = IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetailsWithConfirmation)

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent("paymentHistory.heading")
      )

      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, payments, CutOverCreditsEnabled = false,
        MFACreditsEnabled = false).detail)
    }

    s"return payment from earlier tax year description when CutOverCreditsEnabled and credit is defined $OK" in {
      enable(CutOverCredits)
      enable(MFACreditsAndDebits)
      stubAuthorisedAgentUser(authorised = true)
      disable(PaymentHistoryRefunds)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetailsModel
      )

      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, payments)

      val result = IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetailsWithConfirmation)

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent("paymentHistory.heading")
      )

      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, payments, CutOverCreditsEnabled = true,
        MFACreditsEnabled = true).detail)
    }

  }
  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetailsWithConfirmation))
    }
  }
}
