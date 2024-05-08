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

import models.itsaStatus.ITSAStatus.{NoStatus, Voluntary}
import models.optOut.{OptOutOneYearViewModel, TaxYearITSAStatus}

trait OptOutOptions {
  def getOptOutOptionsForSingleYear(optOutData: OptOutData): Option[OptOutOneYearViewModel]
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

case class OptOutData(previousTaxYear: PreviousTaxYearOptOut,
                      currentTaxYear: CurrentTaxYearOptOut,
                      nextTaxYear: NextTaxYearOptOut) {

  private val optOutYears: Seq[OptOut] = Seq[OptOut](
    previousTaxYear,
    currentTaxYear,
    nextTaxYear)

  def availableOptOutYears: Seq[OptOut] = optOutYears.filter(_.canOptOut)

  lazy val countVoluntaryOptOutYears: Int = availableOptOutYears.size

}

class OptOutOptionsSingleYear extends OptOutOptions {
  def getOptOutOptionsForSingleYear(optOutData: OptOutData): Option[OptOutOneYearViewModel] = {

    if (optOutData.countVoluntaryOptOutYears == 1) {
      Some(OptOutOneYearViewModel(optOutData.availableOptOutYears.head.taxYearStatusDetail.taxYear))
    } else {
      None
    }
  }


}

