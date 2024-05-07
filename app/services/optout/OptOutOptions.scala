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

import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, Voluntary}
import models.optOut.{OptOutMessageResponse, YearStatusDetail}

trait OptOutOptions {
  def getOptOutOptionsFor(finalisedStatus: Boolean,
                          previousYearState: YearStatusDetail,
                          currentYearState: YearStatusDetail,
                          nextYearState: YearStatusDetail): OptOutMessageResponse
}

trait OptOut {
  def canOptOut: Boolean
  val taxYearStatusDetail: YearStatusDetail
}
case class CurrentTaxYearOptOut(taxYearStatusDetail: YearStatusDetail) extends OptOut {
  def canOptOut: Boolean = taxYearStatusDetail.statusDetail.isVoluntary
}

case class NextTaxYearOptOut(taxYearStatusDetail: YearStatusDetail, currentTaxYear: YearStatusDetail) extends OptOut {
  def canOptOut: Boolean = taxYearStatusDetail.statusDetail.isVoluntary || (currentTaxYear.statusDetail.isVoluntary && taxYearStatusDetail.statusDetail.isUnknown)
}

case class PreviousTaxYearOptOut(taxYearStatusDetail: YearStatusDetail, crystallised: Boolean) extends OptOut {
  def canOptOut: Boolean = taxYearStatusDetail.statusDetail.isVoluntary && !crystallised
}

//todo-MISUV-7349: to be replaced, this is a tactical implementation only for one year optout scenario
class OptOutOptionsTacticalSolution extends OptOutOptions {

  def getOptOutOptionsFor(finalisedStatus: Boolean,
                          previousYearState: YearStatusDetail,
                          currentYearState: YearStatusDetail,
                          nextYearState: YearStatusDetail): OptOutMessageResponse = {

    val isPY_V = previousYearState.statusDetail.isVoluntary
    val isCY_V = currentYearState.statusDetail.isVoluntary
    val isNY_V = nextYearState.statusDetail.isVoluntary

    (finalisedStatus, isPY_V, isCY_V, isNY_V) match {
      case (false, true, false, false) => OptOutMessageResponse(taxYears = Array(previousYearState.taxYear))
      case (false, false, true, false) => OptOutMessageResponse(taxYears = Array(currentYearState.taxYear))
      case (false, false, false, true) => OptOutMessageResponse(taxYears = Array(nextYearState.taxYear))
      case _ => OptOutMessageResponse()
    }
  }

  def getOptOutOptionsForSingleYear(finalisedStatus: Boolean,
                                    previousYearState: YearStatusDetail,
                                    currentYearState: YearStatusDetail,
                                    nextYearState: YearStatusDetail): OptOutMessageResponse = {

    val voluntaryOptOutYearsAvailable: Seq[OptOut] = Seq[OptOut](
      PreviousTaxYearOptOut(previousYearState, finalisedStatus),
      CurrentTaxYearOptOut(currentYearState),
      NextTaxYearOptOut(nextYearState, currentYearState)).filter(
        _.canOptOut
      )


    if (voluntaryOptOutYearsAvailable.size == 1) {
      OptOutMessageResponse(voluntaryOptOutYearsAvailable.map(_.taxYearStatusDetail.taxYear).toArray)
    } else {
      OptOutMessageResponse()
    }
  }

  private def validOptOut(currentYearMinusOne: PreviousTaxYearOptOut,
                          currentYear: CurrentTaxYearOptOut,
                          currentYearPlusOne: NextTaxYearOptOut): Seq[OptOut] = {
      Seq[OptOut](
        currentYearMinusOne,
        currentYear,
        currentYearPlusOne
      ).filter(_.canOptOut)
  }
}

