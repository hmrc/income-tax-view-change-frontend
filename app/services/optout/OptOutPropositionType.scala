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

import models.itsaStatus.ITSAStatus.{Annual, Dormant, Mandated, Voluntary}

sealed trait OptOutPropositionTypes {
  val proposition: OptOutProposition

  def state(intent: OptOutTaxYear): Option[OptOutState]
}

case class OneYearOptOutProposition (proposition: OptOutProposition) extends OptOutPropositionTypes {
  val intent: OptOutTaxYear = proposition.availableOptOutYears.head

  def state(): Option[OneYearOptOutState] = state(intent)

  override def state(intent: OptOutTaxYear): Option[OneYearOptOutState] = {
    val OptOutProposition(previousTaxYear, currentTaxYear, nextTaxYear) = proposition

    (intent, currentTaxYear.status, nextTaxYear.status) match {
      case (selectedYear, Mandated | Dormant, _) if selectedYear == previousTaxYear => Some(OneYearOptOutFollowedByMandated)
      case (selectedYear, Annual, _) if selectedYear == previousTaxYear => Some(OneYearOptOutFollowedByAnnual)

      case (selectedYear, _, Mandated | Dormant) if selectedYear == currentTaxYear => Some(OneYearOptOutFollowedByMandated)
      case (selectedYear, _, Annual) if selectedYear == currentTaxYear => Some(OneYearOptOutFollowedByAnnual)

      case (selectedYear, _, _) if selectedYear == nextTaxYear => Some(NextYearOptOut)

      case _ => None
    }
  }
}

case class MultiYearOptOutProposition (proposition: OptOutProposition) extends OptOutPropositionTypes {
  override def state(intent: OptOutTaxYear): Option[OptOutState] = {
    val OptOutProposition(previousTaxYear, currentTaxYear, nextTaxYear) = proposition

    (intent, currentTaxYear.status, nextTaxYear.status) match {
      case (selectedYear, Mandated | Dormant, _) if selectedYear == previousTaxYear => Some(OneYearOptOutFollowedByMandated)
      case (selectedYear, Annual, _) if selectedYear == previousTaxYear => Some(OneYearOptOutFollowedByAnnual)

      case (selectedYear, _, Mandated | Dormant) if selectedYear == currentTaxYear => Some(OneYearOptOutFollowedByMandated)
      case (selectedYear, _, Annual) if selectedYear == currentTaxYear => Some(OneYearOptOutFollowedByAnnual)

      case (selectedYear, currentYearStatus, _) if selectedYear == nextTaxYear && currentYearStatus != Voluntary => Some(NextYearOptOut)

      case _ => Some(MultiYearOptOutDefault)
    }
  }
}