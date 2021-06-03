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

package assets

import assets.BaseIntegrationTestConstants._
import enums.{Crystallised, Estimate}
import models.calculation._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}

object CalcBreakdownIntegrationTestConstants {

  val calculationNoBillModel = Calculation(
    crystallised = true
  )

  val calculationBillTaxableIncomeZeroModel = Calculation(
    totalIncomeTaxAndNicsDue = Some(0.00),
    totalTaxableIncome = Some(0.00),
    crystallised = true,
    nationalRegime = Some("UK")
  )

  val calculationBillWelshModel = Calculation(
    totalTaxableIncome = Some(0.00),
    crystallised = true,
    nationalRegime = Some("Wales"),
    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = None,
      totalPropertyProfit = None,
      incomeTaxAmount = Some(66500),
      taxableIncome = Some(170000),
      List(TaxBand(
        name = "BRT",
        rate = 20.0,
        income = 20000.00,
        taxAmount = 4000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000)
      )
    )
  )

  val calculationBillScotlandModel = Calculation(
    totalTaxableIncome = Some(0.00),
    crystallised = true,
    nationalRegime = Some("Scotland"),
    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = None,
      totalPropertyProfit = None,
      incomeTaxAmount = Some(66500),
      taxableIncome = Some(170000),
      List(TaxBand(
        name = "SRT",
        rate = 10.0,
        income = 20000.00,
        taxAmount = 2000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
        TaxBand(
          name = "IRT",
          rate = 25.0,
          income = 20000.00,
          taxAmount = 45000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000)
      )
    )
  )

  val calculationDataSuccessModel = Calculation(
    totalIncomeTaxAndNicsDue = Some(90500.00),
    totalIncomeTaxNicsCharged = Some(90500.00),
    totalIncomeReceived = Some(199505.00),
    totalTaxableIncome = Some(198500.00),
    incomeTaxNicAmount = None,
    timestamp = Some(testTimeStampString),
    crystallised = false,
    nationalRegime = None,

    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = None,
      totalPropertyProfit = None,
      incomeTaxAmount = Some(66500),
      taxableIncome = Some(170000),
      List(TaxBand(
        name = "BRT",
        rate = 20.0,
        income = 20000.00,
        taxAmount = 4000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    ),

    savingsAndGains = SavingsAndGains(
      Some(0),
      Some(0),
      Some(0),
      Some(0),
      List(TaxBand(
        name = "SSR",
        rate = 0.0,
        income = 1.00,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "ZRT",
          rate = 0.0,
          income = 20.00,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    ),

    reductionsAndCharges = ReductionsAndCharges(
      giftAidTax = Some(5000.99),
      totalPensionSavingsTaxCharges = Some(5000.99),
      statePensionLumpSumCharges = Some(5000.99),
      totalStudentLoansRepaymentAmount = Some(5000.99),
      totalResidentialFinanceCostsRelief = Some(5000.99)
    ),

    dividends = Dividends(
      incomeTaxAmount = Some(5000),
      taxableIncome = Some(6000),
      bands = List(TaxBand(
        name = "basic-band",
        rate = 7.5,
        income = 1000,
        taxAmount = 75.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "higher-band",
          rate = 37.5,
          income = 2000,
          taxAmount = 750.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "additional-band",
          rate = 38.1,
          income = 3000,
          taxAmount = 1143.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        )
      )
    ),

    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(11500),
      totalPensionContributions = Some(11501),
      totalAllowancesAndDeductions = Some(250),
      totalReliefs = Some(250)
    ),
    nic = Nic(
      class2 = Some(10000.00),
      class4 = Some(14000.00),
      totalNic = Some(66000.00)
    )
  )

  val scottishTaxBandModelJustPPP = Calculation(
    totalIncomeTaxAndNicsDue = Some(0),
    totalIncomeTaxNicsCharged = Some(149.86),
    totalTaxableIncome = Some(132.00),
    incomeTaxNicAmount = None,
    timestamp = None,
    crystallised = true,
    nationalRegime = Some("Scotland"),

    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = None,
      totalPropertyProfit = None,
      incomeTaxAmount = Some(66500),
      taxableIncome = Some(170000),
      List(TaxBand(
        name = "SRT",
        rate = 10.0,
        income = 20000.00,
        taxAmount = 2000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),

        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),

        TaxBand(
          name = "IRT",
          rate = 25.0,
          income = 20000.00,
          taxAmount = 45000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),

        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 500000.00,
          taxAmount = 22500.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    )
  )

  val scottishTaxBandModelJustLs = Calculation(
    crystallised = true,
    nationalRegime = Some("Scotland"),
    lumpSums = LumpSums(
      List(TaxBand(
        name = "SRT",
        rate = 10.0,
        income = 20000.00,
        taxAmount = 2000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),

        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),

        TaxBand(
          name = "IRT",
          rate = 25.0,
          income = 20000.00,
          taxAmount = 45000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),

        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 500000.00,
          taxAmount = 22500.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    )
  )

  val scottishTaxBandModelJustGols = Calculation(
    crystallised = true,
    nationalRegime = Some("Scotland"),
    gainsOnLifePolicies = GainsOnLifePolicies(
      List(TaxBand(
        name = "SRT",
        rate = 10.0,
        income = 20000.00,
        taxAmount = 2000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),

        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),

        TaxBand(
          name = "IRT",
          rate = 25.0,
          income = 20000.00,
          taxAmount = 45000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),

        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 500000.00,
          taxAmount = 22500.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    )
  )

  val scottishTaxBandModelJustSRT = Calculation(
    totalIncomeTaxAndNicsDue = Some(0),
    totalIncomeTaxNicsCharged = Some(149.86),
    totalTaxableIncome = Some(132.00),
    incomeTaxNicAmount = None,
    timestamp = None,
    crystallised = true,
    nationalRegime = Some("Scotland"),

    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = None,
      totalPropertyProfit = None,
      incomeTaxAmount = Some(66500),
      taxableIncome = Some(170000),
      List(TaxBand(
        name = "BRT",
        rate = 20.0,
        income = 20000.00,
        taxAmount = 4000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    ),

    savingsAndGains = SavingsAndGains(
      Some(0),
      Some(0),
      Some(0),
      Some(0),
      List(TaxBand(
        name = "SSR",
        rate = 0.0,
        income = 1.00,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "ZRT",
          rate = 0.0,
          income = 20.00,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    ),

    reductionsAndCharges = ReductionsAndCharges(
      giftAidTax = Some(5000.99),
      totalPensionSavingsTaxCharges = Some(5000.99),
      statePensionLumpSumCharges = Some(5000.99),
      totalStudentLoansRepaymentAmount = Some(5000.99),
      totalResidentialFinanceCostsRelief = Some(5000.99)
    ),

    dividends = Dividends(
      incomeTaxAmount = Some(5000),
      taxableIncome = Some(6000),
      bands = List(TaxBand(
        name = "basic-band",
        rate = 7.5,
        income = 1000,
        taxAmount = 75.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "higher-band",
          rate = 37.5,
          income = 2000,
          taxAmount = 750.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "additional-band",
          rate = 38.1,
          income = 3000,
          taxAmount = 1143.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        )
      )
    ),

    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(2868.00),
      totalPensionContributions = Some(12500),
      lossesAppliedToGeneralIncome = Some(12500),
      giftOfInvestmentsAndPropertyToCharity = Some(1000.25),
      totalAllowancesAndDeductions = Some(24.90),
      totalReliefs = Some(120)
    ),
    nic = Nic(
      class2 = Some(10000.00),
      class4 = Some(14000.00),
      totalNic = Some(66000.00)
    )
  )

  val busPropBRTCalcDataModel = Calculation(
    totalIncomeTaxAndNicsDue = Some(0),
    totalIncomeTaxNicsCharged = Some(149.86),
    totalTaxableIncome = Some(132.00),
    incomeTaxNicAmount = None,
    timestamp = Some(testTimeStampString),
    crystallised = true,
    nationalRegime = Some("Scotland"),
    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = None,
      totalPropertyProfit = None,
      incomeTaxAmount = Some(66500),
      taxableIncome = Some(170000),
      bands = List(TaxBand(
        name = "BRT",
        rate = 20.0,
        income = 20000.00,
        taxAmount = 4000.00,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    ),
    savingsAndGains = SavingsAndGains(
      Some(0),
      Some(0),
      Some(0),
      Some(0),
      List(TaxBand(
        name = "SSR",
        rate = 0.0,
        income = 0.00,
        taxAmount = 0.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "ZRT",
          rate = 0.0,
          income = 0.00,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    ),

    reductionsAndCharges = ReductionsAndCharges(
      giftAidTax = Some(5000.99),
      totalPensionSavingsTaxCharges = Some(5000.99),
      statePensionLumpSumCharges = Some(5000.99),
      totalStudentLoansRepaymentAmount = Some(5000.99),
      totalResidentialFinanceCostsRelief = Some(5000.99)
    ),

    dividends = Dividends(
      incomeTaxAmount = Some(5000),
      taxableIncome = Some(6000),
      bands = List(TaxBand(
        name = "basic-band",
        rate = 7.5,
        income = 1000,
        taxAmount = 75.0,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ),
        TaxBand(
          name = "higher-band",
          rate = 37.5,
          income = 2000,
          taxAmount = 750.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "additional-band",
          rate = 38.1,
          income = 3000,
          taxAmount = 1143.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        )
      )
    ),
    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(2868.00),
      totalPensionContributions = Some(12500),
      lossesAppliedToGeneralIncome = Some(12500),
      giftOfInvestmentsAndPropertyToCharity = Some(1000.25),
      totalAllowancesAndDeductions = Some(24.90),
      totalReliefs = Some(120)
    ),
    nic = Nic(
      class2 = Some(110),
      class4 = Some(13.86),
      totalNic = Some(66000.00)
    )
  )


  val justBusinessCalcDataModel = Calculation(
    totalIncomeTaxAndNicsDue = Some(0),
    totalIncomeTaxNicsCharged = Some(149.86),
    totalTaxableIncome = Some(132.00),
    incomeTaxNicAmount = None,
    timestamp = None,
    crystallised = true,
    nationalRegime = Some("Scotland"),
    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = None,
      totalPropertyProfit = None,
      incomeTaxAmount = Some(26),
      taxableIncome = Some(132),
      List(
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 132.00,
          taxAmount = 26.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.00,
          taxAmount = 0.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.00,
          taxAmount = 0.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    ),
    savingsAndGains = SavingsAndGains(
      Some(0),
      Some(0),
      bands = List(
        TaxBand(
          name = "SSR",
          rate = 0.0,
          income = 0.00,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ZRT",
          rate = 0.0,
          income = 0.00,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.0,
          taxAmount = 0.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        ))
    ),
    reductionsAndCharges = ReductionsAndCharges(
      giftAidTax = Some(5000.99),
      totalPensionSavingsTaxCharges = Some(5000.99),
      statePensionLumpSumCharges = Some(5000.99),
      totalStudentLoansRepaymentAmount = Some(5000.99),
      totalResidentialFinanceCostsRelief = Some(5000.99)
    ),

    dividends = Dividends(
      incomeTaxAmount = Some(5000),
      taxableIncome = Some(6000),
      bands = List(
        TaxBand(
          name = "BRT",
          rate = 7.5,
          income = 1000,
          taxAmount = 75.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
        TaxBand(
          name = "higher-band",
          rate = 37.5,
          income = 2000,
          taxAmount = 750.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
        TaxBand(
          name = "additional-band",
          rate = 38.1,
          income = 3000,
          taxAmount = 1143.0,
          bandLimit = 15000,
          apportionedBandLimit = 15000
        )
      )
    ),
    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(2868.00),
      totalPensionContributions = Some(12500),
      lossesAppliedToGeneralIncome = Some(12500),
      giftOfInvestmentsAndPropertyToCharity = Some(1000.25),
      totalAllowancesAndDeductions = Some(24.90),
      totalReliefs = Some(120)
    ),
    nic = Nic(
      class2 = Some(110),
      class4 = Some(13.86),
      totalNic = Some(66000.00)
    )
  )

  val calculationDataErrorModel: CalculationErrorModel = CalculationErrorModel(testErrorStatus, testErrorMessage)
  val calculationDataErrorJson: JsValue =
    Json.obj(
      "code" -> testErrorStatus,
      "message" -> testErrorMessage
    )

  val calculationDisplaySuccessModel: Calculation => CalcDisplayModel = calcModel =>
    CalcDisplayModel(
      calcModel.timestamp.get,
      calcModel.totalIncomeTaxAndNicsDue.get,
      calcModel,
      Estimate
    )

  val calculationDisplaySuccessCrystalisationModel: Calculation => CalcDisplayModel = calcModel =>
    CalcDisplayModel(
      calcModel.timestamp.get,
      calcModel.totalIncomeTaxAndNicsDue.get,
      calcModel,
      Crystallised
    )

  val testCalcDisplayModel: CalcDisplayModel =
    CalcDisplayModel(
      testTimeStampString,
      123.45,
      calculationDataSuccessModel,
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
      "incomeTaxNicAmount" -> 987.65,
      "calculationDataModel" -> Json.obj(
        "totalTaxableIncome" -> 0,
        "totalIncomeTaxNicYtd" -> 123.45,
        "annualAllowances" -> Json.obj(
          "personalAllowance" -> 0
        ),
        "taxReliefs" -> 0,
        "totalIncomeAllowancesUsed" -> 0,
        "giftOfInvestmentsAndPropertyToCharity" -> 0,
        "incomeReceived" -> Json.obj(
          "selfEmployment" -> 0,
          "ukProperty" -> 0,
          "bankBuildingSocietyInterest" -> 0,
          "ukDividends" -> 0
        ),
        "incomeTax" -> Json.obj(
          "dividends" -> Json.obj(
            "totalAmount" -> 0,
            "taxableIncome" -> 0,
            "band" -> Json.arr()
          ),
          "payPensionsProfit" -> Json.obj(
            "totalAmount" -> 0,
            "taxableIncome" -> 0,
            "payAndPensionsProfitBands" -> Json.arr()
          ),
          "savingsAndGains" -> Json.obj(
            "total" -> 0,
            "taxableIncome" -> 0,
            "bands" -> Json.arr()
          ),
          "reductionsAndCharges" -> Json.obj(
            "giftAidTax" -> 0,
            "totalPensionSavingsTaxCharges" -> 0,
            "statePensionLumpSumCharges" -> 0,
            "totalStudentLoansRepaymentAmount" -> 0,
            "propertyFinanceRelief" -> 0
          ),
        ),
        "nic" -> Json.obj(
          "class2" -> 0,
          "class4" -> 0

        )
      )
    )

  val testCalcModelCrystallised: Calculation =
    Calculation(
      totalIncomeTaxAndNicsDue = Some(543.21),
      totalIncomeTaxNicsCharged = Some(123.45),
      totalTaxableIncome = Some(0),
      incomeTaxNicAmount = Some(987.65),
      timestamp = Some(testTimeStampString),
      crystallised = true,
      nationalRegime = Some("UK"),
      payPensionsProfit = PayPensionsProfit(
        totalSelfEmploymentProfit = None,
        totalPropertyProfit = None,
        incomeTaxAmount = Some(66500),
        taxableIncome = Some(170000),
        bands = List(TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 100000.00,
            taxAmount = 40000.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 45.0,
            income = 50000.00,
            taxAmount = 22500.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      lumpSums = LumpSums(
        bands = List(TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 100000.00,
            taxAmount = 40000.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 45.0,
            income = 50000.00,
            taxAmount = 22500.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      gainsOnLifePolicies = GainsOnLifePolicies(
        bands = List(TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 14000,
          apportionedBandLimit = 14000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 100000.00,
            taxAmount = 40000.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 45.0,
            income = 50000.00,
            taxAmount = 22500.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      savingsAndGains = SavingsAndGains(
        Some(0),
        Some(500),
        Some(0),
        Some(500),
        List(
          TaxBand(
            name = "SSR",
            rate = 0.0,
            income = 1.00,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "BRT",
            rate = 10.0,
            income = 20.00,
            taxAmount = 2.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTBR",
            rate = 0.0,
            income = 20.0,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 2000.0,
            taxAmount = 800.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTHR",
            rate = 0.0,
            income = 10000.0,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 50.0,
            income = 100000.0,
            taxAmount = 5000.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      reductionsAndCharges = ReductionsAndCharges(
        giftAidTax = Some(5000),
        totalPensionSavingsTaxCharges = Some(5000),
        statePensionLumpSumCharges = Some(5000),
        totalStudentLoansRepaymentAmount = Some(5000),
        totalResidentialFinanceCostsRelief = Some(5000),
        totalForeignTaxCreditRelief = Some(6000),
        totalNotionalTax = Some(7000),
        incomeTaxDueAfterTaxReductions = Some(2000),
        reliefsClaimed = Some(Seq(ReliefsClaimed("deficiencyRelief", Some(1000)), ReliefsClaimed("vctSubscriptions", Some(2000)),
          ReliefsClaimed("eisSubscriptions", Some(3000)), ReliefsClaimed("seedEnterpriseInvestment", Some(4000)),
          ReliefsClaimed("communityInvestment", Some(5000)), ReliefsClaimed("socialEnterpriseInvestment", Some(6000)),
          ReliefsClaimed("maintenancePayments", Some(7000)),
          ReliefsClaimed("qualifyingDistributionRedemptionOfSharesAndSecurities", Some(8000)),
          ReliefsClaimed("nonDeductibleLoanInterest", Some(9000))
        ))
      ),
      dividends = Dividends(
        incomeTaxAmount = Some(5000),
        taxableIncome = Some(6000),
        bands = List(
          TaxBand(
            name = "BRT",
            rate = 7.5,
            income = 1000,
            taxAmount = 75.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTBR",
            rate = 0,
            income = 1000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 37.5,
            income = 2000,
            taxAmount = 750.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTHR",
            rate = 0,
            income = 2000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 38.1,
            income = 3000,
            taxAmount = 1143.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTAR",
            rate = 0,
            income = 3000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      allowancesAndDeductions = AllowancesAndDeductions(
        personalAllowance = Some(11500)
      ),
      nic = Nic(
        class2 = Some(10000.00),
        class4 = Some(14000.00),
        class4Bands = Some(Seq(
          NicBand(
            name = "ZRT",
            rate = 1,
            income = 2000,
            amount = 100
          ),
          NicBand(
            name = "BRT",
            rate = 2,
            income = 3000,
            amount = 200
          ),
          NicBand(
            name = "HRT",
            rate = 3,
            income = 5000,
            amount = 300
          ),
          NicBand(
            name = "giftAidTax",
            rate = 4,
            income = 6000,
            amount = 400
          ),
          NicBand(
            name = "totalPensionSavingsTaxCharges",
            rate = 5,
            income = 7000,
            amount = 500
          ),
          NicBand(
            name = "statePensionLumpSumCharges",
            rate = 6,
            income = 8000,
            amount = 600
          ),
          NicBand(
            name = "totalStudentLoansRepaymentAmount",
            rate = 7,
            income = 9000,
            amount = 700
          ),
        )),
        totalNic = Some(24000.00),
        class2VoluntaryContributions = Some(true)
      ),
      taxDeductedAtSource = TaxDeductedAtSource(
        Some(100.0),
        Some(200.0),
        Some(300.0),
        Some(400.0),
        Some(500.0),
        Some(600.0),
        Some(700.0),
        Some(800.0),
        Some(900.0),
        Some(1000.0)
      )
    )


  val testCalcModelNic2: Calculation =
    Calculation(
      totalIncomeTaxAndNicsDue = Some(543.21),
      totalIncomeTaxNicsCharged = Some(123.45),
      totalTaxableIncome = Some(0),
      incomeTaxNicAmount = Some(987.65),
      timestamp = Some(testTimeStampString),
      crystallised = true,
      nationalRegime = Some("UK"),
      payPensionsProfit = PayPensionsProfit(
        totalSelfEmploymentProfit = None,
        totalPropertyProfit = None,
        incomeTaxAmount = Some(66500),
        taxableIncome = Some(170000),
        bands = List(TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 100000.00,
            taxAmount = 40000.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 45.0,
            income = 50000.00,
            taxAmount = 22500.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      lumpSums = LumpSums(
        bands = List(TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 100000.00,
            taxAmount = 40000.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 45.0,
            income = 50000.00,
            taxAmount = 22500.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      gainsOnLifePolicies = GainsOnLifePolicies(
        bands = List(TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 100000.00,
            taxAmount = 40000.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 45.0,
            income = 50000.00,
            taxAmount = 22500.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      savingsAndGains = SavingsAndGains(
        Some(0),
        Some(500),
        Some(0),
        Some(500),
        List(
          TaxBand(
            name = "SSR",
            rate = 0.0,
            income = 1.00,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "BRT",
            rate = 10.0,
            income = 20.00,
            taxAmount = 2.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTBR",
            rate = 0.0,
            income = 20.0,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 2000.0,
            taxAmount = 800.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTHR",
            rate = 0.0,
            income = 10000.0,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 50.0,
            income = 100000.0,
            taxAmount = 5000.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      reductionsAndCharges = ReductionsAndCharges(
        giftAidTax = Some(5000),
        totalPensionSavingsTaxCharges = Some(5000),
        statePensionLumpSumCharges = Some(5000),
        totalStudentLoansRepaymentAmount = Some(5000),
        totalResidentialFinanceCostsRelief = Some(5000),
        totalForeignTaxCreditRelief = Some(6000),
        totalNotionalTax = Some(7000),
        incomeTaxDueAfterTaxReductions = Some(2000),
        reliefsClaimed = Some(Seq(ReliefsClaimed("deficiencyRelief", Some(1000)), ReliefsClaimed("vctSubscriptions", Some(2000)),
          ReliefsClaimed("eisSubscriptions", Some(3000)), ReliefsClaimed("seedEnterpriseInvestment", Some(4000)),
          ReliefsClaimed("communityInvestment", Some(5000)), ReliefsClaimed("socialEnterpriseInvestment", Some(6000)),
          ReliefsClaimed("maintenancePayments", Some(7000)),
          ReliefsClaimed("qualifyingDistributionRedemptionOfSharesAndSecurities", Some(8000)),
          ReliefsClaimed("nonDeductibleLoanInterest", Some(9000))
        ))
      ),
      dividends = Dividends(
        incomeTaxAmount = Some(5000),
        taxableIncome = Some(6000),
        bands = List(
          TaxBand(
            name = "BRT",
            rate = 7.5,
            income = 1000,
            taxAmount = 75.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTBR",
            rate = 0,
            income = 1000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 37.5,
            income = 2000,
            taxAmount = 750.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTHR",
            rate = 0,
            income = 2000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 38.1,
            income = 3000,
            taxAmount = 1143.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTAR",
            rate = 0,
            income = 3000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      allowancesAndDeductions = AllowancesAndDeductions(
        personalAllowance = Some(11500)
      ),
      nic = Nic(
        class2 = Some(10000.00),
        class4 = Some(14000.00),
        class4Bands = Some(Seq(
          NicBand(
            name = "ZRT",
            rate = 1,
            income = 2000,
            amount = 100
          ),
          NicBand(
            name = "BRT",
            rate = 2,
            income = 3000,
            amount = 200
          ),
          NicBand(
            name = "HRT",
            rate = 3,
            income = 5000,
            amount = 300
          ),
          NicBand(
            name = "giftAidTax",
            rate = 4,
            income = 6000,
            amount = 400
          ),
          NicBand(
            name = "totalPensionSavingsTaxCharges",
            rate = 5,
            income = 7000,
            amount = 500
          ),
          NicBand(
            name = "statePensionLumpSumCharges",
            rate = 6,
            income = 8000,
            amount = 600
          ),
          NicBand(
            name = "totalStudentLoansRepaymentAmount",
            rate = 7,
            income = 9000,
            amount = 700
          ),
        )),
        totalNic = Some(24000.00),
        class2VoluntaryContributions = Some(false)
      ),
      taxDeductedAtSource = TaxDeductedAtSource(
        Some(100.0),
        Some(200.0),
        Some(300.0),
        Some(400.0),
        Some(500.0),
        Some(600.0),
        Some(700.0),
        Some(800.0),
        Some(900.0),
        Some(1000.0)
      )
    )

  val testCalcModelNic2Empty: Calculation =
    Calculation(
      totalIncomeTaxAndNicsDue = Some(543.21),
      totalIncomeTaxNicsCharged = Some(123.45),
      totalTaxableIncome = Some(0),
      incomeTaxNicAmount = Some(987.65),
      timestamp = Some(testTimeStampString),
      crystallised = true,
      nationalRegime = Some("UK"),
      payPensionsProfit = PayPensionsProfit(
        totalSelfEmploymentProfit = None,
        totalPropertyProfit = None,
        incomeTaxAmount = Some(66500),
        taxableIncome = Some(170000),
        bands = List(TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 100000.00,
            taxAmount = 40000.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 45.0,
            income = 50000.00,
            taxAmount = 22500.00,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      savingsAndGains = SavingsAndGains(
        Some(0),
        Some(500),
        Some(0),
        Some(500),
        List(
          TaxBand(
            name = "SSR",
            rate = 0.0,
            income = 1.00,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "BRT",
            rate = 10.0,
            income = 20.00,
            taxAmount = 2.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTBR",
            rate = 0.0,
            income = 20.0,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 2000.0,
            taxAmount = 800.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTHR",
            rate = 0.0,
            income = 10000.0,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 50.0,
            income = 100000.0,
            taxAmount = 5000.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      reductionsAndCharges = ReductionsAndCharges(
        giftAidTax = Some(5000),
        totalPensionSavingsTaxCharges = Some(5000),
        statePensionLumpSumCharges = Some(5000),
        totalStudentLoansRepaymentAmount = Some(5000),
        totalResidentialFinanceCostsRelief = Some(5000),
        totalForeignTaxCreditRelief = Some(6000),
        totalNotionalTax = Some(7000),
        incomeTaxDueAfterTaxReductions = Some(2000),
        reliefsClaimed = Some(Seq(ReliefsClaimed("deficiencyRelief", Some(1000)), ReliefsClaimed("vctSubscriptions", Some(2000)),
          ReliefsClaimed("eisSubscriptions", Some(3000)), ReliefsClaimed("seedEnterpriseInvestment", Some(4000)),
          ReliefsClaimed("communityInvestment", Some(5000)), ReliefsClaimed("socialEnterpriseInvestment", Some(6000)),
          ReliefsClaimed("maintenancePayments", Some(7000)),
          ReliefsClaimed("qualifyingDistributionRedemptionOfSharesAndSecurities", Some(8000)),
          ReliefsClaimed("nonDeductibleLoanInterest", Some(9000))
        ))
      ),
      dividends = Dividends(
        incomeTaxAmount = Some(5000),
        taxableIncome = Some(6000),
        bands = List(
          TaxBand(
            name = "BRT",
            rate = 7.5,
            income = 1000,
            taxAmount = 75.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTBR",
            rate = 0,
            income = 1000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "HRT",
            rate = 37.5,
            income = 2000,
            taxAmount = 750.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTHR",
            rate = 0,
            income = 2000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ART",
            rate = 38.1,
            income = 3000,
            taxAmount = 1143.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000),
          TaxBand(
            name = "ZRTAR",
            rate = 0,
            income = 3000,
            taxAmount = 0.0,
            bandLimit = 15000,
            apportionedBandLimit = 15000)
        )
      ),
      allowancesAndDeductions = AllowancesAndDeductions(
        personalAllowance = Some(11500)
      ),
      nic = Nic(
        class2 = Some(10000.00),
        class4 = Some(14000.00),
        class4Bands = Some(Seq(
          NicBand(
            name = "ZRT",
            rate = 1,
            income = 2000,
            amount = 100
          ),
          NicBand(
            name = "BRT",
            rate = 2,
            income = 3000,
            amount = 200
          ),
          NicBand(
            name = "HRT",
            rate = 3,
            income = 5000,
            amount = 300
          ),
          NicBand(
            name = "giftAidTax",
            rate = 4,
            income = 6000,
            amount = 400
          ),
          NicBand(
            name = "totalPensionSavingsTaxCharges",
            rate = 5,
            income = 7000,
            amount = 500
          ),
          NicBand(
            name = "statePensionLumpSumCharges",
            rate = 6,
            income = 8000,
            amount = 600
          ),
          NicBand(
            name = "totalStudentLoansRepaymentAmount",
            rate = 7,
            income = 9000,
            amount = 700
          ),
        )),
        totalNic = Some(24000.00)
      ),
      taxDeductedAtSource = TaxDeductedAtSource(
        Some(100.0),
        Some(200.0),
        Some(300.0),
        Some(400.0),
        Some(500.0),
        Some(600.0),
        Some(700.0),
        Some(800.0),
        Some(900.0),
        Some(1000.0)
      )
    )


  val errorCalculationModel: CalculationErrorModel =
    CalculationErrorModel(
      Status.INTERNAL_SERVER_ERROR,
      "Internal server error"
    )

  val calculationAllIncomeSources = Calculation(
    totalIncomeReceived = Some(10010.10),
    crystallised = false,
    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = Some(1001.01),
      totalPropertyProfit = Some(2002.02),
      totalPayeEmploymentAndLumpSumIncome = Some(5005.05),
      totalBenefitsInKind = Some(6006.06),
      totalEmploymentExpenses = Some(7007.07),
      totalOccupationalPensionIncome = Some(8008.08),
      totalStateBenefitsIncome = Some(9009.09),
      totalFHLPropertyProfit = Some(6003),
      totalForeignPropertyProfit = Some(6004),
      totalEeaFhlProfit = Some(6005),
      totalOverseasPensionsStateBenefitsRoyalties = Some(6006.00),
      totalAllOtherIncomeReceivedWhilstAbroad = Some(6007.00),
      totalOverseasIncomeAndGains = Some(6008.00),
      totalForeignBenefitsAndGifts = Some(6009.00),
      totalShareSchemesIncome = Some(6010.00)
    ),
    savingsAndGains = SavingsAndGains(
      taxableIncome = Some(3003.03),
      totalForeignSavingsAndGainsIncome = Some(7019),
      totalOfAllGains = Some(7015)
    ),
    dividends = Dividends(
      taxableIncome = Some(4004.04),
      totalForeignDividends = Some(7026)
    )
  )

  val calculationAllDeductionSources = Calculation(
    crystallised = false,
    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(11500),
      totalPensionContributions = Some(12500),
      lossesAppliedToGeneralIncome = Some(12500),
      giftOfInvestmentsAndPropertyToCharity = Some(10000),
      totalAllowancesAndDeductions = Some(47000),
      totalReliefs = Some(500),
      grossAnnualPayments = Some(1000),
      qualifyingLoanInterestFromInvestments = Some(1001),
      postCessationTradeReceipts = Some(1002),
      paymentsToTradeUnionsForDeathBenefits = Some(1003)
    )

  )

}
