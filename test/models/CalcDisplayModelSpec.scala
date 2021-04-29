/*
 * Copyright 2021 HM Revenue & Customs
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

package models

import assets.CalcBreakdownTestConstants
import enums.Estimate
import models.calculation.{CalcDisplayModel, Calculation, Dividends, GainsOnLifePolicies, LumpSums, PayPensionsProfit, SavingsAndGains, TaxBand}
import org.scalatest.Matchers
import testUtils.TestSupport

class CalcDisplayModelSpec extends TestSupport with Matchers {

  val calculation = Calculation(crystallised = false)

  "whatYouOwe" should {

    "display the income tax total" when {

      "calculation data exists" in {
        CalcDisplayModel("", 149.86, CalcBreakdownTestConstants.justBusinessCalcDataModel, Estimate).whatYouOwe shouldBe "&pound;149.86"
      }
    }

    "display the calculation amount" when {

      "calculation data does not exist" in {
        CalcDisplayModel("", 2, calculation, Estimate).whatYouOwe shouldBe "&pound;2.00"
      }
    }
  }

	"getModifiedBaseTaxBand" should {

		"return the pay/pensions base band when present" in {
			CalcDisplayModel("", 0.0, CalcBreakdownTestConstants.justBusinessCalcDataModel, Estimate).getModifiedBaseTaxBand shouldBe Some(
				TaxBand(
					name = "BRT",
					rate = 20.0,
					income = 132.00,
					taxAmount = 26.00,
					bandLimit = 15000,
					apportionedBandLimit = 15000
				)
			)
		}

		"return the savings base band when present without pay/pensions" in {
			CalcDisplayModel("", 0.0,
				CalcBreakdownTestConstants.justBusinessCalcDataModel.copy(payPensionsProfit = PayPensionsProfit()),
				Estimate).getModifiedBaseTaxBand shouldBe Some(
				TaxBand(
					name = "BRT",
					rate = 20.0,
					income = 0.0,
					taxAmount = 0.0,
					bandLimit = 15000,
					apportionedBandLimit = 15000
				)
			)
		}

		"return the dividends base band when present without pay/pensions or savings" in {
			CalcDisplayModel("", 0.0,
				CalcBreakdownTestConstants.justBusinessCalcDataModel.copy(
					payPensionsProfit = PayPensionsProfit(),
					savingsAndGains = SavingsAndGains()
				), Estimate).getModifiedBaseTaxBand shouldBe Some(
				TaxBand(
					name = "BRT",
					rate = 7.5,
					income = 1000,
					taxAmount = 75.0,
					bandLimit = 15000,
					apportionedBandLimit = 15000)
			)
		}

		"return the lump sums base band when present without pay/pensions, savings or dividends" in {
			CalcDisplayModel("", 0.0,
				CalcBreakdownTestConstants.testCalcModelCrystallised.copy(
					payPensionsProfit = PayPensionsProfit(),
					savingsAndGains = SavingsAndGains(),
					dividends = Dividends()
				), Estimate).getModifiedBaseTaxBand shouldBe Some(
				TaxBand(
					name = "BRT",
					rate = 20.0,
					income = 20000.00,
					taxAmount = 4000.00,
					bandLimit = 15000,
					apportionedBandLimit = 15000)
			)
		}

		"return the gains on life policies base band when present without pay/pensions, savings, dividends or lump sums" in {
			CalcDisplayModel("", 0.0,
				CalcBreakdownTestConstants.testCalcModelCrystallised.copy(
					payPensionsProfit = PayPensionsProfit(),
					savingsAndGains = SavingsAndGains(),
					dividends = Dividends(),
					lumpSums = LumpSums()
				), Estimate).getModifiedBaseTaxBand shouldBe Some(
				TaxBand(
					name = "BRT",
					rate = 20.0,
					income = 20000.00,
					taxAmount = 4000.00,
					bandLimit = 14000,
					apportionedBandLimit = 14000)
			)
		}

		"return None when no base rate tax bands can be found" in {
			CalcDisplayModel("", 0.0,
				CalcBreakdownTestConstants.testCalcModelCrystallised.copy(
					payPensionsProfit = PayPensionsProfit(),
					savingsAndGains = SavingsAndGains(),
					dividends = Dividends(),
					lumpSums = LumpSums(),
					gainsOnLifePolicies = GainsOnLifePolicies()
				), Estimate).getModifiedBaseTaxBand shouldBe None
		}
	}
}
