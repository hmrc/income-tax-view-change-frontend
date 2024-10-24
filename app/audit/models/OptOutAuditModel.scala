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
                             beforeITSAStatusCurrentYearMinusOne: ITSAStatus,
                             beforeITSAStatusCurrentYear: ITSAStatus,
                             beforeITSAStatusCurrentYearPlusOne: ITSAStatus,
                             afterAssumedITSAStatusCurrentYearMinusOne: ITSAStatus,
                             afterAssumedITSAStatusCurrentYear: ITSAStatus,
                             afterAssumedITSAStatusCurrentYearPlusOne: ITSAStatus,
                             currentYearMinusOneCrystallised: Boolean
                           ) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.OptOutQuarterlyReportingRequest

  override val auditType: String = enums.AuditType.OptOutQuarterlyReportingRequest

//  private val optOutDetailsJson: JsObject =
//    Json.obj(
//      "nino" -> nino,
//      "outcome" -> outcome,
//      "optOutRequestedFromTaxYear" -> optOutRequestedFromTaxYear,
//      "currentYear" -> currentYear,
//      "beforeITSAStatusCurrentYearMinusOne" -> beforeITSAStatusCurrentYearMinusOne.toString,
//      "beforeITSAStatusCurrentYear" -> beforeITSAStatusCurrentYear.toString,
//      "beforeITSAStatusCurrentYearPlusOne" -> beforeITSAStatusCurrentYearPlusOne.toString,
//      "afterAssumedITSAStatusCurrentYearMinusOne" -> afterAssumedITSAStatusCurrentYearMinusOne.toString,
//      "afterAssumedITSAStatusCurrentYear" -> afterAssumedITSAStatusCurrentYear.toString,
//      "afterAssumedITSAStatusCurrentYearPlusOne" -> afterAssumedITSAStatusCurrentYearMinusOne.toString,
//      "currentYearMinusOneCrystallised" -> currentYearMinusOneCrystallised
//    )

  override val detail: JsValue = Json.toJson(this)

}


object OptOutAuditModel {

  implicit val format: OFormat[OptOutAuditModel] = Json.format[OptOutAuditModel]

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
      optOutRequestedFromTaxYear = intentTaxYear.formatTaxYearRange,
      currentYear = optOutProposition.currentTaxYear.taxYear.formatTaxYearRange,
      beforeITSAStatusCurrentYearMinusOne = optOutProposition.previousTaxYear.status,
      beforeITSAStatusCurrentYear = optOutProposition.currentTaxYear.status,
      beforeITSAStatusCurrentYearPlusOne = optOutProposition.nextTaxYear.status,
      outcome = createOutcome(resolvedOutcome),
      afterAssumedITSAStatusCurrentYearMinusOne = optOutProposition.previousTaxYear.expectedItsaStatusAfter(intentTaxYear),
      afterAssumedITSAStatusCurrentYear = optOutProposition.currentTaxYear.expectedItsaStatusAfter(intentTaxYear),
      afterAssumedITSAStatusCurrentYearPlusOne = optOutProposition.nextTaxYear.expectedItsaStatusAfter(intentTaxYear),
      currentYearMinusOneCrystallised = optOutProposition.previousTaxYear.crystallised
    )
  }

  private def createOutcome(resolvedResponse: ITSAStatusUpdateResponse): Outcome = {
    resolvedResponse match {
      case response: ITSAStatusUpdateResponseFailure =>
        Outcome(isSuccessful = false, failureCategory = Some(response.failures.head.code), failureReason = Some(response.failures.head.reason))
      case _: ITSAStatusUpdateResponseSuccess =>
        Outcome(isSuccessful = true, failureCategory = None, failureReason = None)
      case _ =>
        Outcome(isSuccessful = false, failureCategory = Some("Unknown failure reason"), failureReason = Some("Unknown failure category"))
    }
  }


}