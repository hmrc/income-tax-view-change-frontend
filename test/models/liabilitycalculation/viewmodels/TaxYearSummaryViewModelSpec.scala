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

import mocks.services.MockDateService
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.viewmodels.CalculationSummary.localDate
import models.obligations.ObligationsModel
import models.taxyearsummary.TaxYearSummaryChargeItem
import services.DateService
import testConstants.ChargeConstants
import testConstants.NextUpdatesTestConstants.nextUpdatesDataSelfEmploymentSuccessModel
import testUtils.UnitSpec

import java.time.LocalDate

class TaxYearSummaryViewModelSpec extends UnitSpec with ChargeConstants with MockDateService {

  implicit val dateService: DateService = mockDateService

  val testWithMissingOriginalAmountChargesList: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(originalAmount = 0)
  ).map(chargeItem => TaxYearSummaryChargeItem.fromChargeItem(chargeItem, chargeItem.dueDate))

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))

  val testCalculationSummary: CalculationSummary = CalculationSummary(
    timestamp = Some("2020-01-01T00:35:34.185Z".toZonedDateTime.toLocalDate),
    income = 1,
    deductions = 2.02,
    totalTaxableIncome = 3,
    taxDue = 4.04,
    crystallised = true,
    unattendedCalc = true,
    forecastIncome = Some(12500),
    forecastIncomeTaxAndNics = Some(5000.99),
    forecastAllowancesAndDeductions = Some(4200.00),
    forecastTotalTaxableIncome = Some(8300),
    periodFrom = Some(LocalDate.of(2020 - 1, 1, 1)),
    periodTo = Some(LocalDate.of(2021, 1, 1))
  )

  val testCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(poaTaxYear = Some(TaxYear(2024, 2025)))

  "TaxYearSummaryViewModel model" when {
    "forecastIncomeTaxAndNics is not defined in CalculationSummaryValue" should {
      "throw IllegalArgumentException" in {

        val thrown = the[IllegalArgumentException] thrownBy TaxYearSummaryViewModel.apply(
          Some(testCalculationSummary.copy(forecastIncomeTaxAndNics = None)), None,
          testWithMissingOriginalAmountChargesList,
          testObligationsModel, showForecastData = true, ctaViewModel = testCTAViewModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false
        )

        thrown.getMessage shouldBe "requirement failed: missing Forecast Tax Due"
      }
    }

    "Calculation timestamp is not defined in CalculationSummaryValue" should {
      "throw IllegalArgumentException" in {
        val thrown = the[IllegalArgumentException] thrownBy TaxYearSummaryViewModel(
          Some(testCalculationSummary.copy(timestamp = None)), None,
          testWithMissingOriginalAmountChargesList,
          testObligationsModel, showForecastData = true, ctaViewModel = testCTAViewModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false
        )

        thrown.getMessage shouldBe "requirement failed: missing Calculation timestamp"
      }
    }
  }

  "TYSClaimToAdjustViewModel claimToAdjustTaxYear val" should {
      "return a TaxYear" in {
        val testModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(Some(TaxYear(2023, 2024)))
        testModel.claimToAdjustTaxYear shouldBe Some(TaxYear(2023, 2024))
      }
    }
}
