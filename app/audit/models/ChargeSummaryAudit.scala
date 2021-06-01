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

import auth.MtdItUser
import models.financialDetails.DocumentDetailWithDueDate
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities._


case class ChargeSummaryAudit(mtdItUser: MtdItUser[_],
                              docDateDetail: DocumentDetailWithDueDate,
                              agentReferenceNumber: Option[String]) extends ExtendedAuditModel {

  private val userType: JsObject = mtdItUser.userType match {
    case Some("Agent") => Json.obj("userType" -> "Agent")
    case Some(_) => Json.obj("userType" -> "Individual")
    case None => Json.obj()
  }

  val getChargeType: String = docDateDetail.documentDetail.documentDescription match {
    case Some("ITSA- POA 1") => "Payment on account 1 of 2"
    case Some("ITSA - POA 2") => "Payment on account 2 of 2"
    case Some("ITSA- Bal Charge") => "Remaining balance"
    case error => {
      Logger.error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
      "unknownCharge"
    }
  }

  private val chargeDetails: JsObject = Json.obj(
    "chargeType" -> getChargeType,
    "remainingToPay" -> docDateDetail.documentDetail.remainingToPay
  ) ++
    ("dueDate", docDateDetail.dueDate) ++
    ("paymentAmount", docDateDetail.documentDetail.originalAmount)


  override val transactionName: String = "charge-summary"
  override val detail: JsValue = {
    Json.obj("nationalInsuranceNumber" -> mtdItUser.nino) ++
      ("agentReferenceNumber", mtdItUser.arn) ++
      ("saUtr", mtdItUser.saUtr) ++
      userType ++
      ("credId", mtdItUser.credId) ++
      Json.obj("mtditid" -> mtdItUser.mtditid) ++
      Json.obj("charge" -> chargeDetails)
  }
  override val auditType: String = "ChargeSummary"

}
