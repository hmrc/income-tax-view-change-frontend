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
    nationalRegime = Some("Scotland"),
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
      0,
      Seq(BandModel(
        taxableIncome = 1.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 20.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 5000.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 7.5, threshold = None,
          apportionedThreshold = None,
          income = 1000,
          amount = 75.0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 37.5, threshold = None,
          apportionedThreshold = None,
          income = 2000,
          amount = 750.0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 38.1,
          threshold = None,
          apportionedThreshold = None,
          income = 3000,
          amount = 1143.0
        )
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
    ),
    giftAid = GiftAidModel(
    paymentsMade = 0,
    rate = 0.0,
    taxableAmount = 0
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
      0,
      Seq(BandModel(
        taxableIncome = 1000.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 5000.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 1000,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 110,
      class4 = 13.86
    ),
    giftAid = GiftAidModel(
      paymentsMade = 200.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 5000.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 1000,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 110,
      class4 = 13.86
    ),
    giftAid = GiftAidModel(
      paymentsMade = 200.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 5000.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 1000,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 110,
      class4 = 13.86
    ),
    giftAid = GiftAidModel(
      paymentsMade = 200.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 0.00,
      class4 = 0.00
    ),
      giftAid = GiftAidModel(
      paymentsMade = 100.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
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
    ),
    giftAid = GiftAidModel(
      paymentsMade = 200.0,
      rate = 0.00,
      taxableAmount = 0.0
    )
  )

  val busPropBRTCalcDataModel = CalculationDataModel(
    nationalRegime = Some("Scotland"),
    totalIncomeTaxNicYtd = 149.86,
    totalTaxableIncome = 132.00,
    personalAllowance = 2868.00,
    taxReliefs = 24.90,
    totalIncomeAllowancesUsed = 2868.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 1500.00,
      ukProperty = 1500.00,
      bankBuildingSocietyInterest = 0.00,
      ukDividends = 0.0
    ),
    savingsAndGains = SavingsAndGainsModel(
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 0.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 110,
      class4 = 13.86
    ),
    giftAid = GiftAidModel(
      paymentsMade = 300.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 0.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
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
    ),
    giftAid = GiftAidModel(
      paymentsMade = 400.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 0.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
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
    ),
    giftAid = GiftAidModel(
      paymentsMade = 500.0,
      rate = 0.00,
      taxableAmount = 5.0
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
      0,
      Seq(BandModel(
        taxableIncome = 1.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 20.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 5000.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 7.5, threshold = None,
          apportionedThreshold = None,
          income = 1000.0,
          amount = 75.0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 10000.00,
      class4 = 14000.00
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 20000.00, 4000.00),
      TaxBandModel("HRT", 40.0, 100000.00, 40000.00),
      TaxBandModel("ART", 45.0, 50000.00, 22500.00)
    ),
    giftAid = GiftAidModel(
      paymentsMade = 600.0,
      rate = 0.00,
      taxableAmount = 0.0
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
    savingsAndGains = SavingsAndGainsModel(
      0,
      Seq(BandModel(
        taxableIncome = 1.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 20.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 5000.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 7.5, threshold = None,
          apportionedThreshold = None,
          income = 1000.0,
          amount = 75.0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 37.5, threshold = None,
          apportionedThreshold = None,
          income = 2000.0,
          amount = 750.0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 10000.00,
      class4 = 14000.00
    ),
    giftAid = GiftAidModel(
      paymentsMade = 700.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 1.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 20.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 5000.0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 7.5, threshold = None,
          apportionedThreshold = None,
          income = 1000.0,
          amount = 75.0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 37.5, threshold = None,
          apportionedThreshold = None,
          income = 2000.0,
          amount = 750.0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 38.1,
          threshold = None,
          apportionedThreshold = None,
          income = 3000.0,
          amount = 1143.0
        )
      )
    ),
    nic = NicModel(
      class2 = 10000.00,
      class4 = 14000.00
    ),
    giftAid = GiftAidModel(
      paymentsMade = 800.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 100.0,
      class4 = 23.86
    ),
    giftAid = GiftAidModel(
      paymentsMade = 900.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq(BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 100.0,
      class4 = 23.86
    ),
    giftAid = GiftAidModel(
      paymentsMade = 30.0,
      rate = 0.00,
      taxableAmount = 5.0
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
      0,
      Seq(BandModel(
        taxableIncome = 2500.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "SSR"
      ),
      BandModel(
        taxableIncome = 0.00,
        taxRate = 0.0,
        taxAmount = 0.0,
        name = "ZRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 20.0,
        taxAmount = 0.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 40.0,
        taxAmount = 0.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 0.0,
        taxRate = 45.0,
        taxAmount = 0.0,
        name = "ART"
      ))
    ),
    dividends = DividendsModel(
      totalAmount = 0,
      Seq(
        DividendsBandModel(
          name = "basic-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "higher-band",
          rate = 0, threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        ),
        DividendsBandModel(
          name = "additional-band",
          rate = 0,
          threshold = None,
          apportionedThreshold = None,
          income = 0,
          amount = 0
        )
      )
    ),
    nic = NicModel(
      class2 = 100.0,
      class4 = 23.86
    ),
    giftAid = GiftAidModel(
      paymentsMade = 40.0,
      rate = 0.00,
      taxableAmount = 0.0
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
      0,
      Seq()
    ),
    dividends = DividendsModel(
      totalAmount = 0,
      Seq()
    ),
    nic = NicModel(
      class2 = 0.0,
      class4 = 0.0
    ),
    giftAid = GiftAidModel(
      paymentsMade = 0.0,
      rate = 0.00,
      taxableAmount = 0.0
    ),
    payAndPensionsProfitBands = Nil
  )

  val mandatoryCalculationDataSuccessJson: JsValue = Json.obj(
    "calcOutput" -> Json.obj(
      "calcResult" -> Json.obj(
        "incomeTaxNicYtd" -> 90500
      )
    )
  )

  val calculationDataSuccessJson: JsValue = Json.obj(
    "nationalRegime" -> "Scotland",
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
    "bands" -> Json.arr(
      Json.obj(
        "name" -> "SSR",
        "taxableIncome" -> 1,
        "taxAmount" -> 0,
        "taxRate" -> 0
      ),
      Json.obj(
        "name" -> "ZRT",
        "taxableIncome" -> 20,
        "taxAmount" -> 0,
        "taxRate" -> 0
      ),
      Json.obj(
        "name" -> "BRT",
        "taxableIncome" -> 0,
        "taxAmount" -> 0,
        "taxRate" -> 20
      ),
      Json.obj(
        "name" -> "HRT",
        "taxableIncome" -> 0,
        "taxAmount" -> 0,
        "taxRate" -> 40
      ),
      Json.obj(
        "name" -> "ART",
        "taxableIncome" -> 0,
        "taxAmount" -> 0,
        "taxRate" -> 45
      )
    ),
    "total" -> 0
  ),
    "nic" -> Json.obj(
      "class2" -> 10000,
      "class4" -> 14000
    ),
    "giftAid" -> Json.obj(
      "paymentsMade" -> 0 ,
      "rate" -> 0.0,
      "taxableAmount" -> 0
    ),
    "eoyEstimate" -> Json.obj(
      "totalNicAmount" -> 66000
    ),
    "incomeTax" -> Json.obj(
      "payAndPensionsProfit" -> Json.obj(
        "band" -> Json.arr(
          Json.obj(
            "name" -> "BRT",
            "rate" -> 20.0,
            "income" -> 20000.00,
            "taxAmount" -> 4000.00
          ),
          Json.obj(
            "name" -> "HRT",
            "rate" -> 40.0,
            "income" -> 100000.00,
            "taxAmount" -> 40000.00
          ),
          Json.obj(
            "name" -> "ART",
            "rate" -> 45.0,
            "income" -> 50000.00,
            "taxAmount" -> 22500.00
          )
        )
      ),
      "dividends" -> Json.obj(
        "totalAmount" -> 5000,
        "band" -> Json.arr(
          Json.obj(
            "name" -> "basic-band",
            "rate" -> 7.5,
            "income" -> 1000,
            "amount" -> 75.0
          ),
          Json.obj(
            "name" -> "higher-band",
            "rate" -> 37.5,
            "income" -> 2000,
            "amount" -> 750
          ),
          Json.obj(
            "name" -> "additional-band",
            "rate" -> 38.1,
            "income" -> 3000,
            "amount" -> 1143
          ))
      )
    )
  )

  val calculationDataFullJson: JsValue = Json.obj(
   "payPensionsProfitAtBRT" -> 20000,
   "incomeTaxOnPayPensionsProfitAtBRT" -> 4000,
   "payPensionsProfitAtHRT" -> 100000,
   "incomeTaxOnPayPensionsProfitAtHRT" -> 40000,
   "payPensionsProfitAtART" -> 50000,
   "incomeTaxOnPayPensionsProfitAtART" -> 22500,
   "netPropertyFinanceCosts" -> 0,
   "dividendsAtZeroRate" -> 0,
   "incomeTaxOnDividendsAtZeroRate" -> 0,
   "dividendsAtBRT" -> 1000,
   "incomeTaxOnDividendsAtBRT" -> 75,
   "dividendsAtHRT" -> 2000,
   "incomeTaxOnDividendsAtHRT" -> 750,
   "dividendsAtART" -> 3000,
   "incomeTaxOnDividendsAtART" -> 1143,
   "limitBRT" -> 0,
   "limitHRT" -> 0,
   "rateBRT" -> 20,
   "rateHRT" -> 40,
   "rateART" -> 45,
   "allowanceBRT" -> 0,
   "dividendAllowance" -> 5000,
   "dividendBRT" -> 7.5,
   "dividendHRT" -> 37.5,
   "dividendART" -> 38.1,
    "calcOutput" -> Json.obj(
    "calcSummary" -> Json.obj(
      "nationalRegime" -> "UK",
      "incomeTaxGross" -> 68985412739.5,
      "taxDeducted" -> 33971782272.57,
      "incomeTaxNetOfDeductions" -> 39426248386.69,
      "nic2Gross" -> 10000,
      "nic4Gross" -> 14000,
      "nic2NetOfDeductions" -> 89311246978.32,
      "nic4NetOfDeductions" -> 193784559071.9
    ),
    "calcResult" -> Json.obj(
      "incomeTaxNicYtd" -> 90500,
      "nationalRegime" -> "Scotland",
      "totalTaxableIncome" -> 198500,
      "totalNic" -> 180000,
      "nic" -> Json.obj(
        "class2" -> Json.obj(
          "amount" -> 10000,
          "weekRate" -> 2.95,
          "weeks" -> 13,
          "limit" -> 6205,
          "apportionedLimit" -> 1547
        ),
        "class4" -> Json.obj(
          "totalAmount" -> 14000,
          "band" -> Json.arr(
            Json.obj(
              "name" -> "ZRT",
              "rate" -> 0,
              "threshold" -> 8424,
              "apportionedThreshold" -> 2101,
              "income" -> 2101,
              "amount" -> 0
            ),
            Json.obj(
              "name" -> "BRT",
              "rate" -> 9,
              "threshold" -> 46350,
              "apportionedThreshold" -> 11556,
              "income" -> 3096,
              "amount" -> 278.64
            )
          )
        )
      ),
      "totalBeforeTaxDeducted" -> 100,
      "totalTaxDeducted" -> 200,
      "annualAllowances" -> Json.obj(
        "personalAllowance" -> 11500
      ),
      "incomeTax" -> Json.obj(
        "totalAllowancesAndReliefs" -> 0,
        "giftAid" -> Json.obj(
          "paymentsMade" -> 0,
          "rate" -> 0.0,
          "taxableIncome" -> 0
        ),
        "payAndPensionsProfit" -> Json.obj(
          "band" -> Json.arr(Json.obj(
            "name" -> "BRT",
            "rate" -> 20.0,
            "income" -> 20000.00,
            "taxAmount" -> 4000.00
          ), Json.obj(
            "name" -> "HRT",
            "rate" -> 40.0,
            "income" -> 100000.00,
            "taxAmount" -> 40000.00
          ), Json.obj(
            "name" -> "ART",
            "rate" -> 45.0,
            "income" -> 50000.00,
            "taxAmount" -> 22500.00
          )
          )
        ),
        "savingsAndGains" -> Json.obj(
          "totalAmount" -> 0,
          "taxableIncome" -> 60207080823.340004,
          "band" -> Json.arr(
            Json.obj(
              "name" -> "SSR",
              "rate" -> 0,
              "income" -> 1,
              "taxAmount" -> 0,
              "threshold" -> 4000,
              "apportionedThreshold" -> 5000
            ),
            Json.obj(
              "name" -> "ZRT",
              "rate" -> 0,
              "income" -> 20,
              "taxAmount" -> 0,
              "threshold" -> 1,
              "apportionedThreshold" -> 1
            ),
            Json.obj(
              "name" -> "BRT",
              "rate" -> 20,
              "income" -> 0,
              "taxAmount" -> 0,
              "threshold" -> 1,
              "apportionedThreshold" -> 1
            ),
            Json.obj(
              "name" -> "HRT",
              "rate" -> 40,
              "income" -> 0,
              "taxAmount" -> 0,
              "threshold" -> 1,
              "apportionedThreshold" -> 1
            ),
            Json.obj(
              "name" -> "ART",
              "rate" -> 45,
              "income" -> 0,
              "taxAmount" -> 0,
              "threshold" -> 1,
              "apportionedThreshold" -> 1
            )
          ),
          "personalAllowanceUsed" -> 15487995938.37
        ),
        "dividends" -> Json.obj(
          "totalAmount" -> 5000,
          "band" -> Json.arr(
            Json.obj(
              "name" -> "basic-band",
              "rate" -> 7.5,
              "income" -> 1000,
              "taxAmount" -> 75.0
            ),
            Json.obj(
              "name" -> "higher-band",
              "rate" -> 37.5,
              "income" -> 2000,
              "taxAmount" -> 750
            ),
            Json.obj(
              "name" -> "additional-band",
              "rate" -> 38.1,
              "income" -> 3000,
              "taxAmount" -> 1143
            )))
      ),
      "taxableIncome" -> Json.obj(
        "totalIncomeAllowancesUsed" -> 12005,
        "incomeReceived" -> Json.obj(
          "employmentIncome" -> 100,
          "selfEmploymentIncome" -> 200000,
          "ukPropertyIncome" -> 10000,
          "bbsiIncome" -> 1999,
          "ukDividendIncome" -> 10000,
          "employments"-> Json.obj(
            "totalPay" -> 55000961025.98,
            "totalBenefitsAndExpenses"-> 96945498573.96,
            "totalAllowableExpenses"-> 94037790451.1,
            "employment"-> Json.arr(
              Json.obj(
                "incomeSourceID"-> "33j38jIEnKNa5aV",
                "latestDate"-> "3661-09-02",
                "netPay"-> 57775446337.53,
                "benefitsAndExpenses"-> 25047077371.97,
                "allowableExpenses"-> 3585774590.1800003
              )
            )
          ),
          "selfEmployment" -> Json.arr(
            Json.obj(
              "incomeSourceID" -> "BcjTLlMBb3vlAne",
              "latestDate" -> "8225-09-22",
              "taxableIncome" -> 60455823926.5,
              "accountStartDate" -> "9571-09-26",
              "accountEndDate" -> "5906-07-06",
              "finalised" -> false,
              "losses" -> 56154428355.74
            ),
            Json.obj(
              "incomeSourceID" -> "v4wly6Tn5JfwLjB",
              "latestDate" -> "5217-10-10",
              "taxableIncome" -> 82204159598.88,
              "accountStartDate" -> "5688-03-30",
              "accountEndDate" -> "6756-05-09",
              "finalised" -> true,
              "losses" -> 16496201041.710001
            )
          ),
          "ukProperty" -> Json.obj(
            "incomeSourceID" -> "Q9wFE164KgzVR2m",
            "latestDate" -> "0379-03-30",
            "taxableProfit" -> 60297189257.64,
            "taxableProfitFhlUk" -> 7347733383.54,
            "finalised" -> false,
            "losses" -> 4549677842.09,
            "lossesFhlUk" -> 79888527010.89
          ),
          "bbsi" -> Json.obj(
            "totalTaxedInterestIncome" -> 66480042461.21,
            "taxedAccounts" -> Json.arr(
              Json.obj(
                "incomeSourceID" -> "yysKzVIfqcLWVuQ",
                "latestDate" -> "7650-11-26",
                "name" -> "eiusmod Ut et dolore deserunt",
                "gross" -> 10513891004.58,
                "net" -> 63946537010.58,
                "taxDeducted" -> 32104251608.440002
              )
            )
          ),
          "ukDividend" -> Json.obj(
            "ukDividends" -> 7549829503.03,
            "otherUkDividends" -> 34590087015.69
          )
        )
      ),
      "eoyEstimate" -> Json.obj(
        "totalTaxableIncome" -> 198500,
        "incomeTaxAmount" -> 89999999.99,
        "nic2" -> 89999999.99,
        "nic4" -> 89999999.99,
        "totalNicAmount" -> 66000,
        "incomeTaxNicAmount" -> 66000.00,
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
        )
      )
    ),
    "eoyEstimate" -> Json.obj(
      "totalTaxableIncome" -> 198500,
      "incomeTaxAmount" -> 89999999.99,
      "nic2" -> 89999999.99,
      "nic4" -> 89999999.99,
      "totalNicAmount" -> 66000,
      "incomeTaxNicAmount" -> 66000.00,
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
       )
     )
   )
  )

  val calculationDataSuccessMinJson: JsValue = Json.obj()

  val calculationDataErrorModel: CalculationDataErrorModel = CalculationDataErrorModel(testErrorStatus, testErrorMessage)
  val calculationDataErrorJson: JsValue =
    Json.obj(
      "code" -> testErrorStatus,
      "message" -> testErrorMessage
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

  val testCalcModelSuccess: CalculationModel =
    CalculationModel(
      testTaxCalculationId,
      Some(543.21),
      Some(testTimeStampString),
      None,
      None,
      None
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
