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

package controllers.agent

import java.time.{LocalDate, LocalDateTime}

import testConstants.BaseIntegrationTestConstants._
import testConstants.CalcDataIntegrationTestConstants._
import testConstants.PaymentHistoryTestConstraints.getCurrentTaxYearEnd
import testConstants.messages.{DeductionsSummaryMessages => messages}
import audit.models.AllowanceAndDeductionsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, TxmEventsApproved}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks._
import models.calculation.{Calculation, CalculationItem, ListCalculationItems}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name

class DeductionsSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val clientDetails: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSource,
    None, Some("1234567890"), credId = None, Some(testUserTypeAgent), arn = Some("1")
  )(FakeRequest())

  lazy val incomeSource: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      None,
      Some(getCurrentTaxYearEnd)
    )),
    property = None
  )

  "Calling the DeductionsSummaryController.showDeductionsSummary(taxYear)" should {
    def test(txmApproved: Boolean): Unit = {
      And("I wiremock stub a successful Deductions Source Details response with single Business and Property income")
      stubAuthorisedAgentUser(authorised = true)

      if (txmApproved) {
        enable(TxmEventsApproved)
      } else {
        disable(TxmEventsApproved)
      }

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSource
      )

      And("I stub a successful calculation response for 2017-18")
      IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
        status = OK,
        body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
      )
      IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
        status = OK,
        body = estimatedCalculationFullJson
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/agents/calculation/$testYear/income")
      val res = IncomeTaxViewChangeFrontend.getDeductionsSummary(testYear, clientDetails)

      verifyIncomeSourceDetailsCall(testMtditid, 0)
      IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18", 1)
      IndividualCalculationStub.verifyGetCalculation(testNino, "idOne", 1)

      Then("I see Allowances and deductions page")
      res should have(
        httpStatus(OK),
        pageTitle(messages.agentsDeductionsSummaryTitle),
      )
      val expectedAllowancesAndDeductions = estimatedCalculationFullJson.as[Calculation].allowancesAndDeductions
      verifyAuditContainsDetail(AllowanceAndDeductionsResponseAuditModel(testUser, expectedAllowancesAndDeductions, txmApproved).detail)
    }

    "return the correct deductions summary page with TXMEvents approved" in {
      test(true)
    }

    "return the correct deductions summary page with TXMEvents disabled" in {
      test(false)
    }
  }
}
