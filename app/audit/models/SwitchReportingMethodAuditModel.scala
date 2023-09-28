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

package audit.models

import audit.Utilities
import auth.MtdItUser
import play.api.data.FormError
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{JsValue, Json}

case class SwitchReportingMethodAuditModel(journeyType: String,
                                           reportingMethodChangeTo: String,
                                           taxYear: String,
                                           errorMessage: Seq[FormError],
                                           messagesApi: MessagesApi
                                          )(implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.SwitchReportingMethod

  override val auditType: String = enums.AuditType.SwitchReportingMethod

  implicit val lang: Lang = Lang("GB")

  override val detail: JsValue = {
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "journeyType" -> journeyType,
        "reportingMethodChangeTo" -> reportingMethodChangeTo.toLowerCase.capitalize,
        "taxYear" -> taxYear,
        "errorMessage" -> errorMessage.flatMap(_.messages.map(messagesApi(_))).headOption
      )
  }
}