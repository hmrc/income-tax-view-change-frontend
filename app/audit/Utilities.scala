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

package audit

import _root_.models.financialDetails.DocumentDetail
import auth.MtdItUser
import play.api.libs.json.{JsObject, Json}
import utils.Utilities.JsonUtil


object Utilities {

  def userAuditDetails(user: MtdItUser[_]): JsObject = Json.obj(
    "nationalInsuranceNumber" -> user.nino,
    "mtditid" -> user.mtditid
  ) ++
    ("agentReferenceNumber", user.arn) ++
    ("saUtr", user.saUtr) ++
    ("credId", user.credId) ++
    userType(user.userType)

  def userType(userType: Option[String]): JsObject = userType match {
    case Some("Agent") => Json.obj("userType" -> "Agent")
    case Some(_) => Json.obj("userType" -> "Individual")
    case None => Json.obj()
  }

  def getChargeType(docDetail: DocumentDetail, latePaymentCharge: Boolean = false): Option[String] = docDetail.documentDescription map {
    case "ITSA- POA 1" => if (latePaymentCharge) "Late payment interest for payment on account 1 of 2" else "Payment on account 1 of 2"
    case "ITSA - POA 2" => if (latePaymentCharge) "Late payment interest for payment on account 2 of 2" else "Payment on account 2 of 2"
    case "TRM New Charge" | "TRM Amend Charge" => if (latePaymentCharge) "Late payment interest for remaining balance" else "Remaining balance"
    case other => other
  }

  def ratePctString(rate: BigDecimal): String = s"$rate%"
}
