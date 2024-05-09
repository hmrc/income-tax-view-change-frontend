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

package services.optout

import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, NoStatus, Voluntary}
import models.optOut.{OptOutOneYearViewModel, TaxYearITSAStatus}

trait OptOutOptions {
  def getOptOutOptionsForSingleYear(finalisedStatus: Boolean,
                          previousYearState: TaxYearITSAStatus,
                          currentYearState: TaxYearITSAStatus,
                          nextYearState: TaxYearITSAStatus): Option[OptOutOneYearViewModel]
}

trait OptOut {
  def canOptOut: Boolean
  val taxYearStatusDetail: TaxYearITSAStatus
}
case class CurrentTaxYearOptOut(taxYearStatusDetail: TaxYearITSAStatus) extends OptOut {
  def canOptOut: Boolean = taxYearStatusDetail.status == Voluntary
}

case class NextTaxYearOptOut(taxYearStatusDetail: TaxYearITSAStatus, currentTaxYear: TaxYearITSAStatus) extends OptOut {
  def canOptOut: Boolean = taxYearStatusDetail.status == Voluntary ||
    (currentTaxYear.status == Voluntary && taxYearStatusDetail.status == NoStatus)
}

case class PreviousTaxYearOptOut(taxYearStatusDetail: TaxYearITSAStatus, crystallised: Boolean) extends OptOut {
  def canOptOut: Boolean = taxYearStatusDetail.status == Voluntary && !crystallised
}

class OptOutOptionsSingleYear extends OptOutOptions {
  def getOptOutOptionsForSingleYear(finalisedStatus: Boolean,
                                    previousYearState: TaxYearITSAStatus,
                                    currentYearState: TaxYearITSAStatus,
                                    nextYearState: TaxYearITSAStatus): Option[OptOutOneYearViewModel] = {

    val voluntaryOptOutYearsAvailable: Seq[OptOut] = Seq[OptOut](
      PreviousTaxYearOptOut(previousYearState, finalisedStatus),
      CurrentTaxYearOptOut(currentYearState),
      NextTaxYearOptOut(nextYearState, currentYearState)).filter(
      _.canOptOut
    )

    if (voluntaryOptOutYearsAvailable.size == 1) {
      Some(OptOutOneYearViewModel(voluntaryOptOutYearsAvailable.head.taxYearStatusDetail.taxYear))
    } else {
      None
    }
  }
}
