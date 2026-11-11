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

package audit.models

import auth.MtdItUser
import enums.AuditType.ChooseTaxYearsRangeSubmitted
import enums.TransactionName
import models.audit.ExtendedAuditModel
import utils.audit.Utilities
import play.api.libs.json.{JsValue, Json}

case class ChooseTaxYearsRangeSubmittedAuditModel(taxYearsPresented: Seq[String], taxYearRangeSelected: String, taxYearRadioSelected: String)(implicit user: MtdItUser[_]) extends ExtendedAuditModel {
  override val transactionName: String = TransactionName.ChooseTaxYearsRangeSubmitted
  override val auditType: String = ChooseTaxYearsRangeSubmitted

  override val detail: JsValue =
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "taxYearsPresented" -> taxYearsPresented,
        "taxYearRangeSelected" -> taxYearRangeSelected,
        "taxYearRadioSelected" -> taxYearRadioSelected
      )
}
