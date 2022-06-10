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
import helpers.servicemocks._
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull
import testConstants.messages.DeductionsSummaryMessages._

class DeductionsSummaryControllerISpec extends ComponentSpecBase {

  "Calling the DeductionsSummaryController.showDeductionsSummary(taxYear)" when {
    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response" should {
      "return the correct income summary page" in {
        When("I wiremock stub a successful Deductions Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

        And("I stub a successful calculation response")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/income")
        val res = IncomeTaxViewChangeFrontend.getDeductionsSummary(testYear)

        verifyIncomeSourceDetailsCall(testMtditid, 0)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

        Then("I see Allowances and deductions page")
        res should have(
          httpStatus(OK),
          pageTitleIndividual(deductionsSummaryTitle),
          elementTextBySelector("h1")(deductionsSummaryHeading)
        )

      }
    }

    unauthorisedTest("/" + testYear + "/allowances-and-deductions")

  }
}
