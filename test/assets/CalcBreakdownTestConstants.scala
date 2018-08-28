/*
 * Copyright 2018 HM Revenue & Customs
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

package assets

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus}
import enums.{Crystallised, Estimate}
import play.api.libs.json.{JsValue, Json}
import assets.EstimatesTestConstants._
import models.calculation._

object CalcBreakdownTestConstants {

  val calculationDataSuccessModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 90500.00,
    totalTaxableIncome = 198500.00,
    personalAllowance = 11500.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 12005.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 200000.00,
      ukProperty = 10000.00,
      bankBuildingSocietyInterest = 1999.00,
      ukDividends = 10000.00
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 20000.00,
        taxRate = 20.0,
        taxAmount = 4000.00
      ),
      higherBand = BandModel(
        taxableIncome = 100000.00,
        taxRate = 40.0,
        taxAmount = 40000.00
      ),
      additionalBand = BandModel(
        taxableIncome = 50000.00,
        taxRate = 45.0,
        taxAmount = 22500.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 1.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 20.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 5000.0,
      basicBand = BandModel(
        taxableIncome = 1000.0,
        taxRate = 7.5,
        taxAmount = 75.0
      ),
      higherBand = BandModel(
        taxableIncome = 2000.0,
        taxRate = 37.5,
        taxAmount = 750.0
      ),
      additionalBand = BandModel(
        taxableIncome = 3000.0,
        taxRate = 38.1,
        taxAmount = 1143.0
      )
    ),
    nic = NicModel(
      class2 = 10000.00,
      class4 = 14000.00
    ),
    eoyEstimate = Some(EoyEstimate(66000.00))
  )

  val noTaxOrNICalcDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 0.00,
    totalTaxableIncome = 0.00,
    personalAllowance = 2868.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 500.00,
      ukProperty = 500.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 20.0,
        taxAmount = 0.00
      ),
      higherBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 40.0,
        taxAmount = 0.00
      ),
      additionalBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 45.0,
        taxAmount = 0.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 0.00,
      class4 = 0.00
    )
  )

  val noTaxJustNICalcDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 37.05,
    totalTaxableIncome = 0.00,
    personalAllowance = 2868.00,
    taxReliefs = 10.05,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 1506.25,
      ukProperty = 0.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 20.0,
        taxAmount = 0.00
      ),
      higherBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 40.0,
        taxAmount = 0.00
      ),
      additionalBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 45.0,
        taxAmount = 0.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 20.05,
      class4 = 17.05
    )
  )


  val busPropBRTCalcDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 149.86,
    totalTaxableIncome = 132.00,
    personalAllowance = 2868.00,
    taxReliefs=24.90,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 1500.00,
      ukProperty = 1500.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 132.00,
        taxRate = 20.0,
        taxAmount = 26.00
      ),
      higherBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 40.0,
        taxAmount = 0.00
      ),
      additionalBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 45.0,
        taxAmount = 0.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 110,
      class4 = 13.86
    ),
    eoyEstimate = Some(EoyEstimate(66000.00))
  )

  val busBropHRTCalcDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 13727.71,
    totalTaxableIncome = 35007.00,
    personalAllowance = 2868.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 30000.00,
      ukProperty = 7875.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 8352.00,
        taxRate = 20.0,
        taxAmount = 1670.00
      ),
      higherBand = BandModel(
        taxableIncome = 26654.00,
        taxRate = 40.0,
        taxAmount = 10661.00
      ),
      additionalBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 45.0,
        taxAmount = 0.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 500.71,
      class4 = 896.00
    )
  )

  val busPropARTCalcDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 15017.71,
    totalTaxableIncome = 38007.00,
    personalAllowance = 2868.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 875.00,
      ukProperty = 40000.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 8352.00,
        taxRate = 20.0,
        taxAmount = 1670.00
      ),
      higherBand = BandModel(
        taxableIncome = 29044.00,
        taxRate = 40.0,
        taxAmount = 11617.00
      ),
      additionalBand = BandModel(
        taxableIncome = 609.00,
        taxRate = 45.0,
        taxAmount = 274.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 1000,
      class4 = 456.71
    )
  )

  val dividendAtBRT = CalculationDataModel(
    totalIncomeTaxNicYtd = 90500.00,
    totalTaxableIncome = 198500.00,
    personalAllowance = 11500.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 11500.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 200000.00,
      ukProperty = 10000.00,
      bankBuildingSocietyInterest = 1999.00,
      ukDividends = 10000.00
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 20000.00,
        taxRate = 20.0,
        taxAmount = 4000.00
      ),
      higherBand = BandModel(
        taxableIncome = 100000.00,
        taxRate = 40.0,
        taxAmount = 40000.00
      ),
      additionalBand = BandModel(
        taxableIncome = 50000.00,
        taxRate = 45.0,
        taxAmount = 22500.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 1.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 20.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 5000.0,
      basicBand = BandModel(
        taxableIncome = 1000.0,
        taxRate = 7.5,
        taxAmount = 75.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 10000.00,
      class4 = 14000.00
    ),
    eoyEstimate = Some(EoyEstimate(66000.00))
  )

  val dividendAtHRT = CalculationDataModel(
    totalIncomeTaxNicYtd = 90500.00,
    totalTaxableIncome = 198500.00,
    personalAllowance = 11500.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 11500.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 200000.00,
      ukProperty = 10000.00,
      bankBuildingSocietyInterest = 1999.00,
      ukDividends = 10000.00
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 20000.00,
        taxRate = 20.0,
        taxAmount = 4000.00
      ),
      higherBand = BandModel(
        taxableIncome = 100000.00,
        taxRate = 40.0,
        taxAmount = 40000.00
      ),
      additionalBand = BandModel(
        taxableIncome = 50000.00,
        taxRate = 45.0,
        taxAmount = 22500.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 1.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 20.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 5000.0,
      basicBand = BandModel(
        taxableIncome = 1000.0,
        taxRate = 7.5,
        taxAmount = 75.0
      ),
      higherBand = BandModel(
        taxableIncome = 2000.0,
        taxRate = 37.5,
        taxAmount = 750.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 10000.00,
      class4 = 14000.00
    ),
    eoyEstimate = Some(EoyEstimate(66000.00))
  )

  val dividendAtART = CalculationDataModel(
    totalIncomeTaxNicYtd = 90500.00,
    totalTaxableIncome = 198500.00,
    personalAllowance = 11500.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 11500.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 200000.00,
      ukProperty = 10000.00,
      bankBuildingSocietyInterest = 1999.00,
      ukDividends = 10000.00
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 20000.00,
        taxRate = 20.0,
        taxAmount = 4000.00
      ),
      higherBand = BandModel(
        taxableIncome = 100000.00,
        taxRate = 40.0,
        taxAmount = 40000.00
      ),
      additionalBand = BandModel(
        taxableIncome = 50000.00,
        taxRate = 45.0,
        taxAmount = 22500.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 1.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 20.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 5000.0,
      basicBand = BandModel(
        taxableIncome = 1000.0,
        taxRate = 7.5,
        taxAmount = 75.0
      ),
      higherBand = BandModel(
        taxableIncome = 2000.0,
        taxRate = 37.5,
        taxAmount = 750.0
      ),
      additionalBand = BandModel(
        taxableIncome = 3000.0,
        taxRate = 38.1,
        taxAmount = 1143.0
      )
    ),
    nic = NicModel(
      class2 = 10000.00,
      class4 = 14000.00
    ),
    eoyEstimate = Some(EoyEstimate(66000.00))
  )

  val justBusinessCalcDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 149.86,
    totalTaxableIncome = 132.00,
    personalAllowance = 2868.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 3000.00,
      ukProperty = 0.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 132.00,
        taxRate = 20.0,
        taxAmount = 26.00
      ),
      higherBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 40.0,
        taxAmount = 0.00
      ),
      additionalBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 45.0,
        taxAmount = 0.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 100.0,
      class4 = 23.86
    )
  )

  val justPropertyCalcDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 149.86,
    totalTaxableIncome = 132.00,
    personalAllowance = 2868.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 0.00,
      ukProperty = 3000.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 132.00,
        taxRate = 20.0,
        taxAmount = 26.00
      ),
      higherBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 40.0,
        taxAmount = 0.00
      ),
      additionalBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 45.0,
        taxAmount = 0.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 100.0,
      class4 = 23.86
    )
  )

  val justPropertyWithSavingsCalcDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 149.86,
    totalTaxableIncome = 132.00,
    personalAllowance = 2868.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 0.00,
      ukProperty = 3000.00,
      bankBuildingSocietyInterest = 2500.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 132.00,
        taxRate = 20.0,
        taxAmount = 26.00
      ),
      higherBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 40.0,
        taxAmount = 0.00
      ),
      additionalBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 45.0,
        taxAmount = 0.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 2500.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 100.0,
      class4 = 23.86
    )
  )

  val mandatoryOnlyDataModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 90500.0,
    totalTaxableIncome = 0.00,
    personalAllowance = 0.00,
    taxReliefs = 0,
    totalIncomeAllowancesUsed = 0.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 0.00,
      ukProperty = 0.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    payPensionsProfit = PayPensionsProfitModel(
      basicBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.00
      ),
      higherBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.00
      ),
      additionalBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.00
      )
    ),
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      zeroBand = BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    dividends = DividendsModel(
      allowance = 0.0,
      basicBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      higherBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      ),
      additionalBand = BandModel(
        taxableIncome = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0
      )
    ),
    nic = NicModel(
      class2 = 0.0,
      class4 = 0.0
    )
  )



  val mandatoryCalculationDataSuccessJson: JsValue = Json.obj(
    "incomeTaxYTD" -> 90500,
    "incomeTaxThisPeriod" -> 2000
  )

  val calculationDataSuccessJson: JsValue = Json.obj(
    "totalTaxableIncome" -> 198500,
    "totalIncomeTaxNicYtd" -> 90500,
    "personalAllowance" -> 11500,
    "taxReliefs" -> 0,
    "totalIncomeAllowancesUsed" -> 12005,
    "incomeReceived" -> Json.obj(
      "selfEmployment" -> 200000,
      "ukProperty" -> 10000,
      "bankBuildingSocietyInterest" -> 1999,
      "ukDividends" -> 10000
    ),
     "payPensionsProfit" -> Json.obj(
       "basicBand" -> Json.obj(
         "taxableIncome" -> 20000,
         "taxRate" -> 20,
         "taxAmount" -> 4000
       ),
       "higherBand" -> Json.obj(
         "taxableIncome" -> 100000,
         "taxRate" -> 40,
         "taxAmount" -> 40000
       ),
       "additionalBand" -> Json.obj(
         "taxableIncome" -> 50000,
         "taxRate" -> 45,
         "taxAmount" -> 22500
       )
     ),
     "savingsAndGains" -> Json.obj(
       "startBand" -> Json.obj(
         "taxableIncome" -> 1.00,
         "taxRate" -> 0,
         "taxAmount" -> 0
       ),
       "zeroBand" -> Json.obj(
         "taxableIncome" -> 20.00,
         "taxRate" -> 0,
         "taxAmount" -> 0
       ),
       "basicBand" -> Json.obj(
         "taxableIncome" -> 0,
         "taxRate" -> 20,
         "taxAmount" -> 0
       ),
       "higherBand" -> Json.obj(
         "taxableIncome" -> 0,
         "taxRate" -> 40,
         "taxAmount" -> 0
       ),
       "additionalBand" -> Json.obj(
         "taxableIncome" -> 0,
         "taxRate" -> 45,
         "taxAmount" -> 0
       )
     ),
     "dividends" -> Json.obj(
       "allowance" -> 5000,
       "basicBand" -> Json.obj(
         "taxableIncome" -> 1000,
         "taxRate" -> 7.5,
         "taxAmount" -> 75
       ),
       "higherBand" -> Json.obj(
         "taxableIncome" -> 2000,
         "taxRate" -> 37.5,
         "taxAmount" -> 750
       ),
       "additionalBand" -> Json.obj(
         "taxableIncome" -> 3000,
         "taxRate" -> 38.1,
         "taxAmount" -> 1143
       )
     ),
     "nic" -> Json.obj(
       "class2" -> 10000,
       "class4" -> 14000
     ),
     "eoyEstimate" -> Json.obj(
       "incomeTaxNicAmount" -> 66000
     )
  )


  val calculationDataFullJson: JsValue = Json.obj(
   "incomeTaxYTD" -> 90500,
   "incomeTaxThisPeriod" -> 20,
   "payFromAllEmployments" -> 0,
   "totalIncomeAllowancesUsed" -> 12005,
   "benefitsAndExpensesReceived" -> 0,
   "allowableExpenses" -> 0,
   "payFromAllEmploymentsAfterExpenses" -> 0,
   "shareSchemes" -> 0,
   "profitFromSelfEmployment" -> 200000,
   "profitFromPartnerships" -> 0,
   "profitFromUkLandAndProperty" -> 10000,
   "dividendsFromForeignCompanies" -> 0,
   "foreignIncome" -> 0,
   "trustsAndEstates" -> 0,
   "interestReceivedFromUkBanksAndBuildingSocieties" -> 1999,
   "dividendsFromUkCompanies" -> 10000,
   "ukPensionsAndStateBenefits" -> 0,
   "gainsOnLifeInsurance" -> 0,
   "otherIncome" -> 0,
   "totalIncomeReceived" -> 230000,
   "paymentsIntoARetirementAnnuity" -> 0,
   "foreignTaxOnEstates" -> 0,
   "incomeTaxRelief" -> 0,
   "incomeTaxReliefReducedToMaximumAllowable" -> 0,
   "annuities" -> 0,
   "giftOfInvestmentsAndPropertyToCharity" -> 0,
   "personalAllowance" -> 11500,
   "marriageAllowanceTransfer" -> 0,
   "blindPersonAllowance" -> 0,
   "blindPersonSurplusAllowanceFromSpouse" -> 0,
   "incomeExcluded" -> 0,
   "totalIncomeOnWhichTaxIsDue" -> 198500,
   "payPensionsExtender" -> 0,
   "giftExtender" -> 0,
   "extendedBR" -> 0,
   "payPensionsProfitAtBRT" -> 20000,
   "incomeTaxOnPayPensionsProfitAtBRT" -> 4000,
   "payPensionsProfitAtHRT" -> 100000,
   "incomeTaxOnPayPensionsProfitAtHRT" -> 40000,
   "payPensionsProfitAtART" -> 50000,
   "incomeTaxOnPayPensionsProfitAtART" -> 22500,
   "netPropertyFinanceCosts" -> 0,
   "interestReceivedAtStartingRate" -> 1,
   "incomeTaxOnInterestReceivedAtStartingRate" -> 0,
   "interestReceivedAtZeroRate" -> 20,
   "incomeTaxOnInterestReceivedAtZeroRate" -> 0,
   "interestReceivedAtBRT" -> 0,
   "incomeTaxOnInterestReceivedAtBRT" -> 0,
   "interestReceivedAtHRT" -> 0,
   "incomeTaxOnInterestReceivedAtHRT" -> 0,
   "interestReceivedAtART" -> 0,
   "incomeTaxOnInterestReceivedAtART" -> 0,
   "dividendsAtZeroRate" -> 0,
   "incomeTaxOnDividendsAtZeroRate" -> 0,
   "dividendsAtBRT" -> 1000,
   "incomeTaxOnDividendsAtBRT" -> 75,
   "dividendsAtHRT" -> 2000,
   "incomeTaxOnDividendsAtHRT" -> 750,
   "dividendsAtART" -> 3000,
   "incomeTaxOnDividendsAtART" -> 1143,
   "totalIncomeOnWhichTaxHasBeenCharged" -> 0,
   "taxOnOtherIncome" -> 0,
   "incomeTaxDue" -> 66500,
   "incomeTaxCharged" -> 0,
   "deficiencyRelief" -> 0,
   "topSlicingRelief" -> 0,
   "ventureCapitalTrustRelief" -> 0,
   "enterpriseInvestmentSchemeRelief" -> 0,
   "seedEnterpriseInvestmentSchemeRelief" -> 0,
   "communityInvestmentTaxRelief" -> 0,
   "socialInvestmentTaxRelief" -> 0,
   "maintenanceAndAlimonyPaid" -> 0,
   "marriedCouplesAllowance" -> 0,
   "marriedCouplesAllowanceRelief" -> 0,
   "surplusMarriedCouplesAllowance" -> 0,
   "surplusMarriedCouplesAllowanceRelief" -> 0,
   "notionalTaxFromLifePolicies" -> 0,
   "notionalTaxFromDividendsAndOtherIncome" -> 0,
   "foreignTaxCreditRelief" -> 0,
   "incomeTaxDueAfterAllowancesAndReliefs" -> 0,
   "giftAidPaymentsAmount" -> 0,
   "giftAidTaxDue" -> 0,
   "capitalGainsTaxDue" -> 0,
   "remittanceForNonDomiciles" -> 0,
   "highIncomeChildBenefitCharge" -> 0,
   "totalGiftAidTaxReduced" -> 0,
   "incomeTaxDueAfterGiftAidReduction" -> 0,
   "annuityAmount" -> 0,
   "taxDueOnAnnuity" -> 0,
   "taxCreditsOnDividendsFromUkCompanies" -> 0,
   "incomeTaxDueAfterDividendTaxCredits" -> 0,
   "nationalInsuranceContributionAmount" -> 0,
   "nationalInsuranceContributionCharge" -> 0,
   "nationalInsuranceContributionSupAmount" -> 0,
   "nationalInsuranceContributionSupCharge" -> 0,
   "totalClass4Charge" -> 14000,
   "nationalInsuranceClass1Amount" -> 0,
   "nationalInsuranceClass2Amount" -> 10000,
   "nicTotal" -> 24000,
   "underpaidTaxForPreviousYears" -> 0,
   "studentLoanRepayments" -> 0,
   "pensionChargesGross" -> 0,
   "pensionChargesTaxPaid" -> 0,
   "totalPensionSavingCharges" -> 0,
   "pensionLumpSumAmount" -> 0,
   "pensionLumpSumRate" -> 0,
   "statePensionLumpSumAmount" -> 0,
   "remittanceBasisChargeForNonDomiciles" -> 0,
   "additionalTaxDueOnPensions" -> 0,
   "additionalTaxReliefDueOnPensions" -> 0,
   "incomeTaxDueAfterPensionDeductions" -> 0,
   "employmentsPensionsAndBenefits" -> 0,
   "outstandingDebtCollectedThroughPaye" -> 0,
   "payeTaxBalance" -> 0,
   "cisAndTradingIncome" -> 0,
   "partnerships" -> 0,
   "ukLandAndPropertyTaxPaid" -> 0,
   "foreignIncomeTaxPaid" -> 0,
   "trustAndEstatesTaxPaid" -> 0,
   "overseasIncomeTaxPaid" -> 0,
   "interestReceivedTaxPaid" -> 0,
   "voidISAs" -> 0,
   "otherIncomeTaxPaid" -> 0,
   "underpaidTaxForPriorYear" -> 0,
   "totalTaxDeducted" -> 0,
   "incomeTaxOverpaid" -> 0,
   "incomeTaxDueAfterDeductions" -> 0,
   "propertyFinanceTaxDeduction" -> 0,
   "taxableCapitalGains" -> 0,
   "capitalGainAtEntrepreneurRate" -> 0,
   "incomeTaxOnCapitalGainAtEntrepreneurRate" -> 0,
   "capitalGrainsAtLowerRate" -> 0,
   "incomeTaxOnCapitalGainAtLowerRate" -> 0,
   "capitalGainAtHigherRate" -> 0,
   "incomeTaxOnCapitalGainAtHigherTax" -> 0,
   "capitalGainsTaxAdjustment" -> 0,
   "foreignTaxCreditReliefOnCapitalGains" -> 0,
   "liabilityFromOffShoreTrusts" -> 0,
   "taxOnGainsAlreadyCharged" -> 0,
   "totalCapitalGainsTax" -> 0,
   "incomeAndCapitalGainsTaxDue" -> 0,
   "taxRefundedInYear" -> 0,
   "unpaidTaxCalculatedForEarlierYears" -> 0,
   "marriageAllowanceTransferAmount" -> 0,
   "marriageAllowanceTransferRelief" -> 0,
   "marriageAllowanceTransferMaximumAllowable" -> 0,
   "nationalRegime" -> "0",
   "allowance" -> 0,
   "limitBRT" -> 0,
   "limitHRT" -> 0,
   "rateBRT" -> 20,
   "rateHRT" -> 40,
   "rateART" -> 45,
   "limitAIA" -> 0,
   "limitAIA" -> 0,
   "allowanceBRT" -> 0,
   "interestAllowanceHRT" -> 0,
   "interestAllowanceBRT" -> 0,
   "dividendAllowance" -> 5000,
   "dividendBRT" -> 7.5,
   "dividendHRT" -> 37.5,
   "dividendART" -> 38.1,
   "class2NICsLimit" -> 0,
   "class2NICsPerWeek" -> 0,
   "class4NICsLimitBR" -> 0,
   "class4NICsLimitHR" -> 0,
   "class4NICsBRT" -> 0,
   "class4NICsHRT" -> 0,
   "proportionAllowance" -> 11500,
   "proportionLimitBRT" -> 0,
   "proportionLimitHRT" -> 0,
   "proportionalTaxDue" -> 0,
   "proportionInterestAllowanceBRT" -> 0,
   "proportionInterestAllowanceHRT" -> 0,
   "proportionDividendAllowance" -> 0,
   "proportionPayPensionsProfitAtART" -> 0,
   "proportionIncomeTaxOnPayPensionsProfitAtART" -> 0,
   "proportionPayPensionsProfitAtBRT" -> 0,
   "proportionIncomeTaxOnPayPensionsProfitAtBRT" -> 0,
   "proportionPayPensionsProfitAtHRT" -> 0,
   "proportionIncomeTaxOnPayPensionsProfitAtHRT" -> 0,
   "proportionInterestReceivedAtZeroRate" -> 0,
   "proportionIncomeTaxOnInterestReceivedAtZeroRate" -> 0,
   "proportionInterestReceivedAtBRT" -> 0,
   "proportionIncomeTaxOnInterestReceivedAtBRT" -> 0,
   "proportionInterestReceivedAtHRT" -> 0,
   "proportionIncomeTaxOnInterestReceivedAtHRT" -> 0,
   "proportionInterestReceivedAtART" -> 0,
   "proportionIncomeTaxOnInterestReceivedAtART" -> 0,
   "proportionDividendsAtZeroRate" -> 0,
   "proportionIncomeTaxOnDividendsAtZeroRate" -> 0,
   "proportionDividendsAtBRT" -> 0,
   "proportionIncomeTaxOnDividendsAtBRT" -> 0,
   "proportionDividendsAtHRT" -> 0,
   "proportionIncomeTaxOnDividendsAtHRT" -> 0,
   "proportionDividendsAtART" -> 0,
   "proportionIncomeTaxOnDividendsAtART" -> 0,
   "proportionClass2NICsLimit" -> 0,
   "proportionClass4NICsLimitBR" -> 0,
   "proportionClass4NICsLimitHR" -> 0,
   "proportionReducedAllowanceLimit" -> 0,
   "eoyEstimate" -> Json.obj(
     "selfEmployment" -> Json.arr(
       Json.obj(
         "id" -> "selfEmploymentId1",
         "taxableIncome" -> 89999999.99,
         "supplied" -> true,
         "finalised" -> true
       ),
       Json.obj(
         "id" -> "selfEmploymentId2",
         "taxableIncome" -> 89999999.99,
         "supplied" -> true,
         "finalised" -> true
       )
     ),
     "ukProperty" -> Json.arr(
       Json.obj(
         "taxableIncome" -> 89999999.99,
         "supplied" -> true,
         "finalised" -> true
       )
     ),
     "totalTaxableIncome" -> 89999999.99,
     "incomeTaxAmount" -> 89999999.99,
     "nic2" -> 89999999.99,
     "nic4" -> 89999999.99,
     "totalNicAmount" -> 9999999.99,
     "incomeTaxNicAmount" -> 66000.00
   )
  )


  val calculationDataSuccessMinJson: JsValue = Json.obj()


  val calculationDataErrorModel: CalculationDataErrorModel = CalculationDataErrorModel(testErrorStatus, testErrorMessage)
  val calculationDataErrorJson: JsValue =
     Json.obj(
       "code" ->testErrorStatus,
       "message" ->testErrorMessage
     )

  val calculationDisplaySuccessModel: CalculationDataModel => CalcDisplayModel = calcModel =>
    CalcDisplayModel(
      lastTaxCalcSuccess.calcTimestamp,
      lastTaxCalcSuccess.calcAmount,
      Some(calcModel),
      Estimate
    )

  val calculationDisplaySuccessCrystalisationModel: CalculationDataModel => CalcDisplayModel = calcModel =>
    CalcDisplayModel(
      lastTaxCalcSuccess.calcTimestamp,
      lastTaxCalcSuccess.calcAmount,
      Some(calcModel),
      Crystallised
    )

  val calculationDisplayNoBreakdownModel =
    CalcDisplayModel(
      lastTaxCalcSuccess.calcTimestamp,
      lastTaxCalcSuccess.calcAmount,
      None,
      Estimate
    )

  val testCalcDisplayModel: CalcDisplayModel =
    CalcDisplayModel(
      lastTaxCalcSuccess.calcTimestamp,
      lastTaxCalcSuccess.calcAmount,
      Some(calculationDataSuccessModel),
      Estimate
    )
}
