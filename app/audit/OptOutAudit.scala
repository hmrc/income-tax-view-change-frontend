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

package audit

import audit.models.{OptOutAuditModel, Outcome}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import services.optout.OptOutProposition
import _root_.models.incomeSourceDetails.TaxYear
import auth.MtdItUser
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

object OptOutAudit {

  def generateOptOutAudit(optOutProposition: OptOutProposition,
                          intentTaxYear: TaxYear,
                          resolvedOutcome: ITSAStatusUpdateResponse
                         )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): OptOutAuditModel = {
    OptOutAuditModel(
      nino = user.nino,
      optOutRequestedFromTaxYear = intentTaxYear.formatTaxYearRange,
      currentYear = optOutProposition.currentTaxYear.taxYear.toString,

      beforeITSAStatusCurrentYearMinusOne = optOutProposition.previousTaxYear.status.toString,
      beforeITSAStatusCurrentYear = optOutProposition.currentTaxYear.status.toString,
      beforeITSAStatusCurrentYearPlusOne = optOutProposition.nextTaxYear.status.toString,

      outcome = createOutcome(resolvedOutcome),

      afterAssumedITSAStatusCurrentYearMinusOne = optOutProposition.previousTaxYear.expectedItsaStatusAfter(intentTaxYear).toString,
      afterAssumedITSAStatusCurrentYear = optOutProposition.currentTaxYear.expectedItsaStatusAfter(intentTaxYear).toString,
      afterAssumedITSAStatusCurrentYearPlusOne = optOutProposition.nextTaxYear.expectedItsaStatusAfter(intentTaxYear).toString,

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
