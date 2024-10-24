/*
 * Copyright 2024 HM Revenue & Customs
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

import audit.models.ForecastTaxCalculationAuditModel
import auth.MtdItUserWithNino
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxCalculationStub}
import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import play.api.http.Status.OK
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
    }
    unauthorisedTest("/" + testYear + "/forecast-tax-calculation")
  }

}
