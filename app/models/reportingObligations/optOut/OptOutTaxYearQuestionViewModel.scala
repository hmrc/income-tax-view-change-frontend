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

package models.reportingObligations.optOut

import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, Voluntary}
import services.reportingObligations.optOut.*

case class OptOutTaxYearQuestionViewModel(taxYear: OptOutTaxYear,
                                          optOutState: Option[OptOutState],
                                          numberOfQuarterlyUpdates: Int,
                                          currentYearStatus: ITSAStatus,
                                          nextYearStatus: ITSAStatus) {

  private val hasNoQuarterlyUpdatesSubmitted: Boolean = numberOfQuarterlyUpdates == 0

  private val taxYearMessageSuffix = taxYear match {
    case _: PreviousOptOutTaxYear => "previousYear"
    case _: CurrentOptOutTaxYear  => "currentYear"
    case _: NextOptOutTaxYear     => "nextYear"
  }

  private val optOutStateMessageSuffix = (optOutState, hasNoQuarterlyUpdatesSubmitted, currentYearStatus, nextYearStatus) match {
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

  def showFollowedByMandatedInset: Boolean =
    optOutState.contains(OneYearOptOutFollowedByMandated)

  def showQuarterlyUpdatesInset: Boolean = optOutState match {
    case Some(OneYearOptOutFollowedByMandated) => false
    case Some(_) if numberOfQuarterlyUpdates > 0 => true
    case _ => false
  }

  def showSecondParagraph: Boolean = optOutState match {
    case Some(OneYearOptOutFollowedByMandated) => false
    case Some(_)                               => true
    case _                                     => false
  }

  val messageSuffix = s"$taxYearMessageSuffix.$optOutStateMessageSuffix"

  def mandatedInsetMessageSuffix: String =
    (taxYearMessageSuffix, hasNoQuarterlyUpdatesSubmitted) match {
      case (tySuffix, true)  => s"$tySuffix.singleYearFollowedByMandated"
      case (tySuffix, false) => s"$tySuffix.singleYearFollowedByMandatedWithUpdates"
    }

  val redirectToConfirmUpdatesPage: Boolean =
    optOutState.contains(OneYearOptOutFollowedByMandated)
}
