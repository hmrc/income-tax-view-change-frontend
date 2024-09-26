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

package models.sessionData

import controllers.agent.sessionUtils.SessionKeys

case class SessionCookieData(mtditid: String,
                             nino: String,
                             utr: String,
                             clientFirstName: Option[String],
                             clientLastName: Option[String]
                            ) {
  val toSessionDataModel: SessionDataModel = {
    SessionDataModel(mtditid = mtditid, nino = nino, utr = utr)
  }

  val toSessionCookieSeq: Seq[(String, String)] = {
    Seq(
      SessionKeys.clientMTDID -> mtditid,
      SessionKeys.clientNino -> nino,
      SessionKeys.clientUTR -> utr
    ) ++ clientFirstName.map(SessionKeys.clientFirstName -> _) ++ clientLastName.map(SessionKeys.clientLastName -> _)
  }
}
