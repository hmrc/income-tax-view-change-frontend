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

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import play.api.libs.json.JsValue

case class PaymentHistoryRequestAuditModel(mtdItUser: MtdItUser[_]) extends ExtendedAuditModel {

  override val transactionName: String = "payment-history-request"
  override val auditType: String = "PaymentHistoryRequest"
  override val detail: JsValue = userAuditDetails(mtdItUser)

}
