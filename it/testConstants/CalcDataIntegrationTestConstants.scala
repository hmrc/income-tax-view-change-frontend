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
package testConstants

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
        "totalAllowancesAndDeductions" -> 100,
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
        "incomeTax" -> Json.obj(
          "totalPensionSavingsTaxCharges" -> 5000.99,
          "statePensionLumpSumCharges" -> 5000.99),
        "nics" -> Json.obj(
          "class2NicsAmount" -> 10000,
          "class4NicsAmount" -> 14000,
          "totalNic" -> 3.0
        )
      ),
      "detail" -> Json.obj(
        "nics" -> Json.obj(
          "class2Nics" -> Json.obj(
            "weeklyRate" -> 3.05,
            "weeks" -> 52,
            "limit" -> 6475,
            "apportionedLimit" -> 6475,
            "underSmallProfitThreshold" -> false,
            "actualClass2Nic" -> false,
            "class2VoluntaryContributions" -> true)
        ),
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
          "giftAid" -> Json.obj(
            "giftAidTax" -> 5000.99
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
        "totalAllowancesAndDeductions" -> 100,
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

  val giftAidCalculationJson: JsObject = Json.obj(
    "incomeTaxAndNicsCalculated" -> Json.obj(
      "summary" -> Json.obj(
        "totalIncomeTaxAndNicsDue" -> 90500,
        "totalIncomeTaxNicsCharged" -> 2.0,
        "taxRegime" -> "Scotland",
        "incomeTax" -> Json.obj(),
        "nics" -> Json.obj()
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
              )
            )
          ),
          "giftAid" -> Json.obj(
            "giftAidTax" -> 5000.99
          ),
          "dividends" -> Json.obj(
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "zero",
                "rate" -> 0,
                "income" -> 500,
                "taxAmount" -> 0
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
        "totalAllowancesAndDeductions" -> 100,
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


  "statePensionLumpSumCharges" -> 5000.99

  val pensionSavingsCalculationJson: JsObject = Json.obj(
    "incomeTaxAndNicsCalculated" -> Json.obj(
      "summary" -> Json.obj(
        "totalIncomeTaxAndNicsDue" -> 90500,
        "totalIncomeTaxNicsCharged" -> 2.0,
        "incomeTax" -> Json.obj(
          "totalPensionSavingsTaxCharges" -> 5000.99),
        "taxRegime" -> "Scotland",
        "nics" -> Json.obj()
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
        "totalAllowancesAndDeductions" -> 100,
        "totalReliefs" -> 400
      ),
      "detail" -> Json.obj(
        "allowancesAndDeductions" -> Json.obj(
          "personalAllowance" -> 11500,
          "giftOfInvestmentsAndPropertyToCharity" -> 1000.25
        )
      )
    ))

  val pensionLumpSumCalculationJson: JsObject = Json.obj(
    "incomeTaxAndNicsCalculated" -> Json.obj(
      "summary" -> Json.obj(
        "totalIncomeTaxAndNicsDue" -> 90500,
        "totalIncomeTaxNicsCharged" -> 2.0,
        "taxRegime" -> "Scotland",
        "incomeTax" -> Json.obj("statePensionLumpSumCharges" -> 5000.99),
        "nics" -> Json.obj()
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
        "totalAllowancesAndDeductions" -> 100,
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

  val minimalCalculationJson: JsObject = Json.obj(
    "incomeTaxAndNicsCalculated" -> Json.obj(
      "summary" -> Json.obj(
        "totalIncomeTaxAndNicsDue" -> 90500,
        "totalIncomeTaxNicsCharged" -> 2.0,
        "taxRegime" -> "Scotland",
        "incomeTax" -> Json.obj(),
        "nics" -> Json.obj()
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
        "totalAllowancesAndDeductions" -> 100,
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
        "totalAllowancesAndDeductions" -> 100,
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

}
