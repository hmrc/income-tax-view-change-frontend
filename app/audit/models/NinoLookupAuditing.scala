/*
 * Copyright 2022 HM Revenue & Customs
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

import models.core.{NinoResponseError, NinoResponseSuccess}

object NinoLookupAuditing {

  case class NinoLookupAuditModel(nino: NinoResponseSuccess, mtdRef: String) extends AuditModel {
    override val transactionName: String = enums.TransactionName.NinoLookup
    override val detail: Seq[(String, String)] = Seq(
      "mtdid" -> mtdRef,
      "nino" -> nino.nino
    )
    override val auditType: String = enums.AuditType.NinoLookup
  }

  case class NinoLookupErrorAuditModel(ninoError: NinoResponseError, mtdRef: String) extends AuditModel {
    override val transactionName: String = enums.TransactionName.NinoLookupError
    override val detail: Seq[(String, String)] = Seq(
      "mtdid" -> mtdRef,
      "status" -> ninoError.status.toString,
      "reason" -> ninoError.reason
    )
    override val auditType: String = enums.AuditType.NinoLookupError
  }
}
