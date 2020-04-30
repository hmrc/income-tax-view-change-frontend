/*
 * Copyright 2020 HM Revenue & Customs
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
import enums.{Crystallised, Estimate}
import models.calculation._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}

object CalcBreakdownTestConstants {

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
        taxAmount = 4000.00),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00)
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
        taxAmount = 2000.00),
        TaxBand(
        name = "BRT",
        rate = 20.0,
        income = 20000.00,
        taxAmount = 4000.00),
        TaxBand(
          name = "IRT",
          rate = 25.0,
          income = 20000.00,
          taxAmount = 45000.00),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00)
      )
    )
  )

  val calculationDataSuccessModel = Calculation(
    totalIncomeTaxAndNicsDue = Some(2010.00),
    totalIncomeTaxNicsCharged = Some(90500.00),
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
        taxAmount = 4000.00

      ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00

        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00

        ))
    ),

    savingsAndGains = SavingsAndGains(
      Some(0),
      Some(0),
      List(TaxBand(
        name = "SSR",
        rate = 0.0,
        income = 1.00,
        taxAmount = 0.0

      ),
        TaxBand(
          name = "ZRT",
          rate = 0.0,
          income = 20.00,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 0.0,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.0,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.0,
          taxAmount = 0.0

        ))
    ),

    dividends = Dividends(
      incomeTaxAmount = Some(5000),
      taxableIncome = Some(6000),
      List(TaxBand(
        name = "basic-band",
        rate = 7.5,
        income = 1000,
        taxAmount = 75.0
      ),
        TaxBand(
          name = "higher-band",
          rate = 37.5,
          income = 2000,
          taxAmount = 750.0
        ),
        TaxBand(
          name = "additional-band",
          rate = 38.1,
          income = 3000,
          taxAmount = 1143.0
        )
      )
    ),

    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(11500)
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
        taxAmount = 2000.00

      ),

        TaxBand(
        name = "BRT",
        rate = 20.0,
        income = 20000.00,
        taxAmount = 4000.00

      ),

        TaxBand(
          name = "IRT",
          rate = 25.0,
          income = 20000.00,
          taxAmount = 45000.00

        ),

        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00

        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 500000.00,
          taxAmount = 22500.00

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
        taxAmount = 4000.00

      ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00

        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00

        ))
    ),

    savingsAndGains = SavingsAndGains(
      Some(0),
      Some(0),
      List(TaxBand(
        name = "SSR",
        rate = 0.0,
        income = 1.00,
        taxAmount = 0.0

      ),
        TaxBand(
          name = "ZRT",
          rate = 0.0,
          income = 20.00,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 0.0,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.0,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.0,
          taxAmount = 0.0

        ))
    ),

    dividends = Dividends(
      incomeTaxAmount = Some(5000),
      taxableIncome = Some(6000),
      List(TaxBand(
        name = "basic-band",
        rate = 7.5,
        income = 1000,
        taxAmount = 75.0
      ),
        TaxBand(
          name = "higher-band",
          rate = 37.5,
          income = 2000,
          taxAmount = 750.0
        ),
        TaxBand(
          name = "additional-band",
          rate = 38.1,
          income = 3000,
          taxAmount = 1143.0
        )
      )
    ),

    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(2868.00),
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
      List(TaxBand(
        name = "BRT",
        rate = 20.0,
        income = 20000.00,
        taxAmount = 4000.00

      ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 100000.00,
          taxAmount = 40000.00

        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 50000.00,
          taxAmount = 22500.00

        ))
    ),
    savingsAndGains = SavingsAndGains(
      Some(0),
      Some(0),
      List(TaxBand(
        name = "SSR",
        rate = 0.0,
        income = 0.00,
        taxAmount = 0.0

      ),
        TaxBand(
          name = "ZRT",
          rate = 0.0,
          income = 0.00,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 0.0,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.0,
          taxAmount = 0.0

        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.0,
          taxAmount = 0.0

        ))
    ),
    dividends = Dividends(
      incomeTaxAmount = Some(5000),
      taxableIncome = Some(6000),
      List(TaxBand(
        name = "basic-band",
        rate = 7.5,
        income = 1000,
        taxAmount = 75.0
      ),
        TaxBand(
          name = "higher-band",
          rate = 37.5,
          income = 2000,
          taxAmount = 750.0
        ),
        TaxBand(
          name = "additional-band",
          rate = 38.1,
          income = 3000,
          taxAmount = 1143.0
        )
      )
    ),
    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(2868.00),
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
        taxAmount = 26.00
        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.00,
          taxAmount = 0.00

        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.00,
          taxAmount = 0.00

        ))
    ),
    savingsAndGains = SavingsAndGains(
      Some(0),
      Some(0),
      List(
        TaxBand(
        name = "SSR",
        rate = 0.0,
        income = 0.00,
        taxAmount = 0.0
        ),
        TaxBand(
          name = "ZRT",
          rate = 0.0,
          income = 0.00,
          taxAmount = 0.0
        ),
        TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 0.0,
          taxAmount = 0.0
        ),
        TaxBand(
          name = "HRT",
          rate = 40.0,
          income = 0.0,
          taxAmount = 0.0
        ),
        TaxBand(
          name = "ART",
          rate = 45.0,
          income = 0.0,
          taxAmount = 0.0
        ))
    ),
    dividends = Dividends(
      incomeTaxAmount = Some(5000),
      taxableIncome = Some(6000),
      List(
        TaxBand(
        name = "basic-band",
        rate = 7.5,
        income = 1000,
        taxAmount = 75.0),
        TaxBand(
          name = "higher-band",
          rate = 37.5,
          income = 2000,
          taxAmount = 750.0),
        TaxBand(
          name = "additional-band",
          rate = 38.1,
          income = 3000,
          taxAmount = 1143.0
        )
      )
    ),
    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(2868.00),
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
          )
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
        List(TaxBand(
          name = "BRT",
          rate = 20.0,
          income = 20000.00,
          taxAmount = 4000.00),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 100000.00,
            taxAmount = 40000.00),
          TaxBand(
            name = "ART",
            rate = 45.0,
            income = 50000.00,
            taxAmount = 22500.00)
          )
      ),
      savingsAndGains = SavingsAndGains(
        Some(0),
        Some(500),
        List(
          TaxBand(
          name = "SSR",
          rate = 0.0,
          income = 1.00,
          taxAmount = 0.0),
          TaxBand(
            name = "BRT",
            rate = 10.0,
            income = 20.00,
            taxAmount = 2.0),
          TaxBand(
            name = "ZRTBR",
            rate = 0.0,
            income = 20.0,
            taxAmount = 0.0),
          TaxBand(
            name = "HRT",
            rate = 40.0,
            income = 2000.0,
            taxAmount = 800.0),
          TaxBand(
            name = "ZRTHR",
            rate = 0.0,
            income = 10000.0,
            taxAmount = 0.0),
          TaxBand(
            name = "ART",
            rate = 50.0,
            income = 100000.0,
            taxAmount = 5000.0)
        )
      ),
      dividends = Dividends(
        incomeTaxAmount = Some(5000),
        taxableIncome = Some(6000),
        List(
          TaxBand(
            name = "BRT",
            rate = 7.5,
            income = 1000,
            taxAmount = 75.0),
          TaxBand(
            name = "ZRTBR",
            rate = 0,
            income = 1000,
            taxAmount = 0.0),
          TaxBand(
            name = "HRT",
            rate = 37.5,
            income = 2000,
            taxAmount = 750.0),
          TaxBand(
            name = "ZRTHR",
            rate = 0,
            income = 2000,
            taxAmount = 0.0),
          TaxBand(
            name = "ART",
            rate = 38.1,
            income = 3000,
            taxAmount = 1143.0),
          TaxBand(
            name = "ZRTAR",
            rate = 0,
            income = 3000,
            taxAmount = 0.0)
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
            rate = 1,
            income = 2000,
            amount = 100
          ),
          NicBand(
            rate = 2,
            income = 3000,
            amount = 200
          ),
          NicBand(
            rate = 3,
            income = 5000,
            amount = 300
          )
        )),
        totalNic = Some(24000.00)
      ),
      savings = Some(10000)
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
      totalPropertyProfit = Some(2002.02)
    ),
    savingsAndGains = SavingsAndGains(
      taxableIncome = Some(3003.03)
    ),
    dividends = Dividends(
      taxableIncome = Some(4004.04)
    )
  )

  val calculationAllDeductionSources = Calculation(
    crystallised = false,
    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(11500),
      giftOfInvestmentsAndPropertyToCharity = Some(10000),
      totalAllowancesAndDeductions = Some (21000),
      totalReliefs = Some(500)
    )

  )

}
