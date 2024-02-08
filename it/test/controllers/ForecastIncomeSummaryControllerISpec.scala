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

import audit.models.ForecastIncomeAuditModel
import auth.MtdItUserWithNino
import config.featureswitch.ForecastCalculation
import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import play.api.http.Status._
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

object ForecastIncomeSummaryControllerTestConstants {
  val mtdItUser: MtdItUserWithNino[_] = MtdItUserWithNino(testMtditid, testNino, None, None, Some("1234567890"),
    Some("12345-credId"), Some(Individual), None)(FakeRequest())

  val taxableIncome = 12500

  val endOfYearEstimate: EndOfYearEstimate = EndOfYearEstimate(
    incomeSource = Some(List(
      IncomeSource("01", Some("self-employment1"), taxableIncome),
      IncomeSource("01", Some("self-employment2"), taxableIncome),
      IncomeSource("02", None, taxableIncome),
      IncomeSource("03", None, taxableIncome),
      IncomeSource("04", None, taxableIncome),
      IncomeSource("05", Some("employment1"), taxableIncome),
      IncomeSource("05", Some("employment2"), taxableIncome),
      IncomeSource("06", None, taxableIncome),
      IncomeSource("07", None, taxableIncome),
      IncomeSource("08", None, taxableIncome),
      IncomeSource("09", None, taxableIncome),
      IncomeSource("10", None, taxableIncome),
      IncomeSource("11", None, taxableIncome),
      IncomeSource("12", None, taxableIncome),
      IncomeSource("13", None, taxableIncome),
      IncomeSource("14", None, taxableIncome),
      IncomeSource("15", None, taxableIncome),
      IncomeSource("16", None, taxableIncome),
      IncomeSource("17", None, taxableIncome),
      IncomeSource("18", None, taxableIncome),
      IncomeSource("19", None, taxableIncome),
      IncomeSource("20", None, taxableIncome),
      IncomeSource("21", None, taxableIncome),
      IncomeSource("22", None, taxableIncome),
      IncomeSource("98", None, taxableIncome)
    )),
    totalEstimatedIncome = Some(taxableIncome),
  )
}

class ForecastIncomeSummaryControllerISpec extends ComponentSpecBase {

  "Calling the ForecastIncomeSummaryController.show(taxYear)" when {

    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response, " should {
      "return the correct forecast income summary page and audit event" in {

        Given("I enable forecast calculation display")
        enable(ForecastCalculation)

        And("I stub a successful calculation response for 2017-18")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, "2018")(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/income/forecast")
        val res = IncomeTaxViewChangeFrontend.getForecastIncomeSummary(testYear)

        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, "2018")

        AuditStub.verifyAuditEvent(ForecastIncomeAuditModel(ForecastIncomeSummaryControllerTestConstants.mtdItUser,
          ForecastIncomeSummaryControllerTestConstants.endOfYearEstimate))

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
          pageTitleIndividual("Page not found - 404", isErrorPage = true)
        )
      }
    }

    unauthorisedTest("/" + testYear + "/forecast-income")

  }
}
