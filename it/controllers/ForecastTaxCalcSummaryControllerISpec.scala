
package controllers

import audit.models.ForecastTaxCalculationAuditModel
import auth.MtdItUserWithNino
import config.featureswitch.ForecastCalculation
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxCalculationStub}
import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testYear}
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

object ForecastTaxSummaryControllerTestConstants {
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
    totalTaxableIncome = Some(taxableIncome),
    incomeTaxAmount = Some(5000.99),
    nic2 = Some(5000.99),
    nic4 = Some(5000.99),
    totalNicAmount = Some(5000.99),
    totalTaxDeductedBeforeCodingOut = Some(5000.99),
    saUnderpaymentsCodedOut = Some(5000.99),
    totalStudentLoansRepaymentAmount = Some(5000.99),
    totalAnnuityPaymentsTaxCharged = Some(5000.99),
    totalRoyaltyPaymentsTaxCharged = Some(5000.99),
    totalTaxDeducted = Some(-99999999999.99),
    incomeTaxNicAmount = Some(-99999999999.99),
    cgtAmount = Some(5000.99),
    incomeTaxNicAndCgtAmount = Some(5000.99)
  )
}

class ForecastTaxCalcSummaryControllerISpec extends ComponentSpecBase {

  "Calling the ForecastTaxCalcSummaryController.show(taxYear)" when {
    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid LiabilityCalculationModel response" should {
      "return the forecast tax calc summary page and audit event when the forecast calculation fs is enabled" in {

        Given("I enable the forecast calculation fs")
        enable(ForecastCalculation)

        And("I stub a successful calculation response")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due/forecast")
        val res = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(testYear)

        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

        AuditStub.verifyAuditEvent(ForecastTaxCalculationAuditModel(ForecastTaxSummaryControllerTestConstants.mtdItUser,
          ForecastTaxSummaryControllerTestConstants.endOfYearEstimate))

        res should have(
          httpStatus(OK),
          pageTitleIndividual("forecast_taxCalc.heading")
        )
      }

      s"return $NOT_FOUND when the forecast calculation fs is disabled" in {
        Given("I disable the forecast calculation fs")
        disable(ForecastCalculation)

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due/forecast")
        val res = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(testYear)

        res should have(
          httpStatus(NOT_FOUND),
          pageTitleIndividual("Page not found - 404", isErrorPage = true)
        )
      }
    }
    unauthorisedTest("/" + testYear + "/forecast-tax-calculation")
  }

}
