/*
 * Copyright 2023 HM Revenue & Customs
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

import models.itsaStatus.ITSAStatus.{Annual, Mandated}

sealed trait OptOutPropositionTypes {
  val proposition: OptOutProposition

  def state(): Option[OptOutState]
}

case class OneYearOptOutProposition private(proposition: OptOutProposition) extends OptOutPropositionTypes {
  val intent: OptOutTaxYear = proposition.availableOptOutYears.head

  override def state(): Option[OneYearOptOutState] = {
    proposition match {
      case OptOutProposition(previousTaxYear, currentTaxYear, _) if previousTaxYear == intent && currentTaxYear.status == Mandated => Some(OneYearOptOutFollowedByMandated)
      case OptOutProposition(_, currentTaxYear, nextTaxYear) if currentTaxYear == intent && nextTaxYear.status == Mandated => Some(OneYearOptOutFollowedByMandated)
      case OptOutProposition(previousTaxYear, currentTaxYear, _) if previousTaxYear == intent && currentTaxYear.status == Annual => Some(OneYearOptOutFollowedByAnnual)
      case OptOutProposition(_, currentTaxYear, nextTaxYear) if currentTaxYear == intent && nextTaxYear.status == Annual => Some(OneYearOptOutFollowedByAnnual)
      case OptOutProposition(_, _, nextTaxYear) if nextTaxYear == intent => Some(NextYearOptOut)
      case _ => None
    }
  }
}

case class MultiYearOptOutProposition private(proposition: OptOutProposition) extends OptOutPropositionTypes {
  override def state(): Option[OptOutState] = Some(MultiYearOptOutDefault)
}