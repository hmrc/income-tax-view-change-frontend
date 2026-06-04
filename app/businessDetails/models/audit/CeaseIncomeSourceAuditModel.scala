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

package businessDetails.models.audit

import common.utils.audit.Utilities.userAuditDetails
import businessDetails.models.updateIncomeSource.UpdateIncomeSourceResponseError
import common.auth.MtdItUser
import common.enums.AuditType.CeaseIncomeSource
import common.enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import common.enums.TransactionName
import common.implicits.ImplicitDateParser
import common.models.audit.ExtendedAuditModel
import common.models.core.IncomeSourceId
import play.api.libs.json.{JsObject, JsValue, Json}

case class CeaseIncomeSourceAuditModel(incomeSourceType: IncomeSourceType,
                                       cessationDate: String,
                                       incomeSourceId: IncomeSourceId,
                                       updateIncomeSourceErrorResponse: Option[UpdateIncomeSourceResponseError],
                                       isTrigMig: Boolean
                                      )(implicit user: MtdItUser[_])
  extends ExtendedAuditModel with ImplicitDateParser {

  private val isSuccessful = updateIncomeSourceErrorResponse.isEmpty
  private val outcome: JsObject = {
    val outcome: JsObject = Json.obj("isSuccessful" -> isSuccessful)

    if (isSuccessful) outcome
    else outcome ++ Json.obj(
      "isSuccessful" -> isSuccessful,
      "failureCategory" -> "API_FAILURE",
      "failureReason" -> updateIncomeSourceErrorResponse.get.reason
    )
  }

  override val transactionName: String = TransactionName.CeaseIncomeSource
  override val auditType: String = CeaseIncomeSource
  override val detail: JsValue = {
    val details = userAuditDetails(user) ++
      Json.obj(
        "outcome" -> outcome,
        "journeyType" -> incomeSourceType.journeyType,
        "dateBusinessStopped" -> cessationDate,
        "incomeSourceID" -> incomeSourceId.value,
        "isTriggeredMigration" -> isTrigMig
      )

    incomeSourceType match {
      case SelfEmployment =>
        val businessName = user.incomeSources
          .getSoleTraderBusiness(incomeSourceId.value).flatMap(_.tradingName)
        details ++ Json.obj("businessName"-> businessName)
      case _ =>
        details
    }
  }
}
