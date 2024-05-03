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

case class CurrentTaxYearOptOut(currentTaxYear: YearStatusDetail) {
  def canOptOut: Boolean = currentTaxYear.statusDetail.isVoluntary
}

case class NextTaxYearOptOut(nextTaxYear: YearStatusDetail, currentTaxYear: YearStatusDetail) {
  def canOptOut: Boolean = nextTaxYear.statusDetail.isVoluntary || (currentTaxYear.statusDetail.isVoluntary && nextTaxYear.statusDetail.isUnknown)
}

case class PreviousTaxYearOptOut(previousTaxYear: YearStatusDetail, crystallised: Boolean) {
  def canOptOut: Boolean = previousTaxYear.statusDetail.isVoluntary && !crystallised
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
      case (false, true, false, false) => OptOutMessageResponse(oneYearOptOut = true, taxYears = Array(previousYearState.taxYear))
      case (false, false, true, false) => OptOutMessageResponse(oneYearOptOut = true, taxYears = Array(currentYearState.taxYear))
      case (false, false, false, true) => OptOutMessageResponse(oneYearOptOut = true, taxYears = Array(nextYearState.taxYear))
      case _ => OptOutMessageResponse()
    }
  }

  def getOptOutOptionsForSingleYear(finalisedStatus: Boolean,
                                    previousYearState: YearStatusDetail,
                                    currentYearState: YearStatusDetail,
                                    nextYearState: YearStatusDetail): OptOutMessageResponse = {


    val previousYear = PreviousTaxYearOptOut(previousYearState, finalisedStatus)
    val currentYear = CurrentTaxYearOptOut(currentYearState)
    val nextYear = NextTaxYearOptOut(nextYearState, currentYearState)

    val voluntaryOptOutYearsAvailable: Seq[String] = validOptOut(previousYear, currentYear, nextYear)

    if (voluntaryOptOutYearsAvailable.size == 1) {
      voluntaryOptOutYearsAvailable match {
        case Seq("CY-1") => OptOutMessageResponse(oneYearOptOut = true, taxYears = Array(previousYearState.taxYear))
        case Seq("CY") => OptOutMessageResponse(oneYearOptOut = true, taxYears = Array(currentYearState.taxYear))
        case Seq("CY+1") => OptOutMessageResponse(oneYearOptOut = true, taxYears = Array(nextYearState.taxYear))
        case _ => OptOutMessageResponse()
      }
    } else {
      OptOutMessageResponse()
    }
  }

  private def validOptOut(currentYearMinusOne: PreviousTaxYearOptOut,
                          currentYear: CurrentTaxYearOptOut,
                          currentYearPlusOne: NextTaxYearOptOut): Seq[String] = {
    if (!currentYearMinusOne.canOptOut && !currentYear.canOptOut && !currentYearPlusOne.canOptOut)
      Seq("No Opt out")
    else {
      val outcomes = Seq(
        if (currentYearMinusOne.canOptOut) Some("CY-1") else None,
        if (currentYear.canOptOut) Some("CY") else None,
        if (currentYearPlusOne.canOptOut) Some("CY+1") else None
      ).flatten

      outcomes
    }
  }
}

