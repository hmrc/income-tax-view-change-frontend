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

import models.calculation.TaxDeductedAtSource.{Message, Messages}
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
        TaxBand("PPPBand", 1.0, 2.0, 3.0, 4.0, 5.0)
      )
    ),
    savingsAndGains = SavingsAndGains(
      incomeTaxAmount = Some(1.0),
      taxableIncome = Some(2.0),
      bands = List(
        TaxBand("SAGBand", 1.0, 2.0, 3.0, 4.0, 5.0)
      )
    ),
    reductionsAndCharges = ReductionsAndCharges(
      giftAidTax = Some(1.0),
      grossGiftAidPayments = Some(2.0),
      totalPensionSavingsTaxCharges = Some(3.0),
      statePensionLumpSumCharges = Some(4.0),
      totalStudentLoansRepaymentAmount = Some(5.0),
      totalResidentialFinanceCostsRelief = Some(6.0),
      totalForeignTaxCreditRelief = Some(7.0),
      totalNotionalTax = Some(8.0),
      reliefsClaimed = Some(Seq(ReliefsClaimed("deficiencyRelief", Some(1.0)), ReliefsClaimed("vctSubscriptions", Some(2.0)),
        ReliefsClaimed("eisSubscriptions", Some(3.0)), ReliefsClaimed("seedEnterpriseInvestment", Some(4.0)),
        ReliefsClaimed("communityInvestment", Some(5.0)), ReliefsClaimed("socialEnterpriseInvestment", Some(6.0)),
        ReliefsClaimed("maintenancePayments", Some(7.0)),
        ReliefsClaimed("qualifyingDistributionRedemptionOfSharesAndSecurities", Some(8.0)),
        ReliefsClaimed("nonDeductibleLoanInterest", Some(9.0))
      ))
    ),
    dividends = Dividends(
      incomeTaxAmount = Some(1.0),
      taxableIncome = Some(2.0),
      bands = List(
        TaxBand("DBand", 1.0, 2.0, 3.0, 4.0, 5.0)
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
      Some(700.0),
      Some(800.0),
      Some(700.0),
      Some(1.0)
    ),
    lumpSums = LumpSums(
      bands = List(
        TaxBand("LSBand", 1.0, 2.0, 3.0, 4.0, 5.0)
      )
    ),
    gainsOnLifePolicies = GainsOnLifePolicies(
      bands = List(
        TaxBand("GOLPBand", 1.0, 2.0, 3.0, 4.0, 5.0)
      )
    ),
    messages = Some(Messages(
      info = Some(Seq(Message("infoId", "infoMessage"))),
      warnings = Some(Seq(Message("warningId", "warningMessage"))),
      errors = Some(Seq(Message("errorId", "errorMessage")))
    ))
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
        "totalStudentLoansRepaymentAmount" -> 5.0,
        "taxRegime" -> "Welsh",
        "nics" -> Json.obj(
          "class2NicsAmount" -> 1.0,
          "class4NicsAmount" -> 2.0,
          "totalNic" -> 3.0
        ),
        "incomeTax" -> Json.obj(
          "totalPensionSavingsTaxCharges" -> 3.0,
          "statePensionLumpSumCharges" -> 4.0,
          "totalNotionalTax" -> 8.0
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
                "amount" -> 3.0,
                "bandLimit" -> 4.0,
                "apportionedBandLimit" -> 5.0
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
          "specialWithholdingTaxOrUkTaxPaid" -> 600.0,
          "voidedIsa" -> 700.0,
          "savings" -> 800.0,
          "totalIncomeTaxAndNicsDue" -> 1.0
        ),
        "incomeTax" -> Json.obj(
          "payPensionsProfit" -> Json.obj(
            "incomeTaxAmount" -> 3.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "PPPBand",
                "rate" -> 1.0,
                "income" -> 2.0,
                "taxAmount" -> 3.0,
                "bandLimit" -> 4.0,
                "apportionedBandLimit" -> 5.0
              )
            )
          ),
          "giftAid" -> Json.obj(
            "giftAidTax" -> 1.0,
            "grossGiftAidPayments" -> 2.0,
          ),
          "savingsAndGains" -> Json.obj(
            "incomeTaxAmount" -> 1.0,
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "SAGBand",
                "rate" -> 1.0,
                "income" -> 2.0,
                "taxAmount" -> 3.0,
                "bandLimit" -> 4.0,
                "apportionedBandLimit" -> 5.0
              )
            )
          ),
          "lumpSums" -> Json.obj(
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "LSBand",
                "rate" -> 1.0,
                "income" -> 2.0,
                "taxAmount" -> 3.0,
                "bandLimit" -> 4.0,
                "apportionedBandLimit" -> 5.0
              )
            )
          ),
          "gainsOnLifePolicies" -> Json.obj(
            "taxBands" -> Json.arr(
              Json.obj(
                "name" -> "GOLPBand",
                "rate" -> 1.0,
                "income" -> 2.0,
                "taxAmount" -> 3.0,
                "bandLimit" -> 4.0,
                "apportionedBandLimit" -> 5.0
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
                "taxAmount" -> 3.0,
                "bandLimit" -> 4.0,
                "apportionedBandLimit" -> 5.0
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
            "totalResidentialFinanceCostsRelief" -> 6.0
          ),
          "foreignTaxCreditRelief" -> Json.obj(
            "totalForeignTaxCreditRelief" -> 7.0
          ),
          "reliefsClaimed" -> Json.arr(
            Json.obj(
              "type" -> "deficiencyRelief",
              "amountClaimed" -> 1.1,
              "allowableAmount" -> 1.2,
              "amountUsed" -> 1.0,
              "rate" -> 2
            ),
            Json.obj(
              "type" -> "vctSubscriptions",
              "amountUsed" -> 2.0
            ),
            Json.obj(
              "type" -> "eisSubscriptions",
              "amountUsed" -> 3.0
            ),
            Json.obj(
              "type" -> "seedEnterpriseInvestment",
              "amountUsed" -> 4.0
            ),
            Json.obj(
              "type" -> "communityInvestment",
              "amountUsed" -> 5.0
            ),
            Json.obj(
              "type" -> "socialEnterpriseInvestment",
              "amountUsed" -> 6.0
            ),
            Json.obj(
              "type" -> "maintenancePayments",
              "amountUsed" -> 7.0
            ),
            Json.obj(
              "type" -> "qualifyingDistributionRedemptionOfSharesAndSecurities",
              "amountUsed" -> 8.0
            ),
            Json.obj(
              "type" -> "nonDeductibleLoanInterest",
              "amountUsed" -> 9.0
            )
          )
        )
      )
    ),
    "messages" -> Json.obj(
      "info" -> Json.arr(
        Json.obj(
          "id" -> "infoId",
          "text" -> "infoMessage"
        )
      ),
      "warnings" -> Json.arr(
        Json.obj(
          "id" -> "warningId",
          "text" -> "warningMessage"
        )
      ),
      "errors" -> Json.arr(
        Json.obj(
          "id" -> "errorId",
          "text" -> "errorMessage"
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
          "taxAmount" -> 3.0,
          "bandLimit" -> 4.0,
          "apportionedBandLimit" -> 5.0
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
          "taxAmount" -> 3.0,
          "bandLimit" -> 4.0,
          "apportionedBandLimit" -> 5.0
        )
      )
    ),
    "reductionsAndCharges" -> Json.obj(
      "giftAidTax" -> 1.0,
      "grossGiftAidPayments" -> 2.0,
      "totalPensionSavingsTaxCharges" -> 3.0,
      "statePensionLumpSumCharges" -> 4.0,
      "totalStudentLoansRepaymentAmount" -> 5.0,
      "totalResidentialFinanceCostsRelief" -> 6.0,
      "totalForeignTaxCreditRelief" -> 7.0,
      "totalNotionalTax" -> 8.0,
      "reliefsClaimed" -> Json.arr(
        Json.obj(
          "type" -> "deficiencyRelief",
          "amountUsed" -> 1.0
        ),
        Json.obj(
          "type" -> "vctSubscriptions",
          "amountUsed" -> 2.0
        ),
        Json.obj(
          "type" -> "eisSubscriptions",
          "amountUsed" -> 3.0
        ),
        Json.obj(
          "type" -> "seedEnterpriseInvestment",
          "amountUsed" -> 4.0
        ),
        Json.obj(
          "type" -> "communityInvestment",
          "amountUsed" -> 5.0
        ),
        Json.obj(
          "type" -> "socialEnterpriseInvestment",
          "amountUsed" -> 6.0
        ),
        Json.obj(
          "type" -> "maintenancePayments",
          "amountUsed" -> 7.0
        ),
        Json.obj(
          "type" -> "qualifyingDistributionRedemptionOfSharesAndSecurities",
          "amountUsed" -> 8.0
        ),
        Json.obj(
          "type" -> "nonDeductibleLoanInterest",
          "amountUsed" -> 9.0
        )
      )
    ),
    "dividends" -> Json.obj(
      "incomeTaxAmount" -> 1.0,
      "taxableIncome" -> 2.0,
      "bands" -> Json.arr(
        Json.obj(
          "name" -> "DBand",
          "rate" -> 1.0,
          "income" -> 2.0,
          "taxAmount" -> 3.0,
          "bandLimit" -> 4.0,
          "apportionedBandLimit" -> 5.0
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
    "taxDeductedAtSource" -> Json.obj(
      "payeEmployments" -> 100,
      "ukPensions" -> 200,
      "stateBenefits" -> 300,
      "cis" -> 400,
      "ukLandAndProperty" -> 500,
      "specialWithholdingTax" -> 600.0,
      "voidISAs" -> 700.0,
      "savings" -> 800,
      "total" -> 700,
      "totalIncomeTaxAndNicsDue" -> 1
    ),
    "lumpSums" -> Json.obj(
      "bands" -> Json.arr(
        Json.obj(
          "name" -> "LSBand",
          "rate" -> 1.0,
          "income" -> 2.0,
          "taxAmount" -> 3.0,
          "bandLimit" -> 4.0,
          "apportionedBandLimit" -> 5.0
        )
      )
    ),
    "gainsOnLifePolicies" -> Json.obj(
      "bands" -> Json.arr(
        Json.obj(
          "name" -> "GOLPBand",
          "rate" -> 1.0,
          "income" -> 2.0,
          "taxAmount" -> 3.0,
          "bandLimit" -> 4.0,
          "apportionedBandLimit" -> 5.0
        )
      )
    ),
    "messages" -> Json.obj(
      "info" -> Json.arr(
        Json.obj(
          "id" -> "infoId",
          "text" -> "infoMessage"
        )
      ),
      "warnings" -> Json.arr(
        Json.obj(
          "id" -> "warningId",
          "text" -> "warningMessage"
        )
      ),
      "errors" -> Json.arr(
        Json.obj(
          "id" -> "errorId",
          "text" -> "errorMessage"
        )
      )
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
    "taxDeductedAtSource" -> Json.obj(),
    "lumpSums" -> Json.obj(
      "bands" -> Json.arr()
    ),
    "gainsOnLifePolicies" -> Json.obj(
      "bands" -> Json.arr()
    )
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
