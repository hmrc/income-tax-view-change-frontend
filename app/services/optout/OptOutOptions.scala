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

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{ITSAStatus, NoStatus, Voluntary}
import models.optOut.OptOutOneYearViewModel

trait OptOutOptions {
  def getOptOutOptionsForSingleYear(optOutData: OptOutData): Option[OptOutOneYearViewModel]
}

trait OptOut {
  def canOptOut: Boolean
  val taxYear: TaxYear
}
case class CurrentTaxYearOptOut(status: ITSAStatus, taxYear: TaxYear) extends OptOut {
  def canOptOut: Boolean = status == Voluntary
}

case class NextTaxYearOptOut(status: ITSAStatus, taxYear: TaxYear, currentTaxYear: CurrentTaxYearOptOut) extends OptOut {
  def canOptOut: Boolean = status == Voluntary ||
    (currentTaxYear.status == Voluntary && status == NoStatus)
}

case class PreviousTaxYearOptOut(status: ITSAStatus, taxYear: TaxYear, crystallised: Boolean) extends OptOut {
  def canOptOut: Boolean = status == Voluntary && !crystallised
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
      Some(OptOutOneYearViewModel(optOutData.availableOptOutYears.head.taxYear))
    } else {
      None
    }
  }


}

