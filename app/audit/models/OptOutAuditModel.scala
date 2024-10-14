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

import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus
import play.api.libs.json.{JsObject, JsValue, Json, OFormat}
import services.optout.OptOutProposition
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

case class Outcome(
                    isSuccessful: Boolean,
                    failureCategory: Option[String],
                    failureReason: Option[String]
                  )


object Outcome {
  implicit val format: OFormat[Outcome] = Json.format[Outcome]
}

case class OptOutAuditModel(
                             mtdItUser: MtdItUser[_],
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

  private val userType =
    mtdItUser.userType match {
      case Some(Agent) => "Agent"
      case Some(_) => "Individual"
      case None => ""
    }

  private val optOutDetailsJson: JsObject =
    Json.obj(
      "saUtr" -> mtdItUser.saUtr,
      "credId" -> mtdItUser.credId,
      "mtditid" -> mtdItUser.mtditid,
      "userType" -> userType,
      "agentReferenceNumber" -> mtdItUser.arn,
      "nino" -> nino,
      "outcome" -> outcome,
      "optOutRequestedFromTaxYear" -> optOutRequestedFromTaxYear,
      "currentYear" -> currentYear,
      "beforeITSAStatusCurrentYearMinusOne" -> beforeITSAStatusCurrentYearMinusOne.toString,
      "beforeITSAStatusCurrentYear" -> beforeITSAStatusCurrentYear.toString,
      "beforeITSAStatusCurrentYearPlusOne" -> beforeITSAStatusCurrentYearPlusOne.toString,
      "afterAssumedITSAStatusCurrentYearMinusOne" -> afterAssumedITSAStatusCurrentYearMinusOne.toString,
      "afterAssumedITSAStatusCurrentYear" -> afterAssumedITSAStatusCurrentYear.toString,
      "afterAssumedITSAStatusCurrentYearPlusOne" -> afterAssumedITSAStatusCurrentYearMinusOne.toString,
      "currentYearMinusOneCrystallised" -> currentYearMinusOneCrystallised
    )

  override val detail: JsValue = optOutDetailsJson

}


object OptOutAuditModel {

  def generateOptOutAudit(optOutProposition: OptOutProposition,
                          intentTaxYear: TaxYear,
                          resolvedOutcome: ITSAStatusUpdateResponse
                         )(implicit user: MtdItUser[_]): OptOutAuditModel = {
    OptOutAuditModel(
      mtdItUser = user,
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
      case _: ITSAStatusUpdateResponseFailure => Outcome(isSuccessful = false, failureCategory = Some("API_FAILURE"), failureReason = Some("Failure reasons"))
      case _: ITSAStatusUpdateResponseSuccess => Outcome(isSuccessful = true, failureCategory = None, failureReason = None)
      case _ => Outcome(isSuccessful = false, failureCategory = Some("Unknown failure reason"), failureReason = Some("Unknown failure category"))
    }
  }


}