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

package models.optout.newJourney

import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Dormant, ITSAStatus, Mandated, Voluntary}
import models.optout.OneYearOptOutCheckpointViewModel
import services.optout._

case class OptOutTaxYearQuestionViewModel(taxYear: OptOutTaxYear,
                                          optOutState: Option[OptOutState],
                                          numberOfQuarterlyUpdates: Int,
                                          currentYearStatus: ITSAStatus,
                                          nextYearStatus: ITSAStatus) {

  private val hasNoQuarterlyUpdatesSubmitted: Boolean = numberOfQuarterlyUpdates == OneYearOptOutCheckpointViewModel.noQuarterlyUpdates

  private val taxYearMessageSuffix = taxYear match {
    case _: PreviousOptOutTaxYear => "previousYear"
    case _: CurrentOptOutTaxYear  => "currentYear"
    case _: NextOptOutTaxYear     => "nextYear"
  }

  private val effectiveOptOutState: Option[OptOutState] = optOutState match {
    case Some(MultiYearOptOutDefault) if isPreviousYear && (currentYearStatus == Mandated | currentYearStatus == ITSAStatus.Dormant) => Some(OneYearOptOutFollowedByMandated)
    case Some(MultiYearOptOutDefault) if isCurrentYear && (nextYearStatus == Mandated | nextYearStatus == ITSAStatus.Dormant) => Some(OneYearOptOutFollowedByMandated)
    case _ => optOutState
  }

  private val optOutStateMessageSuffix = (effectiveOptOutState, hasNoQuarterlyUpdatesSubmitted, currentYearStatus, nextYearStatus) match {
    case (Some(MultiYearOptOutDefault), _, Voluntary, Annual) if isCurrentYear        => "multiYearCYFollowedByAnnual"
    case (Some(MultiYearOptOutDefault), _, _, _)                                      => "multiYear"
    case (Some(OneYearOptOutFollowedByMandated), _, _, _) if isPreviousYear           => "singleYearFollowedByMandated"
    case (Some(OneYearOptOutFollowedByMandated), true, _, _)                          => "singleYearFollowedByMandated"
    case (Some(OneYearOptOutFollowedByMandated), false, _, _)                         => "singleYearFollowedByMandatedWithUpdates"
    case (Some(OneYearOptOutFollowedByAnnual), _, _, _) if isPreviousYear             => "singleYearFollowedByAnnual"
    case (Some(OneYearOptOutFollowedByAnnual), true, _, _)                            => "singleYearFollowedByAnnual"
    case (Some(OneYearOptOutFollowedByAnnual), false, _, _)                           => "singleYearFollowedByAnnualWithUpdates"
    case (Some(NextYearOptOut), _, Mandated, _)                                       => "nextYearOptOutMandated"
    case (Some(NextYearOptOut), _, _, _)                                              => "nextYearOptOutDefault"
    case _                                                                            => "noState"
  }

  private def isCurrentYear: Boolean = taxYear match {
    case _: CurrentOptOutTaxYear => true
    case _                       => false
  }

  private def isPreviousYear: Boolean = taxYear match {
    case _: PreviousOptOutTaxYear => true
    case _                        => false
  }

  def showInset: Boolean = {
    (effectiveOptOutState, isCurrentYear, isPreviousYear, nextYearStatus) match {
      case (Some(MultiYearOptOutDefault), true, _, Annual)      => false
      case (Some(MultiYearOptOutDefault), true, _, _)           => true
      case (Some(MultiYearOptOutDefault), _, true, _)           => true
      case (Some(OneYearOptOutFollowedByMandated), _, false, _) => true
      case _ => false
    }
  }

  def showQuarterlyUpdatesInset: Boolean = {
    (effectiveOptOutState, hasNoQuarterlyUpdatesSubmitted, isPreviousYear) match {
      case (Some(OneYearOptOutFollowedByAnnual), _, true)      => true
      case (Some(OneYearOptOutFollowedByMandated), _, true)    => true
      case (Some(OneYearOptOutFollowedByAnnual), false, false) => true
      case _                                                   => false
    }
  }

  def showSecondParagraph: Boolean = {
    effectiveOptOutState match {
      case Some(MultiYearOptOutDefault)                                     => true
      case Some(OneYearOptOutFollowedByAnnual)                              => true
      case Some(NextYearOptOut) if currentYearStatus == ITSAStatus.Mandated => true
      case Some(OneYearOptOutFollowedByMandated) if isPreviousYear          => true
      case _                                                                => false
    }
  }

  def showThirdParagraph: Boolean = effectiveOptOutState.contains(MultiYearOptOutDefault)

  val messageSuffix = s"$taxYearMessageSuffix.$optOutStateMessageSuffix"

  val redirectToConfirmUpdatesPage: Boolean = optOutState.contains(OneYearOptOutFollowedByMandated) && !hasNoQuarterlyUpdatesSubmitted
}
