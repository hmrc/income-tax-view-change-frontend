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

package models.calculation

import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.{JsObject, JsSuccess, Json}

class CalculationSpec extends WordSpecLike with MustMatchers {

  val fullModel: Calculation = Calculation(
    totalIncomeTaxAndNicsDue = Some(1.0),
    totalIncomeTaxNicsCharged = Some(2.0),
    totalTaxableIncome = Some(3.0),
    incomeTaxNicAmount = Some(4.0),
    timestamp = Some("timestamp"),
    crystallised = true,
    nationalRegime = Some("Welsh"),
    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = Some(1.0),
      totalPropertyProfit = Some(2.0),
      incomeTaxAmount = Some(3.0),
      taxableIncome = Some(4.0),
      bands = List(
        TaxBand("PPPBand", 1.0, 2.0, 3.0)
      )
    ),
    savingsAndGains = SavingsAndGains(
      incomeTaxAmount = Some(1.0),
      taxableIncome = Some(2.0),
      bands = List(
        TaxBand("SAGBand", 1.0, 2.0, 3.0)
      )
    ),
    reductionsAndCharges = ReductionsAndCharges(
      giftAidTax = Some(1.0),
      totalPensionSavingsTaxCharges = Some(2.0),
      statePensionLumpSumCharges = Some(3.0),
      totalStudentLoansRepaymentAmount = Some(4.0),
      propertyFinanceRelief = Some(5.0)
    ),
    dividends = Dividends(
      incomeTaxAmount = Some(1.0),
      taxableIncome = Some(2.0),
      bands = List(
        TaxBand("DBand", 1.0, 2.0, 3.0)
      )
    ),
    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(1.0),
      giftOfInvestmentsAndPropertyToCharity = Some(2.0),
      totalAllowancesAndDeductions = Some(3.0),
      totalReliefs = Some(4.0)
    ),
    nic = Nic(
      class2 = Some(1.0),
      class4 = Some(2.0),
      totalNic = Some(3.0),
      class4Bands = Some(Seq(
        NicBand("ZRT", 1.0, 2.0, 3.0)
      ))
    ),
    taxDeductedAtSource = TaxDeductedAtSource(
      Some(100.0),
      Some(200.0),
      Some(300.0),
      Some(400.0),
      Some(500.0),
      Some(600.0),
      Some(700.0)
    )
  )
  val minimalModel: Calculation = Calculation(
    crystallised = true
  )

  val fullReadJson: JsObject = Json.obj(
    "incomeTaxAndNicsCalculated" -> Json.obj(
      "summary" -> Json.obj(
        "totalIncomeTaxAndNicsDue" -> 1.0,
        "totalIncomeTaxAndNicsDue" -> 1.0,
        "totalIncomeTaxNicsCharged" -> 2.0,
        "totalStudentLoansRepaymentAmount" -> 4.0,
        "taxRegime" -> "Welsh",
        "nics" -> Json.obj(
          "class2NicsAmount" -> 1.0,
          "class4NicsAmount" -> 2.0,
          "totalNic" -> 3.0
        ),
        "incomeTax" -> Json.obj(
          "totalPensionSavingsTaxCharges"-> 2.0,
          "statePensionLumpSumCharges"-> 3.0
        ),
        "totalTaxDeducted" -> 700.0
      ),
      "detail" -> Json.obj(
        "nics" -> Json.obj(
          "class4Nics" -> Json.obj(
            "class4NicBands" -> Json.arr(
              Json.obj("name" -> "ZRT",
                "income" -> 1.0,
                "rate" -> 2.0,
                "amount" -> 3.0
              )
            )
          )
        ),
        "taxDeductedAtSource" -> Json.obj(
					"payeEmployments" -> 100.0,
					"occupationalPensions" -> 200.0,
					"stateBenefits" -> 300.0,
					"cis" -> 400.0,
					"ukLandAndProperty" -> 500.0,
					"savings" -> 600.0
				),
        "incomeTax" -> Json.obj(
          "payPensionsProfit" -> Json.obj(
            "incomeTaxAmount" -> 3.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "PPPBand",
                "rate" -> 1.0,
                "income" -> 2.0,
                "taxAmount" -> 3.0
              )
            )
          ),
          "giftAid" -> Json.obj(
            "giftAidTax" -> 1.0
          ),

          "savingsAndGains" -> Json.obj(
            "incomeTaxAmount" -> 1.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "SAGBand",
                "rate" -> 1.0,
                "income" -> 2.0,
                "taxAmount" -> 3.0
              )
            )
          ),
          "dividends" -> Json.obj(
            "incomeTaxAmount" -> 1.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "DBand",
                "rate" -> 1.0,
                "income" -> 2.0,
                "taxAmount" -> 3.0
              )
            )
          )
        )
    )
    ),
    "taxableIncome" -> Json.obj(
      "summary" -> Json.obj(
        "totalTaxableIncome" -> 3.0
      ),
      "detail" -> Json.obj(
        "payPensionsProfit" -> Json.obj(
          "totalSelfEmploymentProfit" -> 1.0,
          "totalPropertyProfit" -> 2.0,
          "taxableIncome" -> 4.0
        ),
        "savingsAndGains" -> Json.obj(
          "taxableIncome" -> 2.0
        ),
        "dividends" -> Json.obj(
          "taxableIncome" -> 2.0
        )
      )
    ),
    "endOfYearEstimate" -> Json.obj(
      "summary" -> Json.obj(
        "incomeTaxNicAmount" -> 4.0
      )
    ),
    "metadata" -> Json.obj(
      "calculationTimestamp" -> "timestamp",
      "crystallised" -> true
    ),
    "allowancesDeductionsAndReliefs" -> Json.obj(
      "summary" -> Json.obj(
        "totalAllowancesAndDeductions" -> 3.0,
        "totalReliefs" -> 4.0
      ),
      "detail" -> Json.obj(
        "allowancesAndDeductions" -> Json.obj(
          "personalAllowance" -> 1.0,
          "giftOfInvestmentsAndPropertyToCharity" -> 2.0
        ),
        "reliefs" -> Json.obj(
          "residentialFinanceCosts" -> Json.obj(
            "propertyFinanceRelief" -> 5.0
          )
      )

    )
  )
  )
  val minimalReadJson: JsObject = Json.obj(
    "metadata" -> Json.obj(
      "crystallised" -> true
    )
  )

  val fullWriteJson: JsObject = Json.obj(
    "totalIncomeTaxAndNicsDue" -> 1.0,
    "totalIncomeTaxNicsCharged" -> 2.0,
    "totalTaxableIncome" -> 3.0,
    "incomeTaxNicAmount" -> 4.0,
    "timestamp" -> "timestamp",
    "crystallised" -> true,
    "nationalRegime" -> "Welsh",
    "payPensionsProfit" -> Json.obj(
      "totalSelfEmploymentProfit" -> 1.0,
      "totalPropertyProfit" -> 2.0,
      "incomeTaxAmount" -> 3.0,
      "taxableIncome" -> 4.0,
      "bands" -> Json.arr(
        Json.obj(
          "name" -> "PPPBand",
          "rate" -> 1.0,
          "income" -> 2.0,
          "taxAmount" -> 3.0
        )
      )
    ),
    "savingsAndGains" -> Json.obj(
      "incomeTaxAmount" -> 1.0,
      "taxableIncome" -> 2.0,
      "bands" -> Json.arr(
        Json.obj(
          "name" -> "SAGBand",
          "rate" -> 1.0,
          "income" -> 2.0,
          "taxAmount" -> 3.0
        )
      )
    ),
    "reductionsAndCharges" -> Json.obj(
      "giftAidTax" -> 1.0,
      "totalPensionSavingsTaxCharges" -> 2.0,
      "statePensionLumpSumCharges" -> 3.0,
      "totalStudentLoansRepaymentAmount" -> 4.0,
      "propertyFinanceRelief" -> 5.0
    ),
    "dividends" -> Json.obj(
      "incomeTaxAmount" -> 1.0,
      "taxableIncome" -> 2.0,
      "bands" -> Json.arr(
        Json.obj(
          "name" -> "DBand",
          "rate" -> 1.0,
          "income" -> 2.0,
          "taxAmount" -> 3.0
        )
      )
    ),
    "allowancesAndDeductions" -> Json.obj(
      "personalAllowance" -> 1.0,
      "giftOfInvestmentsAndPropertyToCharity" -> 2.0,
      "totalAllowancesAndDeductions" -> 3.0,
      "totalReliefs" -> 4.0
    ),
    "nic" -> Json.obj(
      "class2" -> 1.0,
      "class4" -> 2.0,
      "totalNic" -> 3.0,
      "class4Bands" -> Json.arr(
        Json.obj(
          "name" -> "ZRT",
          "income" -> 1.0,
          "rate" -> 2.0,
          "amount" -> 3.0
        )
      )
    ),
		"taxDeductedAtSource" ->  Json.obj(
			"payeEmployments" -> 100,
			"ukPensions" -> 200,
			"stateBenefits" -> 300,
			"cis" -> 400,
			"ukLandAndProperty" -> 500,
			"savings" -> 600,
			"total" -> 700
		)
  )
  val minimalWriteJson: JsObject = Json.obj(
    "crystallised" -> true,
    "payPensionsProfit" -> Json.obj(
      "bands" -> Json.arr()
    ),
    "savingsAndGains" -> Json.obj(
      "bands" -> Json.arr()
    ),
    "reductionsAndCharges" -> Json.obj(),
    "dividends" -> Json.obj(
      "bands" -> Json.arr()
    ),
    "allowancesAndDeductions" -> Json.obj(),
    "nic" -> Json.obj(),
		"taxDeductedAtSource" -> Json.obj()
  )

  "Calculation" must {
    "read from json successfully" when {
      "all data is provided" in {
        Json.fromJson[Calculation](fullReadJson) mustBe JsSuccess(fullModel)
      }
      "all optional data is not provided" in {
        Json.fromJson[Calculation](minimalReadJson) mustBe JsSuccess(minimalModel)
      }
    }

    "write to json successfully" when {
      "the model is full" in {
        Json.toJson(fullModel) mustBe fullWriteJson
      }
      "the model doesn't contain any optional data" in {
        Json.toJson(minimalModel) mustBe minimalWriteJson
      }
    }
  }

}
