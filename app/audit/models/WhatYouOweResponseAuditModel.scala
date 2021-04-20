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

import models.financialDetails.{Charge, WhatYouOweChargesList}
import models.outstandingCharges.OutstandingChargesModel
import play.api.libs.json._
import utils.Utilities.JsonUtil

case class WhatYouOweResponseAuditModel(arn:Option[String], affinityGroup: Option[String], saUtr: Option[String],
                                        nino: String, credId: Option[String], mtditid: String,
                                   chargesList: WhatYouOweChargesList) extends ExtendedAuditModel {

  override val transactionName: String = "what-you-owe-response"
  override val auditType: String = "WhatYouOweResponse"

  private val userType: JsObject = affinityGroup match {
    case Some("Agent") => Json.obj("userType" -> "Agent")
    case Some("Individual") => Json.obj("userType" -> "Individual")
    case None => Json.obj()
  }

  def getChargeType(charge: Charge): Option[String] = charge.mainType  map {
    case "SA Payment on Account 1" => "Payment on account 1 of 2"
    case "SA Payment on Account 2" => "Payment on account 2 of 2"
    case "SA Balancing Charge" => "Remaining balance"
    case other => other
    }

  private def chargeDetails(charge: Charge): JsObject = Json.obj(
    "outstandingAmount" -> charge.remainingToPay
  ) ++
    ("chargeType", getChargeType(charge)) ++
    ("dueDate", charge.due)

  private def outstandingChargeDetails(outstandingCharge: OutstandingChargesModel)= Json.obj(
    "chargeType" -> "Remaining balance"
  ) ++
    ("outstandingAmount", outstandingCharge.bcdChargeType.map(_.chargeAmount))++
    ("dueDate", outstandingCharge.bcdChargeType.map(_.relevantDueDate)) ++
    ("accruingInterest", outstandingCharge.aciChargeType.map(_.chargeAmount))

  private val chargeListJson: List[JsObject] = (
    chargesList.overduePaymentList ++
      chargesList.dueInThirtyDaysList ++
      chargesList.futurePayments).map(chargeDetails)++
    chargesList.outstandingChargesModel.map(outstandingChargeDetails)



  override val detail: JsValue = {
    Json.obj("nationalInsuranceNumber" -> nino,
      "mtditid" -> mtditid,
      "charges" -> chargeListJson) ++
      ("agentReferenceNumber", arn) ++
      ("saUtr", saUtr) ++
      userType ++
      ("credId", credId)
  }

}
