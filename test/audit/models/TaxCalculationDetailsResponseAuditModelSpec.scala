/*
 * Copyright 2023 HM Revenue & Customs
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

import authV2.AuthActionsTestData.defaultMTDITUser
import controllers.constants.IncomeSourceAddedControllerConstants.testObligationsModel
import forms.IncomeSourcesFormsSpec.commonAuditDetails
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.liabilitycalculation.viewmodels.TaxDueSummaryViewModel
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.*
import testConstants.BaseTestConstants.*
import testConstants.NewCalcBreakdownUnitTestConstants.{liabilityCalculationModelDeductionsMinimal, liabilityCalculationModelSuccessful}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class TaxCalculationDetailsResponseAuditModelSpec extends AnyWordSpecLike with Matchers {

  val transactionName: String = "tax-calculation-response"
  val auditType: String = "TaxCalculationDetailsResponse"

  val taxCalculationDetailsResponseAuditModelFull: TaxDueResponseAuditModel =
    TaxDueResponseAuditModel(
      mtdItUser = defaultMTDITUser(Some(Individual), IncomeSourceDetailsModel(testNino, testMtditid, None, Nil, Nil)),
      viewModel = TaxDueSummaryViewModel(liabilityCalculationModelSuccessful, testObligationsModel),
      taxYear = testTaxYear
    )

  def taxCalculationDetailsResponseAuditModelMinimal(userType: Option[AffinityGroup] = Some(Individual)): TaxDueResponseAuditModel =
    TaxDueResponseAuditModel(
      mtdItUser = defaultMTDITUser(userType, IncomeSourceDetailsModel(testNino, testMtditid, None, Nil, Nil)),
      viewModel = TaxDueSummaryViewModel(liabilityCalculationModelDeductionsMinimal(), testObligationsModel),
      taxYear = testTaxYear
    )

  val taxCalcDetailsResponseAuditModelDetailJsonMinimalIndividual: JsObject =
    commonAuditDetails(Individual) ++ Json.obj(
    "selfAssessmentTaxAmount" -> 0
  )

  val taxCalcDetailsResponseAuditModelDetailJsonMinimalAgent: JsObject =
    commonAuditDetails(Agent) ++ Json.obj(
    "selfAssessmentTaxAmount" -> 0
  )

  val taxCalcDetailsResponseAuditModelDetailJsonFull: JsObject =
    commonAuditDetails(Individual) ++ Json.obj(
    "calculationOnTaxableIncome" -> 12500,
    "selfAssessmentTaxAmount" -> 5000.99,
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
      ),
      Json.obj(
        "rateBand" -> "Transitional profit (£3,000.00)",
        "amount" -> 700
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
        "deductionType" -> "Tax deducted on trading income",
        "amount" -> 563.12
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
        "calculationMessage" -> "This is a forecast of your annual Income Tax liability based on the information you have provided to date. Any overpayments of Income Tax will not be refundable until after you have submitted your final declaration"
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
        assertJsonEquals(taxCalculationDetailsResponseAuditModelFull.detail, taxCalcDetailsResponseAuditModelDetailJsonFull)
      }

      "the user is an agent" in {
        assertJsonEquals(taxCalculationDetailsResponseAuditModelMinimal(
          userType = Some(Agent)
        ).detail, taxCalcDetailsResponseAuditModelDetailJsonMinimalAgent)
      }

      "the audit is empty" in {
        assertJsonEquals(taxCalculationDetailsResponseAuditModelMinimal().detail, taxCalcDetailsResponseAuditModelDetailJsonMinimalIndividual)
      }
    }
  }

  def normalise(js: JsValue): JsValue = js match {

    case JsObject(fields) =>
      val filteredFields = fields.collect {
        case (k, v) if v != JsNull =>
          normalise(v) match {
            case JsObject(inner) if inner.isEmpty => None // remove empty object
            case JsArray(inner) if inner.isEmpty => None // remove empty array
            case n => Some(k -> n)
          }
      }.flatten

      JsObject(filteredFields.toSeq.sortBy(_._1))

    case JsArray(values) =>
      val normValues = values.collect { case v if v != JsNull => normalise(v) }
      val filteredValues = normValues.filter {
        case JsObject(inner) if inner.isEmpty => false
        case JsArray(inner) if inner.isEmpty => false
        case _ => true
      }
      if (filteredValues.forall(_.isInstanceOf[JsObject])) JsArray(filteredValues.sortBy(_.toString))
      else JsArray(filteredValues)

    case JsNumber(n) => JsNumber(n.bigDecimal.stripTrailingZeros())
    case other => other
  }

  def assertJsonEquals(actual: JsValue, expected: JsValue): Assertion =
    normalise(actual) shouldEqual normalise(expected)
}
