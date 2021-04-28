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

import audit.Utilities._
import auth.MtdItUser
import models.financialDetails.Charge
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities._


case class ChargeSummaryAudit(mtdItUser: MtdItUser[_],
                              charge: Charge) extends ExtendedAuditModel {

  val getChargeType: String = charge.mainType match {
    case Some("SA Payment on Account 1") => "Payment on account 1 of 2"
    case Some("SA Payment on Account 2") => "Payment on account 2 of 2"
    case Some("SA Balancing Charge") => "Remaining balance"
    case error =>
      Logger.error(s"[Charge][getChargeTypeKey] Missing or non-matching charge type: $error found")
      "unknownCharge"
  }

  private val chargeDetails: JsObject = Json.obj(
    "chargeType" -> getChargeType,
    "remainingToPay" -> charge.remainingToPay
  ) ++
    ("dueDate", charge.due) ++
    ("paymentAmount", charge.originalAmount) ++
    ("paidToDate", charge.clearedAmount)


  override val transactionName: String = "charge-summary"
  override val detail: JsValue = {
    Json.obj("nationalInsuranceNumber" -> mtdItUser.nino) ++
      ("agentReferenceNumber", mtdItUser.arn) ++
      ("saUtr", mtdItUser.saUtr) ++
      userType(mtdItUser.userType) ++
      ("credId", mtdItUser.credId) ++
      Json.obj("mtditid" -> mtdItUser.mtditid) ++
      Json.obj("charge" -> chargeDetails)
  }
  override val auditType: String = "ChargeSummary"

}
