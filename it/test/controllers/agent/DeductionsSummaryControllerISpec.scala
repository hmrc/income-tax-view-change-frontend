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

import audit.models.AllowanceAndDeductionsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks._
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.liabilitycalculation.viewmodels.AllowancesAndDeductionsViewModel
import play.api.http.Status._
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.NewCalcBreakdownItTestConstants._
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class DeductionsSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {


  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSource,
    None, Some("1234567890"), credId = None, Some(testUserTypeAgent), arn = Some("1")
  )(FakeRequest())
  lazy val fixedDate : LocalDate = LocalDate.of(2024, 6, 5)

  lazy val incomeSource: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
      None,
      None,
      Some(getCurrentTaxYearEnd),
      None,
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = Nil
  )

  "Calling the DeductionsSummaryController.showDeductionsSummary(taxYear)" should {
    "give the correct response" in {
      Given("I login as agent")
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSource
      )

      And("I stub a successful calculation response for 2017-18")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationModelSuccessful
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/agents/calculation/$testYear/income")
      val res = IncomeTaxViewChangeFrontend.getDeductionsSummary(testYear, clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid, 0)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear, 1)

      Then("I see Allowances and deductions page")
      res should have(
        httpStatus(OK),
        pageTitleAgent("deduction_breakdown.heading")
      )

      verifyAuditContainsDetail(AllowanceAndDeductionsResponseAuditModel(testUser,
        AllowancesAndDeductionsViewModel(liabilityCalculationModelSuccessful.calculation)).detail)
    }
  }
}
