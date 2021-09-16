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

import java.time.LocalDate

import assets.BaseTestConstants.taxYear
import assets.CalcBreakdownTestConstants.calculationDataSuccessModel
import assets.FinancialDetailsTestConstants.financialDetailsModel
import auth.MtdItUser
import models.calculation.Calculation
import models.core.AccountingPeriodModel
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.nextUpdates.{ObligationsModel, NextUpdateModel, NextUpdatesModel}
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name

class TaxYearOverviewResponseAuditModelSpec extends WordSpecLike with MustMatchers {

  val transactionName: String = "tax-year-overview-response"
  val auditType: String = "TaxYearOverviewResponse"

  val calculation: Calculation = calculationDataSuccessModel

  val payments: List[DocumentDetailWithDueDate] = financialDetailsModel(2020).getAllDocumentDetailsWithDueDates


  val docDetail: DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some("ITSA- POA 1"),
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
    "testId",
    AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
    Some("Test Trading Name"), None, None, None, None, None, None, None,
    Some(getCurrentTaxYearEnd)
  ))

  def taxYearOverviewResponseAuditFull(userType: Option[String] = Some("Agent"),
                                       agentReferenceNumber: Option[String]): TaxYearOverviewResponseAuditModel =
    TaxYearOverviewResponseAuditModel(
      mtdItUser = MtdItUser(
        mtditid = "mtditid",
        nino = "nino",
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel("mtditid", None, business, None),
        saUtr = Some("saUtr"),
        credId = Some("credId"),
        userType = userType,
        arn = agentReferenceNumber
      )(FakeRequest()),
      agentReferenceNumber = agentReferenceNumber,
      calculation = calculation,
      payments = payments,
      updates = updates
    )

  "TaxYearOverviewResponseAuditModel(mtdItUser, agentReferenceNumber, calculation, payments, updates)" should {

    s"have the correct transaction name of '$transactionName'" in {
      taxYearOverviewResponseAuditFull(
        agentReferenceNumber = Some("1")
      ).transactionName mustBe transactionName
    }

    s"have the correct audit type of '$auditType'" in {
      taxYearOverviewResponseAuditFull(
        agentReferenceNumber = Some("1")
      ).auditType mustBe auditType
    }

    "have the correct details for the audit event" when {
      "the user type is Agent" in {
        taxYearOverviewResponseAuditFull(
          userType = Some("Agent"),
          agentReferenceNumber = Some("agentReferenceNumber"),
        ).detail mustBe Json.obj(
          "nationalInsuranceNumber" -> "nino",
          "mtditid" -> "mtditid",
          "saUtr" -> "saUtr",
          "credId" -> "credId",
          "userType" -> "Agent",
          "agentReferenceNumber" -> "agentReferenceNumber",
          "calculation" -> Json.obj(
            "income" -> 199505,
            "allowancesAndDeductions" -> 500,
            "taxableIncome" -> 198500,
            "taxDue" -> 2010
          ),
          "payments" -> Seq(Json.obj(
            "amount" -> 1400,
            "dueDate" -> "2019-05-15",
            "paymentType" -> "Payment on account 1 of 2",
            "status" -> "unpaid"
          )),
          "updates" -> Seq(Json.obj(
            "incomeSource" -> "Test Trading Name",
            "dateSubmitted" -> LocalDate.now.toString,
            "updateType" -> "Annual Update"
          ))
        )
      }

      "the user type is Individual" in {
        taxYearOverviewResponseAuditFull(
          userType = Some("Individual"),
          agentReferenceNumber = None,
        ).detail mustBe Json.obj(
          "nationalInsuranceNumber" -> "nino",
          "mtditid" -> "mtditid",
          "saUtr" -> "saUtr",
          "credId" -> "credId",
          "userType" -> "Individual",
          "calculation" -> Json.obj(
            "income" -> 199505,
            "allowancesAndDeductions" -> 500,
            "taxableIncome" -> 198500,
            "taxDue" -> 2010
          ),
          "payments" -> Seq(Json.obj(
            "amount" -> 1400,
            "dueDate" -> "2019-05-15",
            "paymentType" -> "Payment on account 1 of 2",
            "status" -> "unpaid"
          )),
          "updates" -> Seq(Json.obj(
            "incomeSource" -> "Test Trading Name",
            "dateSubmitted" -> LocalDate.now.toString,
            "updateType" -> "Annual Update"
          ))
        )
      }
    }
  }
}
