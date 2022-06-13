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

import config.featureswitch.ForecastCalculation
import helpers.ComponentSpecBase
import helpers.servicemocks._
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull

class ForecastIncomeSummaryControllerISpec extends ComponentSpecBase {

  "Calling the ForecastIncomeSummaryController.show(taxYear)" when {

    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response, " should {
      "return the correct forecast income summary page" in {

        Given("I enable forecast calculation display")
        enable(ForecastCalculation)

        And("I stub a successful calculation response for 2017-18")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, "2018")(
          status = OK,
          body = liabilityCalculationModelSuccessFull
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/income/forecast")
        val res = IncomeTaxViewChangeFrontend.getForecastIncomeSummary(testYear)

        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, "2018")

        res should have(
          httpStatus(OK),
          pageTitleIndividual("forecast_income.heading")
        )
      }

      "return notfound when forecast calculation is disabled" in {

        Given("I disable forecast calculation display")
        disable(ForecastCalculation)

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/income/forecast")
        val res = IncomeTaxViewChangeFrontend.getForecastIncomeSummary(testYear)

        res should have(
          httpStatus(NOT_FOUND),
          pageTitleIndividual("Page not found - 404")
        )
      }
    }

    unauthorisedTest("/" + testYear + "/forecast-income")

  }
}
