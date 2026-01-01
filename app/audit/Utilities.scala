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

import auth.MtdItUser
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.Utilities.JsonUtil


object Utilities {

  private def getBaseDetails(user: MtdItUser[_]): JsObject =
    Json.obj( "mtditid" -> user.mtditid) ++
    Json.obj("agentReferenceNumber"->user.arn) ++
    Json.obj("saUtr"-> user.saUtr) ++
    Json.obj("credId"-> user.credId) ++
    userType(user.userType, user.isSupportingAgent)

  def userAuditDetails(user: MtdItUser[_]): JsObject =
    Json.obj("nino" -> user.nino) ++ getBaseDetails(user)

  def userType(userType: Option[AffinityGroup], isSupportingAgent: Boolean = false): JsObject =
    userType match {
      case Some(Agent) => Json.obj("userType" -> "Agent", "isSupportingAgent" -> isSupportingAgent)
      case Some(_) => Json.obj("userType" -> "Individual")
      case None => Json.obj()
    }

  def ratePctString(rate: BigDecimal): String = s"$rate%"
}
