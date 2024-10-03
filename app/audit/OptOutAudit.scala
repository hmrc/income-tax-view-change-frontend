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
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import services.optout.OptOutProposition
import _root_.models.incomeSourceDetails.TaxYear
import auth.MtdItUser
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class OptOutAudit()(auditingService: AuditingService) {

  def generateOptOutAudit(optOutProposition: OptOutProposition,
                          intentTaxYear: TaxYear,
                          resolvedOutcome: Object
                         )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val optOutModel = OptOutAuditModel(
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

    auditingService.extendedAudit(optOutModel)
  }

  private def createOutcome(resolvedResponse: Object): Outcome = {
    resolvedResponse match {
      case ITSAStatusUpdateResponseFailure => new Outcome {
        override val isSuccessful: Boolean = false
      }
      case ITSAStatusUpdateResponseSuccess => new Outcome {
        override val isSuccessful: Boolean = true
        override val failureReason: String = ""
        override val failureCategory: String = ""
      }
      case _ => new Outcome {
        override val isSuccessful: Boolean = false
        override val failureReason: String = "Unknown failure reason"
        override val failureCategory: String = "Unknown failure category"
      }
    }
  }


}
