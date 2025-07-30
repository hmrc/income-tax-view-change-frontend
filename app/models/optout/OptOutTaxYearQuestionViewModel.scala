/*
 * Copyright 2025 HM Revenue & Customs
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

package models.optout

import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated}
import services.optout._

case class OptOutTaxYearQuestionViewModel(taxYear: OptOutTaxYear,
                                          optOutState: Option[OptOutState],
                                          numberOfQuarterlyUpdates: Int,
                                          currentYearStatus: ITSAStatus) {

  private val hasNoQuarterlyUpdatesSubmitted: Boolean = numberOfQuarterlyUpdates == OneYearOptOutCheckpointViewModel.noQuarterlyUpdates

  private val taxYearMessageSuffix = taxYear match {
    case _: PreviousOptOutTaxYear => "previousYear"
    case _: CurrentOptOutTaxYear  => "currentYear"
    case _: NextOptOutTaxYear     => "nextYear"
  }

  private val optOutStateMessageSuffix = (optOutState, hasNoQuarterlyUpdatesSubmitted, currentYearStatus) match {
    case (Some(MultiYearOptOutDefault), _, _)              => "multiYear"
    case (Some(OneYearOptOutFollowedByMandated), true, _)  => "singleYearFollowedByMandated"
    case (Some(OneYearOptOutFollowedByMandated), false, _) => "singleYearFollowedByMandatedWithUpdates"
    case (Some(OneYearOptOutFollowedByAnnual), true, _)    => "singleYearFollowedByAnnual"
    case (Some(OneYearOptOutFollowedByAnnual), false, _)   => "singleYearFollowedByAnnualWithUpdates"
    case (Some(NextYearOptOut), _, Annual)                 => "nextYearOptOutAnnual"
    case (Some(NextYearOptOut), _, Mandated)               => "nextYearOptOutMandated"
    case (None, _, _)                                      => "noState"
  }

  def showInset: Boolean = {
    (optOutState, isCurrentYear) match {
      case (Some(MultiYearOptOutDefault), true) => true
      case (Some(OneYearOptOutFollowedByMandated), _) => true
      case _ => false
    }
  }

  def showQuarterlyUpdatesInset: Boolean = {
    (optOutState, hasNoQuarterlyUpdatesSubmitted) match {
      case (Some(OneYearOptOutFollowedByAnnual), false) => true
      case _ => false
    }
  }

  def showSecondParagraph: Boolean = {
    optOutState match {
      case Some(MultiYearOptOutDefault) => true
      case Some(OneYearOptOutFollowedByAnnual) => true
      case Some(NextYearOptOut) if currentYearStatus == ITSAStatus.Mandated => true
      case _ => false
    }
  }

  def showThirdParagraph: Boolean = optOutState.contains(MultiYearOptOutDefault)

  private def isCurrentYear: Boolean = taxYear match {
    case _: CurrentOptOutTaxYear => true
    case _                       => false
  }

  val messageSuffix = s"$taxYearMessageSuffix.$optOutStateMessageSuffix"
}
