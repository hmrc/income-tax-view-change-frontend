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

import assets.BaseIntegrationTestConstants.{testCalcId, testCalcId2}
import enums.Estimate
import models.calculation._
import play.api.libs.json.{JsObject, JsValue, Json}

object CalcDataIntegrationTestConstants {

  val calculationDataSuccessWithEoYModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 90500.00,
    totalTaxableIncome = 198500.00,
    personalAllowance = 11500.00,
    taxReliefs = 1000,
    totalIncomeAllowancesUsed = 12005.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 200000.00,
      ukProperty = 10000.00,
      bankBuildingSocietyInterest = 2000.00,
      ukDividends = 11000.00
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
        taxableIncome = 500.0,
        taxRate = 20.0,
        taxAmount = 100.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 1000.0,
        taxRate = 40.0,
        taxAmount = 400.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 479.0,
        taxRate = 45.0,
        taxAmount = 215.55,
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
      paymentsMade = 150,
      rate = 0,
      taxableAmount = 0
    ),
    eoyEstimate = Some(EoyEstimate(25000.00)),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 20000.00, 4000.00),
      TaxBandModel("HRT", 40.0, 100000.00, 40000.00),
      TaxBandModel("ART", 45.0, 50000.00, 22500.00)
    )
  )

  val calculationDataSuccessModel = CalculationDataModel(
    totalIncomeTaxNicYtd = 90500.00,
    totalTaxableIncome = 198500.00,
    personalAllowance = 11500.00,
    taxReliefs = 1000,
    totalIncomeAllowancesUsed = 12005.00,
    incomeReceived = IncomeReceivedModel(
      selfEmployment = 200000.00,
      ukProperty = 10000.00,
      bankBuildingSocietyInterest = 2000.00,
      ukDividends = 11000.00
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
        taxableIncome = 500.0,
        taxRate = 20.0,
        taxAmount = 100.0,
        name = "BRT"
      ),
      BandModel(
        taxableIncome = 1000.0,
        taxRate = 40.0,
        taxAmount = 400.0,
        name = "HRT"
      ),
      BandModel(
        taxableIncome = 479.0,
        taxRate = 45.0,
        taxAmount = 215.55,
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
      paymentsMade = 150,
      rate = 0.0,
      taxableAmount = 1.5
    ),
    payAndPensionsProfitBands = List(
      TaxBandModel("BRT", 20.0, 20000.00, 4000.00),
      TaxBandModel("HRT", 40.0, 100000.00, 40000.00),
      TaxBandModel("ART", 45.0, 50000.00, 22500.00)
    )
  )

  val calculationDataErrorModel = CalculationDataErrorModel(code = 500, message = "Calculation Error Model Response")

  val calculationDataSuccessWithEoyJson: JsValue = {
    Json.obj(
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
            "payPensionsProfit" -> Json.obj(
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
              )),
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
                  "income" -> 500,
                  "taxAmount" -> 100,
                  "threshold" -> 1,
                  "apportionedThreshold" -> 1
                ),
                Json.obj(
                  "name" -> "HRT",
                  "rate" -> 40,
                  "income" -> 1000,
                  "taxAmount" -> 400,
                  "threshold" -> 1,
                  "apportionedThreshold" -> 1
                ),
                Json.obj(
                  "name" -> "ART",
                  "rate" -> 45,
                  "income" -> 479,
                  "taxAmount" -> 215.55,
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
              "bbsiIncome" -> 2000,
              "ukDividendIncome" -> 11000,
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
        )
      )
    )
  }

  val calculationDataSuccessJson: JsValue = Json.obj(
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
          "giftAid" -> Json.obj(
            "paymentsMade" -> 150,
            "rate" -> 1.0,
            "taxableIncome" -> 1.5
          ),
          "totalAllowancesAndReliefs" -> 0,
          "payPensionsProfit" -> Json.obj(
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
            )),
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
                "income" -> 500,
                "taxAmount" -> 100,
                "threshold" -> 1,
                "apportionedThreshold" -> 1
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 1000,
                "taxAmount" -> 400,
                "threshold" -> 1,
                "apportionedThreshold" -> 1
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 479,
                "taxAmount" -> 215.55,
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
            "bbsiIncome" -> 2000,
            "ukDividendIncome" -> 11000,
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
        )
      )
    )
  )


  val taxCalculationResponse: JsObject = Json.obj(
    "calcOutput" -> Json.obj(
      "calcID" -> testCalcId,
      "calcAmount" -> 90500.00,
      "calcTimestamp" -> "2017-07-06T12:34:56.789Z",

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
          "giftAid" -> Json.obj(
            "paymentsMade" -> 150,
            "rate" -> 1.0,
            "taxableIncome" -> 1.5
          ),
          "totalAllowancesAndReliefs" -> 0,
          "payPensionsProfit" -> Json.obj(
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
            )),
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
                "income" -> 500,
                "taxAmount" -> 100,
                "threshold" -> 1,
                "apportionedThreshold" -> 1
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 1000,
                "taxAmount" -> 400,
                "threshold" -> 1,
                "apportionedThreshold" -> 1
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 479,
                "taxAmount" -> 215.55,
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
            "bbsiIncome" -> 2000,
            "ukDividendIncome" -> 11000,
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
        )
      )
    )
  )

  val taxCalculationResponseWithEOY: JsObject = Json.obj(
    "calcOutput" -> Json.obj(
      "calcID" -> testCalcId,
      "calcAmount" -> 90500.00,
      "calcTimestamp" -> "2017-07-06T12:34:56.789Z",

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
          "payPensionsProfit" -> Json.obj(
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
            )),
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
                "income" -> 500,
                "taxAmount" -> 100,
                "threshold" -> 1,
                "apportionedThreshold" -> 1
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 1000,
                "taxAmount" -> 400,
                "threshold" -> 1,
                "apportionedThreshold" -> 1
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 479,
                "taxAmount" -> 215.55,
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
            "bbsiIncome" -> 2000,
            "ukDividendIncome" -> 11000,
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
      )
    )
  )

  val taxCalculationCrystallisedResponse: JsObject = Json.obj(
    "calcOutput" -> Json.obj(
      "calcID" -> testCalcId,
      "calcAmount" -> 90500.00,
      "calcTimestamp" -> "2017-07-06T12:34:56.789Z",
      "crystallised" -> true,

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
          "payPensionsProfit" -> Json.obj(
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
            )),
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
                "income" -> 500,
                "taxAmount" -> 100,
                "threshold" -> 1,
                "apportionedThreshold" -> 1
              ),
              Json.obj(
                "name" -> "HRT",
                "rate" -> 40,
                "income" -> 1000,
                "taxAmount" -> 400,
                "threshold" -> 1,
                "apportionedThreshold" -> 1
              ),
              Json.obj(
                "name" -> "ART",
                "rate" -> 45,
                "income" -> 479,
                "taxAmount" -> 215.55,
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
            "bbsiIncome" -> 2000,
            "ukDividendIncome" -> 11000,
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
      )

    )
  )

  val latestCalcModelJson: JsObject = Json.obj(
    "calcOutput" -> Json.obj(
      "calcID" -> "CALCID",
      "calcAmount" -> 543.21,
      "calcTimestamp" -> "2017-07-06T12:34:56.789Z",
      "incomeTaxNicYtd" -> 123.45,
      "incomeTaxNicAmount" -> 987.65
    )
  )

  val estimateLatestTaxCalcResponse =
    CalculationModel(testCalcId,
      Some(90500.00),
      Some("2017-07-06T12:34:56.789Z"),
      None,
      None,
      None
    )

  val estimateLatestTaxCalcResponse2 =
    CalculationModel(
      testCalcId2,
      Some(90500.00),
      Some("2017-07-06T12:34:56.789Z"),
      None,
      None,
      None
    )

  val latestCalcModel: CalculationModel =
    CalculationModel(
      "CALCID",
      Some(543.21),
      Some("2017-07-06T12:34:56.789Z"),
      None,
      Some(123.45),
      Some(987.65)
    )
}