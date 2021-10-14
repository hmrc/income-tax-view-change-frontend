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

import assets.BaseTestConstants._
import auth.MtdItUser
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class ChargeSummaryAuditSpec extends WordSpecLike with MustMatchers {

  val transactionName: String = "charge-summary"
  val auditType: String = "ChargeSummary"

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

  val getChargeType: String = docDetail.documentDescription match {
    case Some("ITSA- POA 1") => "Payment on account 1 of 2"
    case Some("ITSA - POA 2") => "Payment on account 2 of 2"
    case Some("TRM New Charge") | Some("TRM Amend Charge") => "balancingCharge.text"
    case error =>
      Logger("application").error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
      "unknownCharge"
  }

  def chargeSummaryAuditFull(userType: Option[String] = Some("Agent"),
                             docDateDetails: DocumentDetailWithDueDate,
                             agentReferenceNumber: Option[String] = Some("agentReferenceNumber")): ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel("mtditid", None, Nil, None),
      saUtr = Some("saUtr"),
      credId = Some("credId"),
      userType = userType,
      arn = agentReferenceNumber
    ),
    docDateDetail = docDateDetail,
    agentReferenceNumber = Some("agentReferenceNumber")
  )

  val chargeSummaryAuditMin: ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = None,
      incomeSources = IncomeSourceDetailsModel("mtditid", None, List.empty, None),
      saUtr = None,
      credId = None,
      userType = None,
      arn = None
    ),
    docDateDetail = docDateDetail,
    agentReferenceNumber = None
  )

  "ChargeSummaryAudit(mtdItUser, charge, agentReferenceNumber)" should {

    s"have the correct transaction name of '$transactionName'" in {
      chargeSummaryAuditFull(None,
        docDateDetail,
        agentReferenceNumber = Some("arn")
      ).transactionName mustBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      chargeSummaryAuditFull(None,
        docDateDetail,
        agentReferenceNumber = Some("arn")
      ).auditType mustBe auditType
    }

    "have the correct details for the audit event" when {
      "the charge summary audit has all detail" when {
        "there are charge details" in {
          chargeSummaryAuditFull(
            userType = Some("Agent"),
            docDateDetail,
            agentReferenceNumber = Some("agentReferenceNumber")
          ).detail mustBe Json.obj(
            "agentReferenceNumber" -> "agentReferenceNumber",
            "nationalInsuranceNumber" -> "nino",
            "saUtr" -> "saUtr",
            "userType" -> "Agent",
            "credId" -> "credId",
            "mtditid" -> "mtditid",
            "agentReferenceNumber" -> "agentReferenceNumber",
            "charge" -> Json.obj(
              "chargeType" -> getChargeType,
              "dueDate" -> docDateDetail.dueDate,
              "paymentAmount" -> docDetail.originalAmount,
              "remainingToPay" -> docDetail.remainingToPay
            )
          )
        }

        "the charge summary audit has minimal details" in {
          chargeSummaryAuditMin.detail mustBe Json.obj(
            "nationalInsuranceNumber" -> "nino",
            "mtditid" -> "mtditid",
            "charge" -> Json.obj(
              "chargeType" -> getChargeType,
              "dueDate" -> docDateDetail.dueDate,
              "paymentAmount" -> docDetail.originalAmount,
              "remainingToPay" -> docDetail.remainingToPay
            )
          )
        }
      }
    }

  }
}
