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

import implicits.ImplicitDateParser
import models.liabilitycalculation.{Message, Messages}
import testConstants.NewCalcBreakdownUnitTestConstants._
import testUtils.UnitSpec

import java.time.LocalDate

class CalculationSummaryModelSpec extends UnitSpec with ImplicitDateParser {

  "CalculationSummary model" when {
    "create a minimal CalculationSummary when there is a minimal Calculation response" in {
      CalculationSummary(liabilityCalculationModelDeductionsMinimal(calculationReason = "customerRequest")) shouldBe
        CalculationSummary(
          timestamp = None,
          crystallised = true,
          unattendedCalc = false,
          taxDue = 0.0,
          income = 0,
          deductions = 0.0,
          totalTaxableIncome = 0,
          forecastIncome = None,
          forecastIncomeTaxAndNics = None,
          forecastAllowancesAndDeductions = None,
          periodFrom = LocalDate.of(2022, 1, 1),
          periodTo = LocalDate.of(2023, 1, 1)
        )
    }

    "successful successModelFull" should {

      "create a full CalculationSummary when there is a full Calculation" in {
        val expectedCalculationSummary = CalculationSummary(
          timestamp = Some("2019-02-15T09:35:15.094Z".toZonedDateTime.toLocalDate),
          crystallised = true,
          unattendedCalc = false,
          taxDue = 5000.99,
          income = 12500,
          deductions = 12500,
          totalTaxableIncome = 12500,
          forecastIncome = Some(12500),
          forecastIncomeTaxAndNics = Some(5000.99),
          forecastAllowancesAndDeductions = Some(4200),
          forecastTotalTaxableIncome = Some(8300),
          periodFrom = LocalDate.of(2018, 1, 1),
          periodTo = LocalDate.of(2019, 1, 1),
          messages = Some(Messages(
            info = Some(Seq(Message(id = "C22211", text = "info msg text1"))),
            warnings = Some(Seq(Message(id = "C22214", text = "warn msg text1"))),
            errors = Some(Seq(Message(id = "C22216", text = "error msg text1")))
          ))
        )

        CalculationSummary(liabilityCalculationModelSuccessful) shouldBe expectedCalculationSummary
      }

      "create a full CalculationSummary with forecast calculation" when {
        "incomeTaxNicAndCgtAmount is not available then take incomeTaxNicAmount as forecastIncomeTaxAndNics" in {
          val expectedCalculationSummary = CalculationSummary(
            timestamp = Some("2019-02-15T09:35:15.094Z".toZonedDateTime.toLocalDate),
            crystallised = true,
            unattendedCalc = false,
            taxDue = 5000.99,
            income = 12500,
            deductions = 12500,
            totalTaxableIncome = 12500,
            forecastIncome = Some(12500),
            forecastIncomeTaxAndNics = Some(6000.99),
            forecastAllowancesAndDeductions = Some(4200),
            forecastTotalTaxableIncome = Some(8300),
            periodFrom = LocalDate.of(2018, 1, 1),
            periodTo = LocalDate.of(2019, 1, 1),
            messages = Some(Messages(
              info = Some(Seq(Message(id = "C22211", text = "info msg text1"))),
              warnings = Some(Seq(Message(id = "C22214", text = "warn msg text1"))),
              errors = Some(Seq(Message(id = "C22216", text = "error msg text1")))
            ))
          )

          val liabilityCalculationModel = liabilityCalculationModelSuccessful.copy(
            calculation = Some(liabilityCalculationModelSuccessful.calculation.get.copy(
              endOfYearEstimate = Some(liabilityCalculationModelSuccessful.calculation.get.endOfYearEstimate.get.copy(
                incomeTaxNicAndCgtAmount = None, incomeTaxNicAmount = Some(6000.99))))))

          CalculationSummary(liabilityCalculationModel) shouldBe expectedCalculationSummary
        }
      }


      "create a full CalculationSummary with taxDue as totalIncomeTaxAndNicsAndCgt" when {
        "the 'totalIncomeTaxAndNicsAndCgt' field is available" in {
          val taxDue = 6000
          val expectedCalculationSummary = CalculationSummary(
            timestamp = Some("2019-02-15T09:35:15.094Z".toZonedDateTime.toLocalDate),
            crystallised = true,
            unattendedCalc = false,
            taxDue = taxDue,
            income = 12500,
            deductions = 12500,
            totalTaxableIncome = 12500,
            forecastIncome = Some(12500),
            forecastIncomeTaxAndNics = Some(5000.99),
            forecastAllowancesAndDeductions = Some(4200),
            forecastTotalTaxableIncome = Some(8300),
            periodFrom = LocalDate.of(2018, 1, 1),
            periodTo = LocalDate.of(2019, 1, 1),
            messages = Some(Messages(
              info = Some(Seq(Message(id = "C22211", text = "info msg text1"))),
              warnings = Some(Seq(Message(id = "C22214", text = "warn msg text1"))),
              errors = Some(Seq(Message(id = "C22216", text = "error msg text1")))
            ))
          )

          val liabilityCalculationModel = liabilityCalculationModelSuccessful.copy(
            calculation = Some(liabilityCalculationModelSuccessful.calculation.get.copy(
              taxCalculation = Some(liabilityCalculationModelSuccessful.calculation.get.taxCalculation.get.copy(
                totalIncomeTaxAndNicsAndCgt = Some(taxDue))))))

          CalculationSummary(liabilityCalculationModel) shouldBe expectedCalculationSummary
        }
      }
    }

    "error in tax calculation" should {
      "create a CalculationSummary with multiple error message" in {
        CalculationSummary(liabilityCalculationModelErrorMessagesForIndividual) shouldBe
          CalculationSummary(
            timestamp = None,
            crystallised = false,
            unattendedCalc = false,
            taxDue = 0.0,
            income = 0,
            deductions = 0.0,
            totalTaxableIncome = 0,
            forecastIncome = None,
            forecastIncomeTaxAndNics = None,
            forecastAllowancesAndDeductions = None,
            periodFrom = LocalDate.of(2022, 1, 1),
            periodTo = LocalDate.of(2023, 1, 1),
            messages = Some(Messages(
              errors = Some(List(
                Message("C55012", "the update must align to the accounting period end date of 05/01/2023."),
                Message("C15507", "you’ve claimed £2000 in Property Income Allowance but this is more than turnover for your UK property."),
                Message("C15510", "the Rent a Room relief claimed for a jointly let property cannot be more than 10% of the Rent a Room limit."),
                Message("C55009", "updates cannot include gaps.")
              ))
            ))
          )
      }
    }

    "return unattendedCalc as true when calculationReason is 'unattendedCalculation'" in {
      CalculationSummary(liabilityCalculationModelDeductionsMinimal(calculationReason = "unattendedCalculation")) shouldBe
        CalculationSummary(
          timestamp = None,
          crystallised = true,
          unattendedCalc = true,
          taxDue = 0.0,
          income = 0,
          deductions = 0.0,
          totalTaxableIncome = 0,
          periodFrom = LocalDate.of(2022, 1, 1),
          periodTo = LocalDate.of(2023, 1, 1)
        )
    }
  }
}
