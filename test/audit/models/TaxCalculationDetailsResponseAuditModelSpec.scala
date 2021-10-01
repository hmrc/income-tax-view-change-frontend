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

package audit.models

import assets.BaseTestConstants.{testMtditid, testTaxYear}
import assets.CalcBreakdownTestConstants.calculationDataSuccessModel
import auth.MtdItUser
import enums.{CalcStatus, Crystallised, Estimate}
import models.calculation.TaxDeductedAtSource.{Message, Messages}
import models.calculation._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name

class TaxCalculationDetailsResponseAuditModelSpec extends WordSpecLike with MustMatchers {

  val transactionName: String = "tax-calculation-response"
  val auditType: String = "TaxCalculationDetailsResponse"

  val testCalcDataModel: Calculation = Calculation(
    totalIncomeTaxAndNicsDue = Some(10000),
    crystallised = true,
    totalTaxableIncome = Some(25000),
    nic = Nic(
      class4Bands = Some(Seq(
        NicBand(
          name = "SRT",
          income = 12000,
          rate = 10,
          amount = 1200
        )
      )),
      class2 = Some(1000.00),
      class2VoluntaryContributions = Some(true)
    ),
    payPensionsProfit = PayPensionsProfit(
      bands = List(TaxBand(
        name = "BRT",
        rate = 20,
        income = 20000,
        taxAmount = 4000,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ))
    ),
    gainsOnLifePolicies = GainsOnLifePolicies(
      bands = List(TaxBand(
        name = "BRT",
        rate = 20,
        income = 20000,
        taxAmount = 4000,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ))
    ),
    reductionsAndCharges = ReductionsAndCharges(
      totalStudentLoansRepaymentAmount = Some(1234),
      totalResidentialFinanceCostsRelief = Some(4321),
      reliefsClaimed = Some(Seq(ReliefsClaimed("vctSubscriptions", Some(5678))))
    ),
    lumpSums = LumpSums(
      bands = List(TaxBand(
        name = "BRT",
        rate = 20,
        income = 20000,
        taxAmount = 4000,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ))
    ),
    taxDeductedAtSource = TaxDeductedAtSource(
      cis = Some(1350),
      total = Some(1350)
    ),
    dividends = Dividends(
      bands = List(TaxBand(
        name = "ZRTBR",
        rate = 0,
        income = 10000,
        taxAmount = 0,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ))
    ),
    capitalGainsTax = CapitalGainsTax(
      businessAssetsDisposalsAndInvestorsRel = SingleBandCgtDetail(
        taxableGains = Some(12000),
        rate = Some(12),
        taxAmount = Some(1000)
      ),
      propertyAndInterestTaxBands = List(CgtTaxBand(
        name = "lowerRate",
        rate = 23,
        income = 10000,
        taxAmount = 2300
      )),
      otherGainsTaxBands = List(CgtTaxBand(
        name = "higherRate",
        rate = 44,
        income = 35000,
        taxAmount = 3400
      )),
      taxOnGainsAlreadyPaid = Some(3570)
    ),
    messages = Some(Messages(
      info = Some(Seq(Message(id = "C22211", text = "testInfoMessage"))),
      warnings = Some(Seq(Message(id = "C22214", text = "testWarningMessage"))),
      errors = Some(Seq(Message(id="C22216", text = "testErrorMessage")))
    )),
    savingsAndGains = SavingsAndGains(
      bands = List(TaxBand(
        name = "ZRT",
        rate = 0,
        income = 10000,
        taxAmount = 0,
        bandLimit = 15000,
        apportionedBandLimit = 15000
      ))
    )
  )

  val calcDisplayModel: CalcDisplayModel = CalcDisplayModel("", 1, testCalcDataModel, Crystallised)


  val taxCalculationDetailsResponseAuditModelFull: TaxCalculationDetailsResponseAuditModel =
    TaxCalculationDetailsResponseAuditModel(
      mtdItUser = MtdItUser(
        mtditid = testMtditid,
        nino = "nino",
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel(testMtditid, None, Nil, None),
        saUtr = Some("saUtr"),
        credId = Some("credId"),
        userType = Some("Individual"),
        arn = None
      )(FakeRequest()),
      calcDisplayModel = calcDisplayModel,
      taxYear = testTaxYear
    )

  def taxCalculationDetailsResponseAuditModelMinimal(userType: Option[String] = Some("Individual"),
                                                     arn: Option[String] = None): TaxCalculationDetailsResponseAuditModel =
    TaxCalculationDetailsResponseAuditModel(
      mtdItUser = MtdItUser(
        mtditid = testMtditid,
        nino = "nino",
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel(testMtditid, None, Nil, None),
        saUtr = Some("saUtr"),
        credId = Some("credId"),
        userType = userType,
        arn = arn
      )(FakeRequest()),
      calcDisplayModel = CalcDisplayModel("", 0, Calculation(crystallised = false), Estimate),
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
    "calculationOnTaxableIncome" -> 25000.00,
    "incomeTaxAndNationalInsuranceContributionsDue" -> 10000.00,
    "class4NationalInsurance" -> Json.arr(
      Json.obj(
        "rateBand" -> "Starter rate (£12000 at 10%)",
        "amount" -> 1200
      )
    ),
    "gainsOnLifePolicies" -> Json.arr(
      Json.obj(
      "rateBand" -> "Basic rate (£15000 at 20%)",
      "amount" -> 4000
      )
    ),
    "payPensionsProfit" -> Json.arr(
      Json.obj(
        "rateBand" -> "Basic rate (£15000 at 20%)",
        "amount" -> 4000
      )
    ),
    "otherCharges" -> Json.arr(
      Json.obj(
        "chargeType" -> "Student Loan Repayments",
        "amount" -> 1234
      )
    ),
    "employmentLumpSums" -> Json.arr(
      Json.obj(
        "rateBand" -> "Basic rate (£15000 at 20%)",
        "amount" -> 4000
      )
    ),
    "taxDeductions" -> Json.arr(
      Json.obj(
        "deductionsType" -> "CIS and trading income",
        "amount" -> 1350
      ),
      Json.obj(
        "deductionsType" -> "Income Tax due after deductions",
        "amount" -> 1350
      )
    ),
    "taxReductions" -> Json.arr(
      Json.obj(
        "reductionDescription" -> "Venture Capital Trust relief",
        "amount" -> 5678
      ),
      Json.obj(
        "reductionDescription" -> "Relief for finance costs",
        "amount" -> 4321
      )
    ),
    "additionalCharges" -> Json.arr(
      Json.obj(
        "chargeType" -> "Voluntary Class 2 National Insurance",
        "amount" -> 1000
      )
    ),
    "dividends" -> Json.arr(
      Json.obj(
        "rateBand" -> "Basic rate band at nil rate (£15000 at 0%)",
        "amount" -> 0
      )
    ),
    "capitalGainsTax" -> Json.obj(
      "taxOnGainsAlreadyPaid" -> 3570,
      "rates" -> Json.arr(
        Json.obj("rateBand" -> "Business Asset Disposal Relief and or Investors' Relief gains (£12000 at 12 %)",
          "amount" -> 1000),
        Json.obj("rateBand" -> "Residential property and carried interest basic rate (£10000 at 23%)",
          "amount" -> 2300),
        Json.obj("rateBand" -> "Other gains higher rate (£35000 at 44%)",
          "amount" -> 3400)
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
        "rateBand" -> "Zero rate (£15000 at 0%)",
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
