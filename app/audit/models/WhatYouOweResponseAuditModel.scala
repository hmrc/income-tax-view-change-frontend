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
import models.financialDetails.{Charge, WhatYouOweChargesList}
import models.outstandingCharges.OutstandingChargesModel
import play.api.libs.json._
import utils.Utilities.JsonUtil

case class WhatYouOweResponseAuditModel(user: MtdItUser[_],
                                        chargesList: WhatYouOweChargesList) extends ExtendedAuditModel {

  override val transactionName: String = "what-you-owe-response"
  override val auditType: String = "WhatYouOweResponse"

  private val chargeListJson: List[JsObject] = (
    chargesList.overduePaymentList ++
      chargesList.dueInThirtyDaysList ++
      chargesList.futurePayments).map(chargeDetails) ++
    chargesList.outstandingChargesModel.map(outstandingChargeDetails)

  override val detail: JsValue = userAuditDetails(user) ++
    Json.obj("charges" -> chargeListJson)

  private def chargeDetails(charge: Charge): JsObject = Json.obj(
    "outstandingAmount" -> charge.remainingToPay
  ) ++
    ("chargeType", getChargeType(charge)) ++
    ("dueDate", charge.due)

  private def outstandingChargeDetails(outstandingCharge: OutstandingChargesModel) = Json.obj(
    "chargeType" -> "Remaining balance"
  ) ++
    ("outstandingAmount", outstandingCharge.bcdChargeType.map(_.chargeAmount)) ++
    ("dueDate", outstandingCharge.bcdChargeType.map(_.relevantDueDate)) ++
    ("accruingInterest", outstandingCharge.aciChargeType.map(_.chargeAmount))



}
