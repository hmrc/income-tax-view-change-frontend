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

import auth.MtdItUser
import implicits.ImplicitDateParser
import models.core.AccountingPeriodModel
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import org.scalatest.WordSpecLike
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.taxYear
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class TaxYearSummaryResponseAuditModelSpec extends WordSpecLike with TestSupport with ImplicitDateParser {

  val transactionName: String = "tax-year-overview-response"
  val auditType: String = "TaxYearOverviewResponse"
  val paymentsLpiPaymentOnAccount1: String = messages("tax-year-summary.payments.lpi.paymentOnAccount1.text")
  val paymentsPaymentOnAccount1: String = messages("tax-year-summary.payments.paymentOnAccount1.text")
  val updateTypeEops: String = messages("updateTab.updateType.eops")

  def taxYearSummaryViewModel(forecastIncome: Option[Int] = None,
                              forecastIncomeTaxAndNics: Option[BigDecimal] = None): TaxYearSummaryViewModel = TaxYearSummaryViewModel(
    timestamp = Some("2017-07-06T12:34:56.789Z".toZonedDateTime.toLocalDate),
    crystallised = Some(false),
    unattendedCalc = false,
    taxDue = 2010.00,
    income = 199505,
    deductions = 500.00,
    totalTaxableIncome = 198500,
    forecastIncome = forecastIncome,
    forecastIncomeTaxAndNics = forecastIncomeTaxAndNics
  )

  def taxYearSummaryViewModelUnattendedCalc(forecastIncome: Option[Int] = None,
                              forecastIncomeTaxAndNics: Option[BigDecimal] = None): TaxYearSummaryViewModel = TaxYearSummaryViewModel(
    timestamp = Some("2017-07-06T12:34:56.789Z".toZonedDateTime.toLocalDate),
    crystallised = Some(false),
    unattendedCalc = true,
    taxDue = 2010.00,
    income = 199505,
    deductions = 500.00,
    totalTaxableIncome = 198500,
    forecastIncome = forecastIncome,
    forecastIncomeTaxAndNics = forecastIncomeTaxAndNics
  )

  def payments(hasDunningLock: Boolean): List[DocumentDetailWithDueDate] = {
    List(DocumentDetailWithDueDate(DocumentDetail("2020", "1040000123", Some("ITSA- POA 1"), Some("documentText"), Some(1400.0), Some(1400.0), LocalDate.parse("2018-03-29"),
      Some(80), Some(100), None, Some(LocalDate.parse("2018-03-29")), Some(LocalDate.parse("2018-06-15")),
      Some(100), Some(100), None, None), Some(LocalDate.parse("2019-05-15")), true, hasDunningLock))
  }


  val docDetail: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("ITSA- POA 1"),
    documentText = Some("documentText"),
    originalAmount = Some(10.34),
    outstandingAmount = Some(0),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  val docDateDetail: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail,
    dueDate = Some(LocalDate.now())
  )

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val updates: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "testId",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "EOPS",
          dateReceived = Some(LocalDate.now),
          periodKey = "EOPS"
        )
      )
    )
  ))

  val business = List(BusinessDetailsModel(
    Some("testId"),
    Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
    Some("Test Trading Name"),
    Some(getCurrentTaxYearEnd)
  ))

  def taxYearOverviewResponseAuditFull(userType: Option[String] = Some("Agent"),
                                       agentReferenceNumber: Option[String],
                                       paymentHasADunningLock: Boolean = false,
                                       featureSwitch7b: Boolean = false,
                                       forecastIncome: Option[Int] = None,
                                       forecastIncomeTaxAndNics: Option[BigDecimal] = None): TaxYearSummaryResponseAuditModel =
    TaxYearSummaryResponseAuditModel(
      mtdItUser = MtdItUser(
        mtditid = "mtditid",
        nino = "nino",
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel("mtditid", None, business, None),
        btaNavPartial = None,
        saUtr = Some("saUtr"),
        credId = Some("credId"),
        userType = userType,
        arn = agentReferenceNumber
      )(FakeRequest()),
      payments = payments(paymentHasADunningLock),
      updates = updates,
      taxYearSummaryViewModel = Some(taxYearSummaryViewModel(
        forecastIncome = forecastIncome,
        forecastIncomeTaxAndNics = forecastIncomeTaxAndNics)
      ),
    )

  def taxYearOverviewResponseAuditUnattendedCalc(userType: Option[String] = Some("Agent"),
                                       agentReferenceNumber: Option[String],
                                       paymentHasADunningLock: Boolean = false,
                                       featureSwitch7b: Boolean = false,
                                       forecastIncome: Option[Int] = None,
                                       forecastIncomeTaxAndNics: Option[BigDecimal] = None): TaxYearSummaryResponseAuditModel =
    TaxYearSummaryResponseAuditModel(
      mtdItUser = MtdItUser(
        mtditid = "mtditid",
        nino = "nino",
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel("mtditid", None, business, None),
        btaNavPartial = None,
        saUtr = Some("saUtr"),
        credId = Some("credId"),
        userType = userType,
        arn = agentReferenceNumber
      )(FakeRequest()),
      payments = payments(paymentHasADunningLock),
      updates = updates,
      taxYearSummaryViewModel = Some(taxYearSummaryViewModelUnattendedCalc(
        forecastIncome = forecastIncome,
        forecastIncomeTaxAndNics = forecastIncomeTaxAndNics)
      ),
    )

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
      "the user type is Agent" in {
        taxYearOverviewResponseAuditFull(
          userType = Some("Agent"),
          agentReferenceNumber = Some("agentReferenceNumber"),
        ).detail shouldBe Json.obj(
          "nationalInsuranceNumber" -> "nino",
          "mtditid" -> "mtditid",
          "saUtr" -> "saUtr",
          "credId" -> "credId",
          "userType" -> "Agent",
          "agentReferenceNumber" -> "agentReferenceNumber",
          "taxYearOverview" -> Json.obj(
            "calculationDate" -> "2017-07-06",
            "calculationAmount" -> 2010,
            "isCrystallised" -> false,
            "forecastAmount" -> null
          ),
          "forecast" -> Json.obj(
            "income" -> null,
            "taxableIncome" -> null,
            "taxDue" -> null
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
            "paymentType" -> paymentsLpiPaymentOnAccount1,
            "underReview" -> false,
            "status" -> "part-paid"
          )),
          "updates" -> Seq(Json.obj(
            "incomeSource" -> "Test Trading Name",
            "dateSubmitted" -> LocalDate.now.toString,
            "updateType" -> updateTypeEops
          ))
        )
      }

      "the user type is Individual" in {
        taxYearOverviewResponseAuditFull(
          userType = Some("Individual"),
          agentReferenceNumber = None,
          paymentHasADunningLock = true
        ).detail shouldBe Json.obj(
          "nationalInsuranceNumber" -> "nino",
          "mtditid" -> "mtditid",
          "saUtr" -> "saUtr",
          "credId" -> "credId",
          "userType" -> "Individual",
          "taxYearOverview" -> Json.obj(
            "calculationDate" -> "2017-07-06",
            "calculationAmount" -> 2010,
            "isCrystallised" -> false,
            "forecastAmount" -> None
          ),
          "forecast" -> Json.obj(
            "income" -> None,
            "taxableIncome" -> None,
            "taxDue" -> None
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
            "paymentType" -> paymentsLpiPaymentOnAccount1,
            "underReview" -> true,
            "status" -> "part-paid"
          )),
          "updates" -> Seq(Json.obj(
            "incomeSource" -> "Test Trading Name",
            "dateSubmitted" -> LocalDate.now.toString,
            "updateType" -> updateTypeEops
          ))
        )
      }
    }
  }


  "TaxYear audit model" should {
    "present a full audit model" when {
      "the R7b feature switch is on" in {
        val auditJson = taxYearOverviewResponseAuditFull(
          agentReferenceNumber = Some("1"),
          featureSwitch7b = true,
          forecastIncome = Some(2000),
          forecastIncomeTaxAndNics = Some(120.0)
        )

        (auditJson.detail \ "taxYearOverview" \ "calculationAmount").toString() shouldBe "JsDefined(2010)"
        (auditJson.detail \ "taxYearOverview" \ "isCrystallised").toString() shouldBe "JsDefined(false)"
        (auditJson.detail \ "taxYearOverview" \ "forecastAmount").toString() shouldBe "JsDefined(2000)"
        (auditJson.detail \ "forecast").toOption.get shouldBe
          Json.obj(
            "income" -> 2000,
            "taxableIncome" -> 2000,
            "taxDue" -> 120
          )
      }
    }
  }

  "TaxYear audit model test" should {
    "present a full audit model test" when {
      "the R7b feature switch is on test" in {
        val auditJson = taxYearOverviewResponseAuditFull(
          agentReferenceNumber = Some("1"),
          featureSwitch7b = true,
          forecastIncome = Some(2000),
          forecastIncomeTaxAndNics = Some(120.0)
        )
        (auditJson.detail \ "calculation" \ "calculationReason").toString contains "customerRequest"
        (auditJson.detail \ "calculation" \ "income").toString() shouldBe "JsDefined(199505)"
        (auditJson.detail \ "calculation" \ "allowancesAndDeductions").toString() shouldBe "JsDefined(500)"
        (auditJson.detail \ "calculation" \ "taxableIncome").toString() shouldBe "JsDefined(198500)"
        (auditJson.detail \ "calculation" \ "taxDue").toString() shouldBe "JsDefined(2010)"
      }
    }

    "present a full audit model test unattended" when {
      "the R7b feature switch is on test" in {
        val auditJson = taxYearOverviewResponseAuditUnattendedCalc(
          agentReferenceNumber = Some("1"),
          featureSwitch7b = true,
          forecastIncome = Some(2000),
          forecastIncomeTaxAndNics = Some(120.0)
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
