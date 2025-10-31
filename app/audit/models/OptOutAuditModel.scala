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

import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus
import play.api.libs.json._
import services.optout.OptOutProposition
import uk.gov.hmrc.auth.core.AffinityGroup

case class Outcome(
                    isSuccessful: Boolean,
                    failureCategory: Option[String],
                    failureReason: Option[String]
                  )


object Outcome {
  implicit val format: OFormat[Outcome] = Json.format[Outcome]
}

case class OptOutAuditModel(
                             saUtr: Option[String],
                             credId: Option[String],
                             userType: Option[AffinityGroup],
                             agentReferenceNumber: Option[String],
                             mtditid: String,
                             nino: String,
                             outcome: Outcome,
                             optOutRequestedFromTaxYear: String,
                             currentYear: String,
                             `beforeITSAStatusCurrentYear-1`: ITSAStatus,
                             beforeITSAStatusCurrentYear: ITSAStatus,
                             `beforeITSAStatusCurrentYear+1`: ITSAStatus,
                             `afterAssumedITSAStatusCurrentYear-1`: ITSAStatus,
                             afterAssumedITSAStatusCurrentYear: ITSAStatus,
                             `afterAssumedITSAStatusCurrentYear+1`: ITSAStatus,
                             `currentYear-1Crystallised`: Boolean
                           ) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.OptOutQuarterlyReportingRequest

  override val auditType: String = enums.AuditType.OptOutQuarterlyReportingRequest

  override val detail: JsValue = Json.toJson(this)
}

case class OptOutCompleteAuditModel(
                                     saUtr: Option[String],
                                     credId: Option[String],
                                     userType: Option[AffinityGroup],
                                     agentReferenceNumber: Option[String],
                                     mtditid: String,
                                     nino: String,
                                     outcome: List[Outcome],
                                     optOutRequestedFromTaxYear: String,
                                     currentYear: String,
                                     `beforeITSAStatusCurrentYear-1`: ITSAStatus,
                                     beforeITSAStatusCurrentYear: ITSAStatus,
                                     `beforeITSAStatusCurrentYear+1`: ITSAStatus,
                                     `afterAssumedITSAStatusCurrentYear-1`: Option[ITSAStatus],
                                     afterAssumedITSAStatusCurrentYear: Option[ITSAStatus],
                                     `afterAssumedITSAStatusCurrentYear+1`: Option[ITSAStatus],
                                     `currentYear-1Crystallised`: Boolean
                                   ) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.OptOutQuarterlyReportingRequest

  override val auditType: String = enums.AuditType.OptOutQuarterlyReportingRequest

  override val detail: JsValue = Json.toJson(this)
}

object OptOutCompleteAuditModel {

  implicit val format: OFormat[OptOutCompleteAuditModel] = Json.format[OptOutCompleteAuditModel]
}


object OptOutAuditModel {

  implicit val format: OFormat[OptOutAuditModel] = Json.format[OptOutAuditModel]

  def createOutcome(resolvedResponse: ITSAStatusUpdateResponse): Outcome = {
    resolvedResponse match {
      case response: ITSAStatusUpdateResponseFailure =>
        Outcome(isSuccessful = false, failureCategory = Some(response.failures.head.code), failureReason = Some(response.failures.head.reason))
      case _: ITSAStatusUpdateResponseSuccess =>
        Outcome(isSuccessful = true, failureCategory = None, failureReason = None)
      case _ =>
        Outcome(isSuccessful = false, failureCategory = Some("Unknown failure reason"), failureReason = Some("Unknown failure category"))
    }
  }

  def generateOptOutAudit(optOutProposition: OptOutProposition,
                          intentTaxYear: TaxYear,
                          resolvedOutcome: ITSAStatusUpdateResponse
                         )(implicit user: MtdItUser[_]): OptOutAuditModel = {
    OptOutAuditModel(
      saUtr = user.saUtr,
      credId = user.credId,
      userType = user.userType,
      agentReferenceNumber = user.arn,
      mtditid = user.mtditid,
      nino = user.nino,
      optOutRequestedFromTaxYear = intentTaxYear.formatAsShortYearRange,
      currentYear = optOutProposition.currentTaxYear.taxYear.formatAsShortYearRange,
      `beforeITSAStatusCurrentYear-1` = optOutProposition.previousTaxYear.status,
      beforeITSAStatusCurrentYear = optOutProposition.currentTaxYear.status,
      `beforeITSAStatusCurrentYear+1` = optOutProposition.nextTaxYear.status,
      outcome = createOutcome(resolvedOutcome),
      `afterAssumedITSAStatusCurrentYear-1` = optOutProposition.previousTaxYear.expectedItsaStatusAfter(intentTaxYear),
      afterAssumedITSAStatusCurrentYear = optOutProposition.currentTaxYear.expectedItsaStatusAfter(intentTaxYear),
      `afterAssumedITSAStatusCurrentYear+1` = optOutProposition.nextTaxYear.expectedItsaStatusAfter(intentTaxYear),
      `currentYear-1Crystallised` = optOutProposition.previousTaxYear.crystallised
    )
  }

}