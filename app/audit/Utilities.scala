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

package audit

import _root_.models.financialDetails.DocumentDetail
import auth.MtdItUserBase
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.Utilities.JsonUtil


object Utilities {


  private def getBaseDetails(user: MtdItUserBase[_]): JsObject = Json.obj(
    "mtditid" -> user.mtditid) ++
    ("agentReferenceNumber", user.arn) ++
    ("saUtr", user.saUtr) ++
    ("credId", user.credId) ++
    userType(user.userType)

  def userAuditDetails(user: MtdItUserBase[_]): JsObject =
    getBaseDetails(user) ++ Json.obj("nino" -> user.nino)

  def userAuditDetailsNino(user: MtdItUserBase[_]): JsObject =
    getBaseDetails(user) ++ Json.obj("nino" -> user.nino)


  def userType(userType: Option[AffinityGroup]): JsObject = userType match {
    case Some(Agent) => Json.obj("userType" -> "Agent")
    case Some(_) => Json.obj("userType" -> "Individual")
    case None => Json.obj()
  }

  def getChargeType(docDetail: DocumentDetail, latePaymentCharge: Boolean = false): Option[String] =
    (docDetail.documentDescription, docDetail.documentText) match {
    case (_, Some(documentText)) if (documentText.contains("Class 2 National Insurance")) => Some("Class 2 National Insurance")
    case(_, Some(documentDescription)) if (documentDescription.contains("Cancelled PAYE Self Assessment")) => Some("Cancelled PAYE Self Assessment (through your PAYE tax code)")
    case (Some("ITSA- POA 1"), _) => if (latePaymentCharge) Some("Late payment interest for payment on account 1 of 2") else Some("Payment on account 1 of 2")
    case (Some("ITSA - POA 2"),_) => if (latePaymentCharge) Some("Late payment interest for payment on account 2 of 2") else Some("Payment on account 2 of 2")
    case (Some("TRM New Charge") | Some("TRM Amend Charge"),_ ) => if (latePaymentCharge) Some("Late payment interest for remaining balance") else Some("Remaining balance")
    case (other, _) => other
  }

  def ratePctString(rate: BigDecimal): String = s"$rate%"
}
