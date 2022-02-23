/*
 * Copyright 2022 HM Revenue & Customs
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

package audit.models

import auth.MtdItUser
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.liabilitycalculation.viewmodels.TaxDueSummaryViewModel
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testTaxYear}
import testConstants.NewCalcBreakdownUnitTestConstants.{liabilityCalculationModelDeductionsMinimal, liabilityCalculationModelSuccessFull}
import uk.gov.hmrc.auth.core.retrieve.Name

class TaxCalculationDetailsResponseAuditModelSpec extends WordSpecLike with MustMatchers {

  val transactionName: String = "tax-calculation-response"
  val auditType: String = "TaxCalculationDetailsResponse"

  val taxCalculationDetailsResponseAuditModelFull: TaxDueResponseAuditModel =
    TaxDueResponseAuditModel(
      mtdItUser = MtdItUser(
        mtditid = testMtditid,
        nino = "nino",
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel(testMtditid, None, Nil, None),
        btaNavPartial = None,
        saUtr = Some("saUtr"),
        credId = Some("credId"),
        userType = Some("Individual"),
        arn = None
      )(FakeRequest()),
      viewModel = TaxDueSummaryViewModel(liabilityCalculationModelSuccessFull),
      taxYear = testTaxYear
    )

  def taxCalculationDetailsResponseAuditModelMinimal(userType: Option[String] = Some("Individual"),
                                                     arn: Option[String] = None): TaxDueResponseAuditModel =
    TaxDueResponseAuditModel(
      mtdItUser = MtdItUser(
        mtditid = testMtditid,
        nino = "nino",
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel(testMtditid, None, Nil, None),
        btaNavPartial = None,
        saUtr = Some("saUtr"),
        credId = Some("credId"),
        userType = userType,
        arn = arn
      )(FakeRequest()),
      viewModel = TaxDueSummaryViewModel(liabilityCalculationModelDeductionsMinimal),
      taxYear = testTaxYear
    )

  val taxCalcDetailsResponseAuditModelDetailJsonMinimalIndividual: JsObject = Json.obj(
    "userType" -> "Individual",
    "saUtr" -> "saUtr",
    "nationalInsuranceNumber" -> "nino",
    "credId" -> "credId",
    "mtditid" -> testMtditid
  )

  val taxCalcDetailsResponseAuditModelDetailJsonMinimalAgent: JsObject = Json.obj(
    "userType" -> "Agent",
    "saUtr" -> "saUtr",
    "nationalInsuranceNumber" -> "nino",
    "credId" -> "credId",
    "mtditid" -> testMtditid,
    "agentReferenceNumber" -> "1"
  )

  val taxCalcDetailsResponseAuditModelDetailJsonFull: JsObject = Json.obj(
    "userType" -> "Individual",
    "saUtr" -> "saUtr",
    "nationalInsuranceNumber" -> "nino",
    "credId" -> "credId",
    "mtditid" -> testMtditid,
    "calculationOnTaxableIncome" -> 12500,
    "incomeTaxAndNationalInsuranceContributionsDue" -> 5000.99,
    "class4NationalInsurance" -> Json.arr(
      Json.obj(
        "rateBand" -> "Zero rate (£12,500.00 at 20%)",
        "amount" -> 5000.99
      )
    ),
    "gainsOnLifePolicies" -> Json.arr(
      Json.obj(
        "rateBand" -> "Starting rate (£12,500.00 at 20%)",
        "amount" -> 5000.99
      )
    ),
    "payPensionsProfit" -> Json.arr(
      Json.obj(
        "rateBand" -> "Basic rate (£12,500.00 at 20%)",
        "amount" -> 5000.99
      )
    ),
    "otherCharges" -> Json.arr(
      Json.obj(
        "chargeType" -> "Student Loan Repayments",
        "amount" -> 5000.99
      ),
      Json.obj(
        "chargeType" -> "Underpaid tax for earlier years in your tax code for 2017 to 2018",
        "amount" -> 5000.99
      ),
      Json.obj(
        "chargeType" -> "Underpaid tax for earlier years in your self assessment for 2017 to 2018",
        "amount" -> -2500.99
      )
    ),
    "employmentLumpSums" -> Json.arr(
      Json.obj(
        "rateBand" -> "Starting rate (£12,500.00 at 20%)",
        "amount" -> 5000.99
      )
    ),
    "taxDeductions" -> Json.arr(
      Json.obj(
        "deductionType" -> "Outstanding debt collected through PAYE",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "All employments",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "UK pensions",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "State benefits",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "CIS and trading income",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "UK land and property",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "Special withholding tax",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "Void ISAs",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "Interest received from UK banks and building societies",
        "amount" -> 5000.99
      ),
      Json.obj(
        "deductionType" -> "Income Tax due after deductions",
        "amount" -> 50000.99
      )
    ),
    "taxReductions" -> Json.arr(
      Json.obj(
        "reductionDescription" -> "Venture Capital Trust relief",
        "amount" -> 5000.99
      ),
      Json.obj(
        "reductionDescription" -> "Deficiency Relief",
        "amount" -> 5000.99
      ),
      Json.obj(
        "reductionDescription" -> "Marriage allowance transfer",
        "amount" -> 5000.99
      ),
      Json.obj(
        "reductionDescription" -> "Top slicing relief",
        "amount" -> 5000.99
      ),
      Json.obj(
        "reductionDescription" -> "Relief for finance costs",
        "amount" -> 5000.99
      ),
      Json.obj(
        "reductionDescription" -> "Notional tax from gains on life policies etc.",
        "amount" -> 5000.99
      ),
      Json.obj(
        "reductionDescription" -> "Foreign Tax Credit Relief",
        "amount" -> 5000.99
      ),
      Json.obj(
        "reductionDescription" -> "Income Tax due after tax reductions",
        "amount" -> 5000.99
      )
    ),
    "additionalCharges" -> Json.arr(
      Json.obj(
        "chargeType" -> "Voluntary Class 2 National Insurance",
        "amount" -> 5000.99
      ),
      Json.obj(
        "chargeType" -> "Gift Aid tax charge",
        "amount" -> 5000.99
      ),
      Json.obj(
        "chargeType" -> "Total pension saving charges",
        "amount" -> 5000.99
      ),
      Json.obj(
        "chargeType" -> "State pension lump sum",
        "amount" -> 5000.99
      )
    ),
    "dividends" -> Json.arr(
      Json.obj(
        "rateBand" -> "Starting rate (£12,500.00 at 20%)",
        "amount" -> 5000.99
      )
    ),
    "capitalGainsTax" -> Json.obj(
      "taxableCapitalGains" -> 5000.99,
      "capitalGainsTaxAdjustment" -> -2500.99,
      "foreignTaxCreditReliefOnCapitalGains" -> 5000.99,
      "taxOnGainsAlreadyPaid" -> 5000.99,
      "capitalGainsTaxDue" -> 5000.99,
      "capitalGainsTaxCalculatedAsOverpaid" -> 5000.99,
      "rates" -> Json.arr(
        Json.obj("rateBand" -> "Business Asset Disposal Relief and or Investors' Relief gains (£5,000.99 at 20%)",
          "amount" -> 5000.99),
        Json.obj("rateBand" -> "Residential property and carried interest basic rate (£5,000.99 at 20%)",
          "amount" -> 5000.99),
        Json.obj("rateBand" -> "Residential property and carried interest lowerRate2 (£5,000.99 at 21%)",
          "amount" -> 5000.99),
        Json.obj("rateBand" -> "Other gains basic rate (£5,000.99 at 20%)",
          "amount" -> 5000.99),
        Json.obj("rateBand" -> "Other gains lowerRate2 (£5,000.99 at 21%)",
          "amount" -> 5000.99)
      )
    ),
    "taxCalculationMessage" -> Json.arr(
      Json.obj(
        "calculationMessage" -> "This is a forecast of your annual income tax liability based on the information you have provided to date. Any overpayments of income tax will not be refundable until after you have submitted your final declaration"
      ),
      Json.obj(
        "calculationMessage" -> "Your Class 4 has been adjusted for Class 2 due and primary Class 1 contributions."
      ),
      Json.obj(
        "calculationMessage" -> "Due to the level of your income, you are no longer eligible for Marriage Allowance and your claim will be cancelled."
      )
    ),
    "savings" -> Json.arr(
      Json.obj(
        "rateBand" -> "Zero rate (£12,500.00 at 0%)",
        "amount" -> 0
      )
    )
  )

  "TaxCalculationDetailsResponseAuditModel(user, calcDisplayModel, taxYear)" should {
    s"have the correct transaction name of: $transactionName" in {
      taxCalculationDetailsResponseAuditModelFull.transactionName mustBe transactionName
    }

    s"have the correct audit type of: $auditType" in {
      taxCalculationDetailsResponseAuditModelFull.auditType mustBe auditType
    }

    "have the correct detail for the audit event" when {
      "the user is an individual" in {
        taxCalculationDetailsResponseAuditModelFull.detail mustBe taxCalcDetailsResponseAuditModelDetailJsonFull
      }

      "the user is an agent" in {
        taxCalculationDetailsResponseAuditModelMinimal(
          userType = Some("Agent"), arn = Some("1")
        ).detail mustBe taxCalcDetailsResponseAuditModelDetailJsonMinimalAgent
      }

      "the audit is empty" in {
        taxCalculationDetailsResponseAuditModelMinimal().detail mustBe taxCalcDetailsResponseAuditModelDetailJsonMinimalIndividual
      }
    }
  }


}
