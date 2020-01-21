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

  val calculationDataSuccessModel = Calculation(
    totalIncomeTaxAndNicsDue = Some(0),
    totalIncomeTaxNicsCharged = Some(90500.00),
    totalTaxableIncome = Some(198500.00),
    incomeTaxNicAmount = None,
    timestamp = Some(testTimeStampString),
    crystallised = true,
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
    ),
    giftAid = GiftAid(
      payments = Some(0),
      rate = Some(0),
      giftAidTax = Some(0)
    )
  )

  val scottishBandModelJustSRT = Calculation(
    totalIncomeTaxAndNicsDue = Some(0),
    totalIncomeTaxNicsCharged = Some(149.86),
    totalTaxableIncome = Some(132.00),
    incomeTaxNicAmount = None,
    timestamp = None,
    crystallised = true,
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
      personalAllowance = Some(2868.00),
      giftOfInvestmentsAndPropertyToCharity = Some(1000.25),
      totalAllowancesAndDeductions = Some(24.90),
      totalReliefs = Some(120)
    ),
    nic = Nic(
      class2 = Some(10000.00),
      class4 = Some(14000.00),
      totalNic = Some(66000.00)
    ),
    giftAid = GiftAid(
      payments = Some(0),
      rate = Some(0),
      giftAidTax = Some(0)
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
    ),
    giftAid = GiftAid(
      payments = Some(0),
      rate = Some(0),
      giftAidTax = Some(0)
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
      List(TaxBand(
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
    ),
    giftAid = GiftAid(
      payments = Some(900),
      rate = Some(0),
      giftAidTax = Some(0)
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
          "personalAllowance" -> 0,
          "giftAidExtender" -> 0
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
        "giftAid" -> Json.obj(
          "paymentsMade" -> 0,
          "rate" -> 0,
          "taxableAmount" -> 0
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
      ),
      giftAid = GiftAid(
        payments = Some(0),
        rate = Some(0),
        giftAidTax = Some(0)
      )
    )
  val errorCalculationModel: CalculationErrorModel =
    CalculationErrorModel(
      Status.INTERNAL_SERVER_ERROR,
      "Internal server error"
    )

}
