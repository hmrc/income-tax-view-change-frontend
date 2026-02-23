/*
 * Copyright 2024 HM Revenue & Customs
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

package models.sessionData

import controllers.agent.sessionUtils.SessionKeys
import services.agent.ClientDetailsService.ClientDetails

case class SessionCookieData(mtditid: String,
                             nino: String,
                             utr: String,
                             clientFirstName: Option[String],
                             clientLastName: Option[String],
                             isSupportingAgent: Boolean = false
                            ) {
  val toSessionDataModel: SessionDataModel = {
    SessionDataModel(mtditid = mtditid, nino = nino, utr = utr, isSupportingAgent)
  }

  @deprecated("To be decommission after switch to income-tax-session-data")
  val toSessionCookieSeq: Seq[(String, String)] = {
    Seq(
      SessionKeys.clientMTDID -> mtditid,
      SessionKeys.clientNino -> nino,
      SessionKeys.clientUTR -> utr,
      SessionKeys.isSupportingAgent -> isSupportingAgent.toString
    ) ++ clientFirstName.map(SessionKeys.clientFirstName -> _) ++ clientLastName.map(SessionKeys.clientLastName -> _)
  }
}

object SessionCookieData {

  def apply(cd: ClientDetails, utr: String, isSupportingAgent: Boolean): SessionCookieData = {
    SessionCookieData(
      cd.mtdItId,
      cd.nino,
      utr,
      cd.firstName,
      cd.lastName,
      isSupportingAgent)
  }
}