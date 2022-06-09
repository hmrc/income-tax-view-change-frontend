
package controllers

import config.featureswitch.ForecastCalculation
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxCalculationStub
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import testConstants.BaseIntegrationTestConstants.{testNino, testYear}
import testConstants.NewCalcBreakdownItTestConstants.{liabilityCalculationModelError, liabilityCalculationModelSuccessFull}

class ForecastTaxCalcSummaryControllerISpec extends ComponentSpecBase {

  "Calling the ForecastTaxCalcSummaryController.show(taxYear)" when {
    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid LiabilityCalculationModel response" should {
      "return the forecast tax calc summary page when the forecast calculation fs is enabled" in {

        Given("I enable the forecast calculation fs")
        enable(ForecastCalculation)

        And("I stub a successful calculation response")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due/forecast")
        val res = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(testYear)

        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

        res should have(
          httpStatus(OK),
          pageTitleIndividual("Forecast tax calculation"),
          elementTextBySelector("h1")("6 April 2017 to 5 April 2018 Forecast tax calculation")
        )
      }

      s"return $NOT_FOUND when the forecast calculation fs is disabled" in {
        Given("I disable the forecast calculation fs")
        disable(ForecastCalculation)

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due/forecast")
        val res = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(testYear)

        res should have (
          httpStatus(NOT_FOUND),
          pageTitleIndividual("Page not found - 404"),
          elementTextBySelector("h1")("This page can’t be found")
        )
      }
    }

    unauthorisedTest("/" + testYear + "/forecast-tax-calculation")
  }

}
