/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.EstimatesTestConstants._
import enums.{Crystallised, Estimate}
import models.calculation._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}

object CalcBreakdownTestConstants {

  val calculationDataSuccessModel = CalculationDataModel(
    nationalRegime = Some("UK"),
    totalIncomeTaxNicYtd = 90500.00,
    totalTaxableIncome = 198500.00,
    personalAllowance = 11500.00,
    taxReliefs = 0.00,
    totalIncomeAllowancesUsed = 12005.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 200000.00,
      ukProperty = 10000.00,
      bankBuildingSocietyInterest = 1999.00,
      ukDividends = 10000.00
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
    eoyEstimate = Some(EoyEstimate(66000.00)),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 20000.00, 4000.00),
      TaxBandModel("HRT", 40.0, 100000.00, 40000.00),
      TaxBandModel("ART", 45.0, 50000.00, 22500.00)
    )
  )

  val scottishBandModelJustSRT = CalculationDataModel(
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
    savingsAndGains = SavingsAndGainsModel(
      startBand = BandModel(
        taxableIncome = 1000.00,
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
    eoyEstimate = Some(EoyEstimate(66000.00)),
    payAndPensionsProfitBands = List(
      TaxBandModel("SRT", 15.0, 5000.00, 750.00),
      TaxBandModel("BRT", 20.0, 0.00, 0.00),
      TaxBandModel("IRT", 30.0, 0.00, 0.00),
      TaxBandModel("HRT", 40.0, 0.00, 0.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
    )
  )

  val scottishBandModelIRT = CalculationDataModel(
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
    payAndPensionsProfitBands = List(
      TaxBandModel("SRT", 15.0, 5000.00, 750.00),
      TaxBandModel("BRT", 20.0, 10000.00, 2000.00),
      TaxBandModel("IRT", 30.0, 20000.00, 6000.00),
      TaxBandModel("HRT", 40.0, 0.00, 0.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
    )
  )

  val scottishBandModelAllIncomeBands = CalculationDataModel(
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
    payAndPensionsProfitBands = List(
      TaxBandModel("SRT", 15.0, 5000.00, 750.00),
      TaxBandModel("BRT", 20.0, 10000.00, 2000.00),
      TaxBandModel("IRT", 30.0, 20000.00, 6000.00),
      TaxBandModel("HRT", 40.0, 50000.00, 20000.00),
      TaxBandModel("ART", 45.0, 10000.00, 4500.00)
    )
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
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 0.00, 0.00),
      TaxBandModel("HRT", 40.0, 0.00, 0.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
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
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 0.00, 0.00),
      TaxBandModel("HRT", 40.0, 0.00, 0.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
    )
  )

  val busPropBRTCalcDataModel = CalculationDataModel(
    nationalRegime = Some("Scotland"),
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
    eoyEstimate = Some(EoyEstimate(66000.00)),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 132.00, 26.00),
      TaxBandModel("HRT", 40.0, 0.00, 0.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
    )
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
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 8352.00, 1670.00),
      TaxBandModel("HRT", 40.0, 26654.00, 10661.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
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
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 8352.00, 1670.00),
      TaxBandModel("HRT", 40.0, 29044.00, 11617.00),
      TaxBandModel("ART", 45.0, 609.00, 274.00)
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
    eoyEstimate = Some(EoyEstimate(66000.00)),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 20000.00, 4000.00),
      TaxBandModel("HRT", 40.0, 100000.00, 40000.00),
      TaxBandModel("ART", 45.0, 50000.00, 22500.00)
    )
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
    eoyEstimate = Some(EoyEstimate(66000.00)),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 20000.00, 4000.00),
      TaxBandModel("HRT", 40.0, 100000.00, 40000.00),
      TaxBandModel("ART", 45.0, 50000.00, 22500.00)
    )
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
    eoyEstimate = Some(EoyEstimate(66000.00)),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 20000.00, 4000.00),
      TaxBandModel("HRT", 40.0, 100000.00, 40000.00),
      TaxBandModel("ART", 45.0, 50000.00, 22500.00)
    )
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
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 132.00, 26.00),
      TaxBandModel("HRT", 40.0, 0.00, 0.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
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
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 132.00, 26.00),
      TaxBandModel("HRT", 40.0, 0.00, 0.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
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
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 132.00, 26.00),
      TaxBandModel("HRT", 40.0, 0.00, 0.00),
      TaxBandModel("ART", 45.0, 0.00, 0.00)
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
    ),
    payAndPensionsProfitBands = Nil
  )

  val mandatoryCalculationDataSuccessJson: JsValue = Json.obj(
    "incomeTaxYTD" -> 90500,
    "incomeTaxThisPeriod" -> 2000
  )

  val calculationDataSuccessJson: JsValue = Json.obj(
    "nationalRegime" -> "UK",
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
    ),
    "incomeTax" -> Json.obj(
      "payAndPensionsProfit" -> Json.obj(
        "band" -> Json.arr(
          Json.obj(
            "name" -> "BRT",
            "rate" -> 20.0,
            "income" -> 20000.00,
            "amount" -> 4000.00
          ),
          Json.obj(
            "name" -> "HRT",
            "rate" -> 40.0,
            "income" -> 100000.00,
            "amount" -> 40000.00
          ),
          Json.obj(
            "name" -> "ART",
            "rate" -> 45.0,
            "income" -> 50000.00,
            "amount" -> 22500.00
          )
        )
      )
    )
  )

  val calculationDataFullJson: JsValue = Json.obj(
    "nationalRegime" -> "UK",
    "incomeTaxYTD" -> 90500.00,
    "incomeTaxThisPeriod" -> 20.00,
    "payFromAllEmployments" -> 0.00,
    "totalIncomeAllowancesUsed" -> 12005.00,
    "benefitsAndExpensesReceived" -> 0.00,
    "allowableExpenses" -> 0.00,
    "payFromAllEmploymentsAfterExpenses" -> 0.00,
    "shareSchemes" -> 0.00,
    "profitFromSelfEmployment" -> 200000.00,
    "profitFromPartnerships" -> 0.00,
    "profitFromUkLandAndProperty" -> 10000.00,
    "dividendsFromForeignCompanies" -> 0.00,
    "foreignIncome" -> 0.00,
    "trustsAndEstates" -> 0.00,
    "interestReceivedFromUkBanksAndBuildingSocieties" -> 1999.00,
    "dividendsFromUkCompanies" -> 10000.00,
    "ukPensionsAndStateBenefits" -> 0.00,
    "gainsOnLifeInsurance" -> 0.00,
    "otherIncome" -> 0.00,
    "totalIncomeReceived" -> 230000.00,
    "paymentsIntoARetirementAnnuity" -> 0.00,
    "foreignTaxOnEstates" -> 0.00,
    "incomeTaxRelief" -> 0.00,
    "incomeTaxReliefReducedToMaximumAllowable" -> 0.00,
    "annuities" -> 0.00,
    "giftOfInvestmentsAndPropertyToCharity" -> 0.00,
    "personalAllowance" -> 11500.00,
    "marriageAllowanceTransfer" -> 0.00,
    "blindPersonAllowance" -> 0.00,
    "blindPersonSurplusAllowanceFromSpouse" -> 0.00,
    "incomeExcluded" -> 0.00,
    "totalIncomeOnWhichTaxIsDue" -> 198500.00,
    "payPensionsExtender" -> 0.00,
    "giftExtender" -> 0.00,
    "extendedBR" -> 0.00,
    "payPensionsProfitAtBRT" -> 20000.00,
    "incomeTaxOnPayPensionsProfitAtBRT" -> 4000.00,
    "payPensionsProfitAtHRT" -> 100000.00,
    "incomeTaxOnPayPensionsProfitAtHRT" -> 40000.00,
    "payPensionsProfitAtART" -> 50000.00,
    "incomeTaxOnPayPensionsProfitAtART" -> 22500.00,
    "netPropertyFinanceCosts" -> 0.00,
    "interestReceivedAtStartingRate" -> 1.00,
    "incomeTaxOnInterestReceivedAtStartingRate" -> 0.00,
    "interestReceivedAtZeroRate" -> 20.00,
    "incomeTaxOnInterestReceivedAtZeroRate" -> 0.00,
    "interestReceivedAtBRT" -> 0.00,
    "incomeTaxOnInterestReceivedAtBRT" -> 0.00,
    "interestReceivedAtHRT" -> 0.00,
    "incomeTaxOnInterestReceivedAtHRT" -> 0.00,
    "interestReceivedAtART" -> 0.00,
    "incomeTaxOnInterestReceivedAtART" -> 0.00,
    "dividendsAtZeroRate" -> 0.00,
    "incomeTaxOnDividendsAtZeroRate" -> 0.00,
    "dividendsAtBRT" -> 1000.00,
    "incomeTaxOnDividendsAtBRT" -> 75.00,
    "dividendsAtHRT" -> 2000.00,
    "incomeTaxOnDividendsAtHRT" -> 750.00,
    "dividendsAtART" -> 3000.00,
    "incomeTaxOnDividendsAtART" -> 1143.00,
    "totalIncomeOnWhichTaxHasBeenCharged" -> 0.00,
    "taxOnOtherIncome" -> 0.00,
    "incomeTaxDue" -> 66500.00,
    "incomeTaxCharged" -> 0.00,
    "deficiencyRelief" -> 0.00,
    "topSlicingRelief" -> 0.00,
    "ventureCapitalTrustRelief" -> 0.00,
    "enterpriseInvestmentSchemeRelief" -> 0.00,
    "seedEnterpriseInvestmentSchemeRelief" -> 0.00,
    "communityInvestmentTaxRelief" -> 0.00,
    "socialInvestmentTaxRelief" -> 0.00,
    "maintenanceAndAlimonyPaid" -> 0.00,
    "marriedCouplesAllowance" -> 0.00,
    "marriedCouplesAllowanceRelief" -> 0.00,
    "surplusMarriedCouplesAllowance" -> 0.00,
    "surplusMarriedCouplesAllowanceRelief" -> 0.00,
    "notionalTaxFromLifePolicies" -> 0.00,
    "notionalTaxFromDividendsAndOtherIncome" -> 0.00,
    "foreignTaxCreditRelief" -> 0.00,
    "incomeTaxDueAfterAllowancesAndReliefs" -> 0.00,
    "giftAidPaymentsAmount" -> 0.00,
    "giftAidTaxDue" -> 0.00,
    "capitalGainsTaxDue" -> 0.00,
    "remittanceForNonDomiciles" -> 0.00,
    "highIncomeChildBenefitCharge" -> 0.00,
    "totalGiftAidTaxReduced" -> 0.00,
    "incomeTaxDueAfterGiftAidReduction" -> 0.00,
    "annuityAmount" -> 0.00,
    "taxDueOnAnnuity" -> 0.00,
    "taxCreditsOnDividendsFromUkCompanies" -> 0.00,
    "incomeTaxDueAfterDividendTaxCredits" -> 0.00,
    "nationalInsuranceContributionAmount" -> 0.00,
    "nationalInsuranceContributionCharge" -> 0.00,
    "nationalInsuranceContributionSupAmount" -> 0.00,
    "nationalInsuranceContributionSupCharge" -> 0.00,
    "totalClass4Charge" -> 14000.00,
    "nationalInsuranceClass1Amount" -> 0.00,
    "nationalInsuranceClass2Amount" -> 10000.00,
    "nicTotal" -> 24000.00,
    "underpaidTaxForPreviousYears" -> 0.00,
    "studentLoanRepayments" -> 0.00,
    "pensionChargesGross" -> 0.00,
    "pensionChargesTaxPaid" -> 0.00,
    "totalPensionSavingCharges" -> 0.00,
    "pensionLumpSumAmount" -> 0.00,
    "pensionLumpSumRate" -> 0.00,
    "statePensionLumpSumAmount" -> 0.00,
    "remittanceBasisChargeForNonDomiciles" -> 0.00,
    "additionalTaxDueOnPensions" -> 0.00,
    "additionalTaxReliefDueOnPensions" -> 0.00,
    "incomeTaxDueAfterPensionDeductions" -> 0.00,
    "employmentsPensionsAndBenefits" -> 0.00,
    "outstandingDebtCollectedThroughPaye" -> 0.00,
    "payeTaxBalance" -> 0.00,
    "cisAndTradingIncome" -> 0.00,
    "partnerships" -> 0.00,
    "ukLandAndPropertyTaxPaid" -> 0.00,
    "foreignIncomeTaxPaid" -> 0.00,
    "trustAndEstatesTaxPaid" -> 0.00,
    "overseasIncomeTaxPaid" -> 0.00,
    "interestReceivedTaxPaid" -> 0.00,
    "voidISAs" -> 0.00,
    "otherIncomeTaxPaid" -> 0.00,
    "underpaidTaxForPriorYear" -> 0.00,
    "totalTaxDeducted" -> 0.00,
    "incomeTaxOverpaid" -> 0.00,
    "incomeTaxDueAfterDeductions" -> 0.00,
    "propertyFinanceTaxDeduction" -> 0.00,
    "taxableCapitalGains" -> 0.00,
    "capitalGainAtEntrepreneurRate" -> 0.00,
    "incomeTaxOnCapitalGainAtEntrepreneurRate" -> 0.00,
    "capitalGrainsAtLowerRate" -> 0.00,
    "incomeTaxOnCapitalGainAtLowerRate" -> 0.00,
    "capitalGainAtHigherRate" -> 0.00,
    "incomeTaxOnCapitalGainAtHigherTax" -> 0.00,
    "capitalGainsTaxAdjustment" -> 0.00,
    "foreignTaxCreditReliefOnCapitalGains" -> 0.00,
    "liabilityFromOffShoreTrusts" -> 0.00,
    "taxOnGainsAlreadyCharged" -> 0.00,
    "totalCapitalGainsTax" -> 0.00,
    "incomeAndCapitalGainsTaxDue" -> 0.00,
    "taxRefundedInYear" -> 0.00,
    "unpaidTaxCalculatedForEarlierYears" -> 0.00,
    "marriageAllowanceTransferAmount" -> 0.00,
    "marriageAllowanceTransferRelief" -> 0.00,
    "marriageAllowanceTransferMaximumAllowable" -> 0.00,
    "allowance" -> 0.00,
    "limitBRT" -> 0.00,
    "limitHRT" -> 0.00,
    "rateBRT" -> 20.00,
    "rateHRT" -> 40.00,
    "rateART" -> 45.00,
    "limitAIA" -> 0.00,
    "limitAIA" -> 0.00,
    "allowanceBRT" -> 0.00,
    "interestAllowanceHRT" -> 0.00,
    "interestAllowanceBRT" -> 0.00,
    "dividendAllowance" -> 5000.00,
    "dividendBRT" -> 7.5,
    "dividendHRT" -> 37.5,
    "dividendART" -> 38.1,
    "class2NICsLimit" -> 0.00,
    "class2NICsPerWeek" -> 0.00,
    "class4NICsLimitBR" -> 0.00,
    "class4NICsLimitHR" -> 0.00,
    "class4NICsBRT" -> 0.00,
    "class4NICsHRT" -> 0.00,
    "proportionAllowance" -> 11500.00,
    "proportionLimitBRT" -> 0.00,
    "proportionLimitHRT" -> 0.00,
    "proportionalTaxDue" -> 0.00,
    "proportionInterestAllowanceBRT" -> 0.00,
    "proportionInterestAllowanceHRT" -> 0.00,
    "proportionDividendAllowance" -> 0.00,
    "proportionPayPensionsProfitAtART" -> 0.00,
    "proportionIncomeTaxOnPayPensionsProfitAtART" -> 0.00,
    "proportionPayPensionsProfitAtBRT" -> 0.00,
    "proportionIncomeTaxOnPayPensionsProfitAtBRT" -> 0.00,
    "proportionPayPensionsProfitAtHRT" -> 0.00,
    "proportionIncomeTaxOnPayPensionsProfitAtHRT" -> 0.00,
    "proportionInterestReceivedAtZeroRate" -> 0.00,
    "proportionIncomeTaxOnInterestReceivedAtZeroRate" -> 0.00,
    "proportionInterestReceivedAtBRT" -> 0.00,
    "proportionIncomeTaxOnInterestReceivedAtBRT" -> 0.00,
    "proportionInterestReceivedAtHRT" -> 0.00,
    "proportionIncomeTaxOnInterestReceivedAtHRT" -> 0.00,
    "proportionInterestReceivedAtART" -> 0.00,
    "proportionIncomeTaxOnInterestReceivedAtART" -> 0.00,
    "proportionDividendsAtZeroRate" -> 0.00,
    "proportionIncomeTaxOnDividendsAtZeroRate" -> 0.00,
    "proportionDividendsAtBRT" -> 0.00,
    "proportionIncomeTaxOnDividendsAtBRT" -> 0.00,
    "proportionDividendsAtHRT" -> 0.00,
    "proportionIncomeTaxOnDividendsAtHRT" -> 0.00,
    "proportionDividendsAtART" -> 0.00,
    "proportionIncomeTaxOnDividendsAtART" -> 0.00,
    "proportionClass2NICsLimit" -> 0.00,
    "proportionClass4NICsLimitBR" -> 0.00,
    "proportionClass4NICsLimitHR" -> 0.00,
    "proportionReducedAllowanceLimit" -> 0.00,
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
    ),
    "incomeTax" -> Json.obj(
      "payAndPensionsProfit" -> Json.obj(
        "band" -> Json.arr(Json.obj(
          "name" -> "BRT",
          "rate" -> 20.0,
          "income" -> 20000.00,
          "amount" -> 4000.00
        ), Json.obj(
          "name" -> "HRT",
          "rate" -> 40.0,
          "income" -> 100000.00,
          "amount" -> 40000.00
        ), Json.obj(
          "name" -> "ART",
          "rate" -> 45.0,
          "income" -> 50000.00,
          "amount" -> 22500.00
        )
        )
      )
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
  
  val testCalculationErrorModel: CalculationErrorModel = CalculationErrorModel(testErrorStatus, testErrorMessage)
  val testCalculationErrorJson: JsValue =
     Json.obj(
       "code" -> testErrorStatus,
       "message" -> testErrorMessage
     )

  val testCalculationInputJson: JsValue =
    Json.obj(
      "calcOutput" -> Json.obj(
        "calcID" -> testTaxCalculationId,
        "calcAmount" -> 543.21,
        "calcTimestamp" -> testTimeStampString,
        "crystallised" -> true,
        "calcResult" -> Json.obj(
          "incomeTaxNicYtd" -> 123.45,
          "eoyEstimate" -> Json.obj(
            "incomeTaxNicAmount" -> 987.65
          )
       )
     )
  )

  val testCalculationOutputJson: JsValue =
    Json.obj(
      "calcID" -> testTaxCalculationId,
      "calcAmount" -> 543.21,
      "calcTimestamp" -> testTimeStampString,
      "crystallised" -> true,
      "incomeTaxNicYtd" -> 123.45,
      "incomeTaxNicAmount" -> 987.65
    )

  val testCalcModelCrystalised: CalculationModel =
    CalculationModel(
      testTaxCalculationId,
      Some(543.21),
      Some(testTimeStampString),
      Some(true),
      Some(123.45),
      Some(987.65)
    )

  val testCalcModelEstimate: CalculationModel =
    CalculationModel(
      testTaxCalculationId,
      Some(543.21),
      Some(testTimeStampString),
      None,
      Some(123.45),
      Some(987.65)
    )

  val errorCalculationModel: CalculationErrorModel =
    CalculationErrorModel(
      Status.INTERNAL_SERVER_ERROR,
      "Internal server error"
    )

  val testCalcModelNoDisplayAmount: CalculationModel =
    CalculationModel(
      testTaxCalculationId,
      None,
      Some(testTimeStampString),
      Some(true),
      None,
      Some(987.65)
    )

  val testCalcModelNoAnnualEstimate: CalculationModel =
    CalculationModel(
      testTaxCalculationId,
      Some(543.21),
      Some(testTimeStampString),
      Some(true),
      Some(123.45),
      None
    )

  val testCalcModelEmpty: CalculationModel =
    CalculationModel(
      testTaxCalculationId,
      None,
      None,
      None,
      None,
      None
    )
}
