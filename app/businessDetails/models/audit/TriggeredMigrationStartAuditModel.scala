/*
 * Copyright 2026 HM Revenue & Customs
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

package businessDetails.models.audit

import common.auth.MtdItUser
import common.enums.AuditType.TriggeredMigrationStart
import common.enums.TransactionName
import common.models.audit.ExtendedAuditModel
import common.utils.audit.Utilities
import play.api.libs.json.{JsValue, Json}

case class TriggeredMigrationStartAuditModel(referrer: String)(implicit user: MtdItUser[_]) extends ExtendedAuditModel {
  override val transactionName: String = TransactionName.TriggeredMigrationStart
  override val auditType: String = TriggeredMigrationStart
  
  override val detail: JsValue =
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "referrer" -> referrer
      )
}
