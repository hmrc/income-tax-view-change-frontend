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

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import implicits.ImplicitDateParser
import models.updateIncomeSource.UpdateIncomeSourceResponseError
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Utilities.JsonUtil

case class CeaseIncomeSourceAuditModel(incomeSourceType: IncomeSourceType,
                                       cessationDate: String,
                                       incomeSourceId: String,
                                       updateIncomeSourceErrorResponse: Option[UpdateIncomeSourceResponseError])(implicit user: MtdItUser[_], hc: HeaderCarrier)
  extends ExtendedAuditModel with ImplicitDateParser {

  private val isSuccessful = updateIncomeSourceErrorResponse.isEmpty
  private val outcome: JsObject = {
    val outcome: JsObject = Json.obj("isSuccessful" -> isSuccessful)

    if (isSuccessful) outcome
    else outcome ++ Json.obj(
      "isSuccessful" -> isSuccessful,
      "failureCategory" -> "API_FAILURE",
      "failureReason" -> updateIncomeSourceErrorResponse.get.reason)
  }

  override val transactionName: String = enums.TransactionName.CeaseIncomeSource
  override val auditType: String = enums.AuditType.CeaseIncomeSource
  override val detail: JsValue = {
    val details = userAuditDetails(user) ++
      Json.obj("outcome" -> outcome,
        "journeyType" -> incomeSourceType.journeyType,
        "dateBusinessStopped" -> cessationDate,
        "incomeSourceID" -> incomeSourceId)

    incomeSourceType match {
      case SelfEmployment =>
        val businessName = user.incomeSources.getSoleTraderBusiness(incomeSourceId).flatMap(_.tradingName)
        details ++ ("businessName", businessName)
      case _ =>
        details
    }
  }
}
