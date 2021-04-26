/*
 * Copyright 2017 HM Revenue & Customs
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

import models.calculation.{AllowancesAndDeductions, Calculation, Dividends, Nic, PayPensionsProfit, ReductionsAndCharges, SavingsAndGains, TaxBand}
import play.api.libs.json.{JsObject, Json}

object CalcDataIntegrationTestConstants {

  val crystallisedCalculationEmptyJson: JsObject = Json.obj(
    "metadata" -> Json.obj(
      "crystallised" -> true
    )
  )

  val estimateCalculationEmptyJson: JsObject = Json.obj(
    "metadata" -> Json.obj(
      "crystallised" -> false
    )
  )

  val crystallisedCalculationFullJson: JsObject = Json.obj(
    "incomeTaxAndNicsCalculated" -> Json.obj(
      "summary" -> Json.obj(
        "totalIncomeTaxAndNicsDue" -> 90500,
        "totalIncomeTaxNicsCharged" -> 2.0,
        "taxRegime" -> "Scotland",
        "nics" -> Json.obj(
          "class2NicsAmount" -> 10000,
          "class4NicsAmount" -> 14000,
          "totalNic" -> 3.0
        )
      ),
      "detail" -> Json.obj(
        "incomeTax" -> Json.obj(
          "payPensionsProfit" -> Json.obj(
            "incomeTaxAmount" -> 3.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "BRT",
                "rate" -> 20,
                "income" -> 20000,
                "taxAmount" -> 4000
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 100000,
                "taxAmount" -> 40000
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 50000,
                "taxAmount" -> 22500
              )
            )
          ),
          "savingsAndGains" -> Json.obj(
            "incomeTaxAmount" -> 1.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "SSR",
                "rate" -> 0,
                "income" -> 1,
                "taxAmount" -> 0
              ),
              Json.obj(
                "name" -> "ZRT",
                "rate" -> 0,
                "income" -> 20,
                "taxAmount" -> 0
              ),
              Json.obj(
                "name" -> "BRT",
                "rate" -> 20,
                "income" -> 500,
                "taxAmount" -> 100
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 1000,
                "taxAmount" -> 400
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 479,
                "taxAmount" -> 215.55
              )
            )
          ),
          "dividends" -> Json.obj(
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "zero",
                "rate" -> 0,
                "income" -> 500,
                "taxAmount" -> 0
              ),
                Json.obj(
              "name" -> "basic",
              "rate" -> 7.5,
              "income" -> 1000,
              "taxAmount" -> 75
            ),
            Json.obj(
              "name" -> "higher",
              "rate" -> 37.5,
              "income" -> 2000,
              "taxAmount" -> 750
            ),
              Json.obj(
              "name" -> "additional",
              "rate" -> 38.1,
              "income" -> 3000,
              "taxAmount" -> 1143
            )
          )
        )
      )
    )
  )
  ,
  "taxableIncome" -> Json.obj(
    "summary" -> Json.obj(
      "totalTaxableIncome" -> 198500,
      "totalIncomeReceivedFromAllSources" -> 199505
    ),
    "detail" -> Json.obj(
      "payPensionsProfit" -> Json.obj(
        "totalSelfEmploymentProfit" -> 200000,
        "totalPropertyProfit" -> 10000,
        "taxableIncome" -> 4.0
      ),
      "savingsAndGains" -> Json.obj(
        "taxableIncome" -> 2000
      ),
      "dividends" -> Json.obj(
        "taxableIncome" -> 11000
      )
    )
  )
  ,
  "endOfYearEstimate" -> Json.obj(
    "summary" -> Json.obj(
      "incomeTaxNicAmount" -> 66000
    )
  )
  ,
  "metadata" -> Json.obj(
    "calculationTimestamp" -> "2017-07-06T12:34:56.789Z",
    "crystallised" -> true
  )
  ,
  "allowancesDeductionsAndReliefs" -> Json.obj(
    "summary" -> Json.obj(
      "totalAllowancesAndDeductions" ->100,
      "totalReliefs" -> 400
    ),
    "detail" -> Json.obj(
      "allowancesAndDeductions" -> Json.obj(
        "personalAllowance" -> 11500,
        "giftOfInvestmentsAndPropertyToCharity" -> 1000.25
      )
    )
  )
  )

  val estimatedCalculationFullJson: JsObject = Json.obj(
    "incomeTaxAndNicsCalculated" -> Json.obj(
      "summary" -> Json.obj(
        "totalIncomeTaxAndNicsDue" -> 90500,
        "totalIncomeTaxNicsCharged" -> 2.0,
        "taxRegime" -> "Scotland",
        "nics" -> Json.obj(
          "class2NicsAmount" -> 10000,
          "class4NicsAmount" -> 14000,
          "totalNic" -> 3.0
        )
      ),
      "detail" -> Json.obj(
        "incomeTax" -> Json.obj(
          "payPensionsProfit" -> Json.obj(
            "incomeTaxAmount" -> 3.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "BRT",
                "rate" -> 20,
                "income" -> 20000,
                "taxAmount" -> 4000
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 100000,
                "taxAmount" -> 40000
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 50000,
                "taxAmount" -> 22500
              )
            )
          ),
          "savingsAndGains" -> Json.obj(
            "incomeTaxAmount" -> 1.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "SSR",
                "rate" -> 0,
                "income" -> 1,
                "taxAmount" -> 0
              ),
              Json.obj(
                "name" -> "ZRT",
                "rate" -> 0,
                "income" -> 20,
                "taxAmount" -> 0
              ),
              Json.obj(
                "name" -> "BRT",
                "rate" -> 20,
                "income" -> 500,
                "taxAmount" -> 100
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 1000,
                "taxAmount" -> 400
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 479,
                "taxAmount" -> 215.55
              )
            )
          ),
          "dividends" -> Json.obj(
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "zero",
                "rate" -> 0,
                "income" -> 500,
                "taxAmount" -> 0
              ),
              Json.obj(
                "name" -> "basic",
                "rate" -> 7.5,
                "income" -> 1000,
                "taxAmount" -> 75
              ),
              Json.obj(
                "name" -> "higher",
                "rate" -> 37.5,
                "income" -> 2000,
                "taxAmount" -> 750
              ),
              Json.obj(
                "name" -> "additional",
                "rate" -> 38.1,
                "income" -> 3000,
                "taxAmount" -> 1143
              )
            )
          )
        )
      )
    ),
    "taxableIncome" -> Json.obj(
      "summary" -> Json.obj(
        "totalTaxableIncome" -> 198500,
        "totalIncomeReceivedFromAllSources" -> 199505
      ),
      "detail" -> Json.obj(
        "payPensionsProfit" -> Json.obj(
          "totalSelfEmploymentProfit" -> 200000,
          "totalPropertyProfit" -> 10000,
          "taxableIncome" -> 4.0
        ),
        "savingsAndGains" -> Json.obj(
          "taxableIncome" -> 2000
        ),
        "dividends" -> Json.obj(
          "taxableIncome" -> 11000
        )
      )
    ),
    "endOfYearEstimate" -> Json.obj(
      "summary" -> Json.obj(
        "incomeTaxNicAmount" -> 66000
      )
    ),
    "metadata" -> Json.obj(
      "calculationTimestamp" -> "2017-07-06T12:34:56.789Z",
      "crystallised" -> false
    ),
    "allowancesDeductionsAndReliefs" -> Json.obj(
      "summary" -> Json.obj(
        "totalAllowancesAndDeductions" ->100,
        "totalReliefs" -> 400
      ),
      "detail" -> Json.obj(
        "allowancesAndDeductions" -> Json.obj(
          "personalAllowance" -> 11500,
          "giftOfInvestmentsAndPropertyToCharity" -> 1000.25
        )
      )
    )
  )

  val estimatedNoEOYEstimateCalculationFullJson: JsObject = Json.obj(
    "incomeTaxAndNicsCalculated" -> Json.obj(
      "summary" -> Json.obj(
        "totalIncomeTaxAndNicsDue" -> 90500,
        "totalIncomeTaxNicsCharged" -> 2.0,
        "taxRegime" -> "Scotland",
        "nics" -> Json.obj(
          "class2NicsAmount" -> 10000,
          "class4NicsAmount" -> 14000,
          "totalNic" -> 3.0
        )
      ),
      "detail" -> Json.obj(
        "incomeTax" -> Json.obj(
          "payPensionsProfit" -> Json.obj(
            "incomeTaxAmount" -> 3.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "BRT",
                "rate" -> 20,
                "income" -> 20000,
                "taxAmount" -> 4000
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 100000,
                "taxAmount" -> 40000
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 50000,
                "taxAmount" -> 22500
              )
            )
          ),
          "savingsAndGains" -> Json.obj(
            "incomeTaxAmount" -> 1.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "SSR",
                "rate" -> 0,
                "income" -> 1,
                "taxAmount" -> 0
              ),
              Json.obj(
                "name" -> "ZRT",
                "rate" -> 0,
                "income" -> 20,
                "taxAmount" -> 0
              ),
              Json.obj(
                "name" -> "BRT",
                "rate" -> 20,
                "income" -> 500,
                "taxAmount" -> 100
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 1000,
                "taxAmount" -> 400
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 479,
                "taxAmount" -> 215.55
              )
            )
          ),
          "dividends" -> Json.obj(
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "zero",
                "rate" -> 0,
                "income" -> 500,
                "taxAmount" -> 0
              ),
              Json.obj(
                "name" -> "basic",
                "rate" -> 7.5,
                "income" -> 1000,
                "taxAmount" -> 75
              ),
              Json.obj(
                "name" -> "higher",
                "rate" -> 37.5,
                "income" -> 2000,
                "taxAmount" -> 750
              ),
              Json.obj(
                "name" -> "additional",
                "rate" -> 38.1,
                "income" -> 3000,
                "taxAmount" -> 1143
              )
            )
          )
        )
      )
    )
    ,
    "taxableIncome" -> Json.obj(
      "summary" -> Json.obj(
        "totalTaxableIncome" -> 198500
      ),
      "detail" -> Json.obj(
        "payPensionsProfit" -> Json.obj(
          "totalSelfEmploymentProfit" -> 200000,
          "totalPropertyProfit" -> 10000,
          "taxableIncome" -> 4.0
        ),
        "savingsAndGains" -> Json.obj(
          "taxableIncome" -> 2000
        ),
        "dividends" -> Json.obj(
          "taxableIncome" -> 11000
        )
      )
    ),
    "metadata" -> Json.obj(
      "calculationTimestamp" -> "2017-07-06T12:34:56.789Z",
      "crystallised" -> false
    )
    ,
    "allowancesDeductionsAndReliefs" -> Json.obj(
      "summary" -> Json.obj(
        "totalAllowancesAndDeductions" ->100,
        "totalReliefs" -> 400
      ),
      "detail" -> Json.obj(
        "allowancesAndDeductions" -> Json.obj(
          "personalAllowance" -> 11500,
          "giftOfInvestmentsAndPropertyToCharity" -> 1000.25
        )
      )
    )
  )

  val calculationDataSuccessModel = Calculation(
    totalIncomeTaxAndNicsDue = Some(90500.00),
    totalIncomeTaxNicsCharged = Some(90500.00),
    totalIncomeReceived = Some(199505.00),
    totalTaxableIncome = Some(198500.00),
    incomeTaxNicAmount = None,
    timestamp = Some("2017-07-06T12:34:56.789Z"),
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

}
