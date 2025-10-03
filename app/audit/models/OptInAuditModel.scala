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

import audit.Utilities
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import models.incomeSourceDetails.TaxYear
import play.api.libs.json.{JsObject, JsValue, Json}
import services.optIn.core.OptInProposition

case class OptInAuditModel(
                            optInProposition: OptInProposition,
                            intentTaxYear: TaxYear,
                            resolvedOutcome: ITSAStatusUpdateResponse
                          )(implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.OptInQuarterlyReportingRequest

  override val auditType: String = enums.AuditType.OptInQuarterlyReportingRequest

  val outcome: JsObject = {
    val outcome = resolvedOutcome match {
      case response: ITSAStatusUpdateResponseFailure => Json.obj("isSuccessful" -> false, "failureCategory" -> response.failures.head.code, "failureReason" -> response.failures.head.reason)
      case _: ITSAStatusUpdateResponseSuccess => Json.obj("isSuccessful" -> true)
      case _ => Json.obj("isSuccessful" -> false, "failureCategory" -> "Unknown failure reason", "failureReason" -> "Unknown failure category")
    }
    Json.obj("outcome" -> outcome)
  }

  override val detail: JsValue =
    Utilities.userAuditDetails(user) ++ outcome ++
      Json.obj(
        "optInRequestedFromTaxYear" -> intentTaxYear.formatAsShortYearRange,
        "currentYear" -> optInProposition.currentTaxYear.taxYear.formatAsShortYearRange,
        "beforeITSAStatusCurrentYear" -> optInProposition.currentTaxYear.status,
        "beforeITSAStatusCurrentYear+1" -> optInProposition.nextTaxYear.status,
        "afterAssumedITSAStatusCurrentYear" -> optInProposition.currentTaxYear.expectedItsaStatusAfter(intentTaxYear),
        "afterAssumedITSAStatusCurrentYear+1" -> optInProposition.nextTaxYear.expectedItsaStatusAfter(intentTaxYear)
      )
}
