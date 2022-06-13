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
import config.featureswitch.{CutOverCredits, R7bTxmEvents}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.AccountingPeriodModel
import models.financialDetails.Payment
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status._
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate


class PaymentHistoryControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
  }

  val paymentsFull: Seq[Payment] = Seq(
    Payment(reference = Some("reference"), amount = Some(100.00), method = Some("method"), lot = Some("lot"), lotItem = Some("lotItem"), date = Some("2018-04-25"), Some("DOCID01"))
  )

  val paymentsnotFull: List[Payment] = List(
    Payment(reference = Some("reference"), amount = Some(-10000.00), method = Some("method"), lot = None, lotItem = None, date = Some("2018-04-25"), Some("AY777777202206"))
  )

  val testArn: String = "1"
  val currentTaxYearEnd: Int = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd: Int = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd: Int = currentTaxYearEnd - 2

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.getYear.toString),
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      None,
      Some(getCurrentTaxYearEnd)
    )),
    property = None
  )

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSourceDetailsModel,
    None, Some("1234567890"), None, Some("Agent"), Some(testArn)
  )(FakeRequest())

  s"GET ${controllers.routes.PaymentHistoryController.showAgent().url}" should {
    s"SEE_OTHER to " when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsModel
        )

        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentsFull)

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
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetailsModel
      )

      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentsFull)

      val result = IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetailsWithConfirmation)

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent("paymentHistory.heading")
      )

      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, paymentsFull, CutOverCreditsEnabled = false, R7bTxmEvents = false).detail)
    }

    s"return payment from earlier tax year description when CutOverCreditsEnabled and credit is defined $OK" in {
      enable(R7bTxmEvents)
      enable(CutOverCredits)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetailsModel
      )

      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentsnotFull)

      val result = IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetailsWithConfirmation)

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent("paymentHistory.heading")
      )

      verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser, paymentsnotFull, CutOverCreditsEnabled = true, R7bTxmEvents = true).detail)
    }

  }
  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetailsWithConfirmation))
    }
  }
}
