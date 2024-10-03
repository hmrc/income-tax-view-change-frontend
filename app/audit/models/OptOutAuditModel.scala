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

package audit.models

import audit.models.OptOutAuditModel.format
import play.api.libs.json.{JsObject, JsValue, Json, OFormat}

case class OptOutAuditModel(nino: String, outcome: Outcome, optOutRequestedFromTaxYear: String, currentYear: String,
                            beforeITSAStatusCurrentYearMinusOne: String, beforeITSAStatusCurrentYear: String,
                            beforeITSAStatusCurrentYearPlusOne: String, afterAssumedITSAStatusCurrentYearMinusOne: String,
                            afterAssumedITSAStatusCurrentYear: String, afterAssumedITSAStatusCurrentYearPlusOne: String,
                            currentYearMinusOneCrystallised: Boolean) extends ExtendedAuditModel {


  override val transactionName: String = enums.TransactionName.ClientDetailsConfirmed

  override val auditType: String = enums.AuditType.ClientDetailsConfirmed

  private val optOutDetailsJson: JsObject = Json.obj(
    "nino" -> nino,
    "outcome" -> outcome,
    "optOutRequestedFromTaxYear" -> optOutRequestedFromTaxYear,
    "currentYear" -> currentYear,
    "beforeITSAStatusCurrentYearMinusOne" -> beforeITSAStatusCurrentYearMinusOne,
    "beforeITSAStatusCurrentYear" -> beforeITSAStatusCurrentYear,
    "beforeITSAStatusCurrentYearPlusOne" -> beforeITSAStatusCurrentYearPlusOne,
    "afterAssumedITSAStatusCurrentYearMinusOne" -> afterAssumedITSAStatusCurrentYearMinusOne,
    "afterAssumedITSAStatusCurrentYear" -> afterAssumedITSAStatusCurrentYear,
    "afterAssumedITSAStatusCurrentYearPlusOne" -> afterAssumedITSAStatusCurrentYearMinusOne,
    "currentYearMinusOneCrystallised" -> currentYearMinusOneCrystallised)

  override val detail: JsValue = optOutDetailsJson

}

case class Outcome(isSuccessful: Boolean = true,
                   failureCategory: String = "API_FAILURE",
                   failureReason: String = "Failure reasons")

object OptOutAuditModel {

  implicit val format: OFormat[Outcome] = Json.format[Outcome]
}