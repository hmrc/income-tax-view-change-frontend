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

import authV2.AuthActionsTestData._
import implicits.ImplicitDateParser
import models.core.AccountingPeriodModel
import models.financialDetails.{ChargeItem, DocumentDetail, DocumentDetailWithDueDate}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.liabilitycalculation.{Message, Messages}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import models.taxyearsummary.TaxYearSummaryChargeItem
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}
import testConstants.BaseTestConstants.{taxYear, testMtditid, testNino}
import testConstants.BusinessDetailsTestConstants.{address, testIncomeSource}
import testConstants.ChargeConstants
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate

class TaxYearSummaryResponseAuditModelSpec extends AnyWordSpecLike with TestSupport with ImplicitDateParser with ChargeConstants {

  val transactionName: String = "tax-year-overview-response"
  val auditType: String = "TaxYearOverviewResponse"
  val paymentsLpiPaymentOnAccount1: String = messages("tax-year-summary.payments.lpi.paymentOnAccount1.text")
  val paymentsPaymentOnAccount1: String = messages("tax-year-summary.payments.paymentOnAccount1.text")
  val updateTypeQuarterly: String = "Quarterly Update"
  val emptyCTAViewModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(None)

  def calculationSummary(forecastIncome: Option[Int] = None,
                         forecastIncomeTaxAndNics: Option[BigDecimal] = None,
                         forecastAllowancesAndDeductions: Option[BigDecimal] = None): CalculationSummary = CalculationSummary(
    timestamp = Some("2017-07-06T12:34:56.789Z".toZonedDateTime.toLocalDate),
    crystallised = false,
    unattendedCalc = false,
    taxDue = 2010.00,
    income = 199505,
    deductions = 500.00,
    totalTaxableIncome = 198500,
    forecastIncome = forecastIncome,
    forecastIncomeTaxAndNics = forecastIncomeTaxAndNics,
    forecastAllowancesAndDeductions = forecastAllowancesAndDeductions
  )

  def unattendedCalcSummary(forecastIncome: Option[Int] = None,
                            forecastIncomeTaxAndNics: Option[BigDecimal] = None,
                            forecastAllowancesAndDeductions: Option[BigDecimal] = None): CalculationSummary = CalculationSummary(
    timestamp = Some("2017-07-06T12:34:56.789Z".toZonedDateTime.toLocalDate),
    crystallised = false,
    unattendedCalc = true,
    taxDue = 2010.00,
    income = 199505,
    deductions = 500.00,
    totalTaxableIncome = 198500,
    forecastIncome = forecastIncome,
    forecastIncomeTaxAndNics = forecastIncomeTaxAndNics,
    forecastAllowancesAndDeductions = forecastAllowancesAndDeductions
  )

  def payments(hasDunningLock: Boolean): List[ChargeItem] = {
    List(
      chargeItemModel(dunningLock = hasDunningLock)
    )
  }


  val docDetail: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("ITSA- POA 1"),
    documentText = Some("documentText"),
    originalAmount = 10.34,
    outstandingAmount = 0,
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val docDateDetail: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail,
    dueDate = Some(fixedDate)
  )

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = fixedDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val updates: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(fixedDate),
          periodKey = "Quarterly",
          StatusFulfilled
        )
      )
    )
  ))

  val business: List[BusinessDetailsModel] = List(BusinessDetailsModel(
    "testId",
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
    tradingName = Some("Test Trading Name"),
    firstAccountingPeriodEndDate = None,
    tradingStartDate = Some(getCurrentTaxYearEnd),
    contextualTaxYear = None,
    cessation = None,
    address = Some(address),
    cashOrAccruals = true
  ))

  val singleErrorMessage: Option[Messages] = Some(Messages(errors = Some(Seq(
    Message("C55012", "the update must align to the accounting period end date of 05/01/2023.")
  ))))
  val singleAgentErrorMessage: Option[Messages] = Some(Messages(errors = Some(Seq(
    Message("C55509", "your client claimed Property Income Allowance for their UK furnished holiday lettings. This means that they cannot claim any further expenses."),
  ))))
  val singleMultiLineErrorMessage: Option[Messages] = Some(Messages(errors = Some(Seq(
    Message("C159028", "the total tax taken off your employment must be less than the total taxable pay including: tips, other payments, lump sums")
  ))))
  val singleMultiLineAgentErrorMessage: Option[Messages] = Some(Messages(errors = Some(Seq(
    Message("C159028", "the total tax taken off your client’s employment must be less than the total taxable pay including: tips, other payments, lump sums")
  ))))
  val multipleErrorMessage: Option[Messages] = Some(Messages(errors = Some(Seq(
    Message("C55012", "the update must align to the accounting period end date of 05/01/2023."),
    Message("C15507", "you’ve claimed £2000 in Property Income Allowance but this is more than turnover for your UK property."),
    Message("C15510", "the Rent a Room relief claimed for a jointly let property cannot be more than 10% of the Rent a Room limit."),
    Message("C159028", "the total tax taken off your employment must be less than the total taxable pay including: tips, other payments, lump sums")
  ))))
  val multipleAgentErrorMessage: Option[Messages] = Some(Messages(errors = Some(Seq(
    Message("C55012", "the update must align to the accounting period end date of 05/01/2023."),
    Message("C15507", "your client claimed £2000 in Property Income Allowance but this is more than turnover for their UK property."),
    Message("C15510", "the Rent a Room relief claimed for a jointly let property cannot be more than 10% of the Rent a Room limit."),
    Message("C159028", "the total tax taken off your client’s employment must be less than the total taxable pay including: tips, other payments, lump sums")
  ))))

  val jsonAuditAgentResponse: JsObject = commonAuditDetails(Agent) ++ Json.obj(
    "taxYearOverview" -> Json.obj(
      "calculationDate" -> "2017-07-06",
      "calculationAmount" -> 2010,
      "isCrystallised" -> false,
      "forecastAmount" -> null
    ),
    "forecast" -> Json.obj(
      "income" -> null,
      "taxableIncome" -> null,
      "taxDue" -> null,
      "totalAllowancesAndDeductions" -> null
    ),
    "calculation" -> Json.obj(
      "income" -> 199505,
      "allowancesAndDeductions" -> 500,
      "taxableIncome" -> 198500,
      "taxDue" -> 2010,
      "calculationReason" -> "customerRequest",
    ),
    "payments" -> Seq(Json.obj(
      "amount" -> 1400,
      "dueDate" -> "2019-05-15",
      "paymentType" -> paymentsPaymentOnAccount1,
      "underReview" -> false,
      "status" -> "unpaid"
    ), Json.obj(
      "amount" -> 100,
      "dueDate" -> "2019-05-15",
      "paymentType" -> "Late payment interest on first payment on account",
      "underReview" -> false,
      "status" -> "part-paid"
    )),
    "updates" -> Seq(Json.obj(
      "incomeSource" -> "Test Trading Name",
      "dateSubmitted" -> fixedDate.toString,
      "updateType" -> updateTypeQuarterly
    ))
  )

  val jsonAuditIndividualResponse: JsObject = commonAuditDetails(Individual) ++ Json.obj(
    "taxYearOverview" -> Json.obj(
      "calculationDate" -> "2017-07-06",
      "calculationAmount" -> 2010,
      "isCrystallised" -> false,
      "forecastAmount" -> None
    ),
    "forecast" -> Json.obj(
      "income" -> None,
      "taxableIncome" -> None,
      "taxDue" -> None,
      "totalAllowancesAndDeductions" -> None
    ),
    "calculation" -> Json.obj(
      "income" -> 199505,
      "allowancesAndDeductions" -> 500,
      "taxableIncome" -> 198500,
      "taxDue" -> 2010,
      "calculationReason" -> "customerRequest",
    ),
    "payments" -> Seq(Json.obj(
      "amount" -> 1400,
      "dueDate" -> "2019-05-15",
      "paymentType" -> paymentsPaymentOnAccount1,
      "underReview" -> true,
      "status" -> "unpaid"
    ), Json.obj(
      "amount" -> 100,
      "dueDate" -> "2019-05-15",
      "paymentType" -> "Late payment interest on first payment on account",
      "underReview" -> true,
      "status" -> "part-paid"
    )),
    "updates" -> Seq(Json.obj(
      "incomeSource" -> "Test Trading Name",
      "dateSubmitted" -> fixedDate.toString,
      "updateType" -> updateTypeQuarterly
    ))
  )

  def taxYearOverviewResponseAuditFull(userType: Option[AffinityGroup] = Some(Agent),
                                       agentReferenceNumber: Option[String],
                                       paymentHasADunningLock: Boolean = false,
                                       forecastIncome: Option[Int] = None,
                                       forecastIncomeTaxAndNics: Option[BigDecimal] = None,
                                       forecastAllowancesAndDeductions: Option[BigDecimal] = None,
                                       messages: Option[Messages] = None): TaxYearSummaryResponseAuditModel =
    TaxYearSummaryResponseAuditModel(
      mtdItUser = defaultMTDITUser(userType, IncomeSourceDetailsModel(testNino, testMtditid, None, business, Nil)),
      messagesApi = messagesApi,
      taxYearSummaryViewModel = TaxYearSummaryViewModel(
        calculationSummary = Some(calculationSummary(
          forecastIncome = forecastIncome,
          forecastIncomeTaxAndNics = forecastIncomeTaxAndNics,
          forecastAllowancesAndDeductions = forecastAllowancesAndDeductions)
        ), charges = payments(paymentHasADunningLock).map(TaxYearSummaryChargeItem.fromChargeItem),
        obligations = updates, ctaViewModel = emptyCTAViewModel, LPP2Url = ""
        ),
      messages
    )

  def taxYearOverviewResponseAuditUnattendedCalc(userType: Option[AffinityGroup] = Some(Agent),
                                                 agentReferenceNumber: Option[String],
                                                 paymentHasADunningLock: Boolean = false,
                                                 forecastIncome: Option[Int] = None,
                                                 forecastIncomeTaxAndNics: Option[BigDecimal] = None,
                                                 forecastAllowancesAndDeductions: Option[BigDecimal] = None): TaxYearSummaryResponseAuditModel =
    TaxYearSummaryResponseAuditModel(
      mtdItUser = defaultMTDITUser(userType, IncomeSourceDetailsModel(testNino, testMtditid, None, business, Nil)),
      messagesApi = messagesApi,
      taxYearSummaryViewModel = TaxYearSummaryViewModel(Some(unattendedCalcSummary(
        forecastIncome = forecastIncome,
        forecastIncomeTaxAndNics = forecastIncomeTaxAndNics,
        forecastAllowancesAndDeductions = forecastAllowancesAndDeductions)
      ), charges = payments(paymentHasADunningLock).map(TaxYearSummaryChargeItem.fromChargeItem),
        obligations = updates, showForecastData = true, ctaViewModel = emptyCTAViewModel, LPP2Url = ""
      )
    )

  def errorAuditResponseJson(auditResponse: JsObject, messages: Option[Messages]): JsObject = {
    val calculation = auditResponse("calculation")
    val updatedCalc = calculation.as[JsObject] ++ Json.obj("errors" -> messages.get.errors.get.map(_.text))
    auditResponse ++ Json.obj("calculation" -> updatedCalc)
  }

  "TaxYearOverviewResponseAuditModel(mtdItUser, agentReferenceNumber, calculation, payments, updates)" should {

    s"have the correct transaction name of '$transactionName'" in {
      taxYearOverviewResponseAuditFull(
        agentReferenceNumber = Some("1")
      ).transactionName shouldBe transactionName
    }

    s"have the correct audit type of '$auditType'" in {
      taxYearOverviewResponseAuditFull(
        agentReferenceNumber = Some("1")
      ).auditType shouldBe auditType
    }

    "have the correct details for the audit event" when {
      "the user type is Agent" when {
        "full audit response" in {
          taxYearOverviewResponseAuditFull(
            userType = Some(Agent),
            agentReferenceNumber = Some("agentReferenceNumber"),
          ).detail shouldBe jsonAuditAgentResponse
        }
        "audit response has single error messages" in {
          taxYearOverviewResponseAuditFull(
            userType = Some(Agent),
            agentReferenceNumber = Some("agentReferenceNumber"),
            messages = singleAgentErrorMessage
          ).detail shouldBe errorAuditResponseJson(jsonAuditAgentResponse, singleAgentErrorMessage)
        }
        "audit response has single multi-line error messages" in {
          taxYearOverviewResponseAuditFull(
            userType = Some(Agent),
            agentReferenceNumber = Some("agentReferenceNumber"),
            messages = singleMultiLineAgentErrorMessage
          ).detail shouldBe errorAuditResponseJson(jsonAuditAgentResponse, singleMultiLineAgentErrorMessage)
        }
        "audit response has multiple error messages" in {
          taxYearOverviewResponseAuditFull(
            userType = Some(Agent),
            agentReferenceNumber = Some("agentReferenceNumber"),
            messages = multipleAgentErrorMessage
          ).detail shouldBe errorAuditResponseJson(jsonAuditAgentResponse, multipleAgentErrorMessage)
        }
      }

      "the user type is Individual" when {
        "full audit response" in {
          taxYearOverviewResponseAuditFull(
            userType = Some(Individual),
            agentReferenceNumber = None,
            paymentHasADunningLock = true
          ).detail shouldBe jsonAuditIndividualResponse
        }
        "audit response has single error messages" in {
          taxYearOverviewResponseAuditFull(
            userType = Some(Individual),
            agentReferenceNumber = None,
            paymentHasADunningLock = true,
            messages = singleErrorMessage
          ).detail shouldBe errorAuditResponseJson(jsonAuditIndividualResponse, singleErrorMessage)
        }
        "audit response has single multi-line error messages" in {
          taxYearOverviewResponseAuditFull(
            userType = Some(Individual),
            agentReferenceNumber = None,
            paymentHasADunningLock = true,
            messages = singleMultiLineErrorMessage
          ).detail shouldBe errorAuditResponseJson(jsonAuditIndividualResponse, singleMultiLineErrorMessage)
        }
        "audit response has multiple error messages" in {
          taxYearOverviewResponseAuditFull(
            userType = Some(Individual),
            agentReferenceNumber = None,
            paymentHasADunningLock = true,
            messages = multipleErrorMessage
          ).detail shouldBe errorAuditResponseJson(jsonAuditIndividualResponse, multipleErrorMessage)
        }
      }


    }
  }


  "TaxYear audit model" should {
    "present a full audit model" when {
      "there's expected behaviour" in {
        val auditJson = taxYearOverviewResponseAuditFull(
          agentReferenceNumber = Some("1"),
          forecastIncome = Some(2000),
          forecastIncomeTaxAndNics = Some(120.0),
          forecastAllowancesAndDeductions = Some(100)
        )

        (auditJson.detail \ "taxYearOverview" \ "calculationAmount").toString shouldBe "JsDefined(2010)"
        (auditJson.detail \ "taxYearOverview" \ "isCrystallised").toString shouldBe "JsDefined(false)"
        (auditJson.detail \ "taxYearOverview" \ "forecastAmount").toString shouldBe "JsDefined(2000)"
        (auditJson.detail \ "forecast").toOption.get shouldBe
          Json.obj(
            "income" -> 2000,
            "taxableIncome" -> 2000,
            "taxDue" -> 120,
            "totalAllowancesAndDeductions" -> 100
          )
      }
    }
  }

  "TaxYear audit model test" should {
    "present a full audit model test" when {
      "audit is on test" in {
        val auditJson = taxYearOverviewResponseAuditFull(
          agentReferenceNumber = Some("1"),
          forecastIncome = Some(2000),
          forecastIncomeTaxAndNics = Some(120.0),
          forecastAllowancesAndDeductions = Some(100)
        )
        (auditJson.detail \ "calculation" \ "calculationReason").toString contains "customerRequest"
        (auditJson.detail \ "calculation" \ "income").toString() shouldBe "JsDefined(199505)"
        (auditJson.detail \ "calculation" \ "allowancesAndDeductions").toString() shouldBe "JsDefined(500)"
        (auditJson.detail \ "calculation" \ "taxableIncome").toString() shouldBe "JsDefined(198500)"
        (auditJson.detail \ "calculation" \ "taxDue").toString() shouldBe "JsDefined(2010)"
      }
    }

    "present a full audit model test unattended" when {
      "audit has expected test behaviour" in {
        val auditJson = taxYearOverviewResponseAuditUnattendedCalc(
          agentReferenceNumber = Some("1"),
          forecastIncome = Some(2000),
          forecastIncomeTaxAndNics = Some(120.0),
          forecastAllowancesAndDeductions = Some(100)
        )
        (auditJson.detail \ "calculation" \ "calculationReason").toString contains "Unattended Calculation"
        (auditJson.detail \ "calculation" \ "income").toString() shouldBe "JsDefined(199505)"
        (auditJson.detail \ "calculation" \ "allowancesAndDeductions").toString() shouldBe "JsDefined(500)"
        (auditJson.detail \ "calculation" \ "taxableIncome").toString() shouldBe "JsDefined(198500)"
        (auditJson.detail \ "calculation" \ "taxDue").toString() shouldBe "JsDefined(2010)"
      }
    }
  }

}
