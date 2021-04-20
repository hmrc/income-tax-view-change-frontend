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
import models.financialDetails.{Charge, SubItem}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve.Name

class ChargeSummaryAuditSpec extends WordSpecLike with MustMatchers {

  val transactionName: String = "charge-summary"
  val auditType: String = "ChargeSummary"
  val charge: Charge = Charge(
    taxYear = taxYear,
    transactionId = "1040000124",
    transactionDate = Some("2019-05-16"),
    `type` = Some("POA1"),
    totalAmount = Some(43.21),
    originalAmount = Some(10.34),
    outstandingAmount = Some(0),
    clearedAmount = Some(10.34),
    chargeType = Some("POA1"),
    mainType = Some("SA Payment on Account 1"),
    items = Some(Seq(SubItem(subItemId = Some("003"), amount = Some(110), clearingDate = Some("2019-05-17"), clearingReason = Some("03"),
      outgoingPaymentMethod = Some("C"), paymentReference = Some("C"), paymentAmount = Some(5000), dueDate = Some("2019-05-19"),
      paymentMethod = Some("C"), paymentId = Some("081203010026-000003"))
    )))

  val getChargeType: String = charge.mainType match {
    case Some("SA Payment on Account 1") => "Payment on account 1 of 2"
    case Some("SA Payment on Account 2") => "Payment on account 2 of 2"
    case Some("SA Balancing Charge") => "Remaining balance"
    case error => {
      Logger.error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
      "unknownCharge"
    }
  }

  def chargeSummaryAuditFull(userType: Option[String] = Some("Agent"),
                             charge: Charge,
                             agentReferenceNumber: Option[String]): ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel("mtditid", None, Nil, None),
      saUtr = Some("saUtr"),
      credId = Some("credId"),
      userType = userType
    ),
    charge = charge,
    agentReferenceNumber = Some("agentReferenceNumber")
  )

  val chargeSummaryAuditMin: ChargeSummaryAudit = ChargeSummaryAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = None,
      incomeSources = IncomeSourceDetailsModel("mtditid", None, Nil, None),
      saUtr = None,
      credId = None,
      userType = None
    ),
    charge = charge,
    agentReferenceNumber = None
  )

  "ChargeSummaryAudit(mtdItUser, charge, agentReferenceNumber)" should {

    s"have the correct transaction name of '$transactionName'" in {
      chargeSummaryAuditFull(
        charge = charge,
        agentReferenceNumber = Some("arn")
      ).transactionName mustBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      chargeSummaryAuditFull(
        charge = charge,
        agentReferenceNumber = Some("arn")
      ).auditType mustBe auditType
    }

    "have the correct details for the audit event" when {
      "the charge summary audit has all detail" when {
        "there are charge details" in {
          chargeSummaryAuditFull(
            userType = Some("Agent"),
            charge = charge,
            agentReferenceNumber = Some("arn")
          ).detail mustBe Json.obj(
            "agentReferenceNumber" -> "agentReferenceNumber",
            "nationalInsuranceNumber" -> "nino",
            "saUtr" -> "saUtr",
            "userType" -> "Agent",
            "credId" -> "credId",
            "mtditid" -> "mtditid",
            "charge" -> Json.obj(
              "chargeType" -> getChargeType,
              "dueDate" -> charge.due,
              "paymentAmount" -> charge.originalAmount,
              "paidToDate" -> charge.clearedAmount,
              "remainingToPay" -> charge.remainingToPay
            )
          )
        }

        "the charge summary audit has minimal details" in {
          chargeSummaryAuditMin.detail mustBe Json.obj(
            "nationalInsuranceNumber" -> "nino",
            "mtditid" -> "mtditid",
            "charge" -> Json.obj(
              "chargeType" -> getChargeType,
              "dueDate" -> charge.due,
              "paymentAmount" -> charge.originalAmount,
              "paidToDate" -> charge.clearedAmount,
              "remainingToPay" -> charge.remainingToPay
            )
          )
        }
      }
    }

  }
}
