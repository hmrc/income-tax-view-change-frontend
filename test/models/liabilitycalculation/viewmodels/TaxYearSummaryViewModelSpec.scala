/*
 * Copyright 2023 HM Revenue & Customs
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

package models.liabilitycalculation.viewmodels

import models.financialDetails.DocumentDetailWithDueDate
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.viewmodels.CalculationSummary.localDate
import models.nextUpdates.ObligationsModel
import testConstants.FinancialDetailsTestConstants.{dateService, fullDocumentDetailModel, fullDocumentDetailWithDueDateModel}
import testConstants.NextUpdatesTestConstants.nextUpdatesDataSelfEmploymentSuccessModel
import testUtils.UnitSpec

import java.time.LocalDate

class TaxYearSummaryViewModelSpec extends UnitSpec {


  val testWithMissingOriginalAmountChargesList: List[DocumentDetailWithDueDate] = List(
    fullDocumentDetailWithDueDateModel.copy(documentDetail = fullDocumentDetailModel.copy(originalAmount = 0))
  )

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))

  val testCalculationSummary: CalculationSummary = CalculationSummary(
    timestamp = Some("2020-01-01T00:35:34.185Z".toZonedDateTime.toLocalDate),
    income = 1,
    deductions = 2.02,
    totalTaxableIncome = 3,
    taxDue = 4.04,
    crystallised = Some(true),
    unattendedCalc = true,
    forecastIncome = Some(12500),
    forecastIncomeTaxAndNics = Some(5000.99),
    forecastAllowancesAndDeductions = Some(4200.00),
    forecastTotalTaxableIncome = Some(8300),
    periodFrom = Some(LocalDate.of(2020 - 1, 1, 1)),
    periodTo = Some(LocalDate.of(2021, 1, 1))
  )

  val testCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = true, poaTaxYear = Some(TaxYear(2024, 2025)))

  "TaxYearSummaryViewModel model" when {
    "forecastIncomeTaxAndNics is not defined in CalculationSummaryValue" should {
      "throw IllegalArgumentException" in {

        val thrown = the[IllegalArgumentException] thrownBy TaxYearSummaryViewModel.apply(
          Some(testCalculationSummary.copy(forecastIncomeTaxAndNics = None)),
          testWithMissingOriginalAmountChargesList,
          testObligationsModel, codingOutEnabled = true, showForecastData = true, ctaViewModel = testCTAViewModel
        )

        thrown.getMessage shouldBe "requirement failed: missing Forecast Tax Due"
      }
    }

    "Calculation timestamp is not defined in CalculationSummaryValue" should {
      "throw IllegalArgumentException" in {
        val thrown = the[IllegalArgumentException] thrownBy TaxYearSummaryViewModel(
          Some(testCalculationSummary.copy(timestamp = None)),
          testWithMissingOriginalAmountChargesList,
          testObligationsModel, codingOutEnabled = true, showForecastData = true, ctaViewModel = testCTAViewModel
        )

        thrown.getMessage shouldBe "requirement failed: missing Calculation timestamp"
      }
    }

//    "originalAmount is not defined in Charge list" should {
//      "throw IllegalArgumentException" in {
//        val thrown = the[IllegalArgumentException] thrownBy TaxYearSummaryViewModel(
//          Some(testCalculationSummary),
//          testWithMissingOriginalAmountChargesList,
//          testObligationsModel,
//          codingOutEnabled = true, ctaViewModel = testCTAViewModel
//        )
//
//        thrown.getMessage shouldBe "requirement failed: missing originalAmount on charges"
//      }
//    }
  }

  "TYSClaimToAdjustViewModel claimToAdjustTaxYear val" when {
    "adjustPaymentsOnAccountFSEnabled is false" should {
      "return None" in {
        val testModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = false, Some(TaxYear(2023, 2024)))

        testModel.claimToAdjustTaxYear shouldBe None
      }
    }
    "adjustPaymentsOnAccountFSEnabled is true" should {
      "return a TaxYear" in {
        val testModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = true, Some(TaxYear(2023, 2024)))

        testModel.claimToAdjustTaxYear shouldBe Some(TaxYear(2023, 2024))
      }
    }
  }
  "TYSClaimToAdjustViewModel object" when {
    "ctaLink val is called for an individual" should {
      "return the correct redirect link" in {
        TYSClaimToAdjustViewModel.ctaLink(false) shouldBe "/report-quarterly/income-and-expenses/view/adjust-poa/start"
      }
    }
    "ctaLink val is called for an agent" should {
      "return the correct redirect link" in {
        TYSClaimToAdjustViewModel.ctaLink(true) shouldBe "/report-quarterly/income-and-expenses/view/agents/adjust-poa/start"
      }
    }
  }
}
