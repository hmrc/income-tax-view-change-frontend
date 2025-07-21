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

import services.optout.{CurrentOptOutTaxYear, MultiYearOptOutDefault, NextOptOutTaxYear, NextYearOptOut, OneYearOptOutFollowedByAnnual, OneYearOptOutFollowedByMandated, OptOutState, OptOutTaxYear, PreviousOptOutTaxYear}

case class OptOutTaxYearQuestionViewModel(taxYear: OptOutTaxYear, optOutState: Option[OptOutState]) {
  private val taxYearMessageSuffix = taxYear match {
    case _: PreviousOptOutTaxYear => "previousYear"
    case _: CurrentOptOutTaxYear  => "currentYear"
    case _: NextOptOutTaxYear     => "nextYear"
  }

  private val optOutStateMessageSuffix = optOutState match {
    case Some(MultiYearOptOutDefault)          => "multiYear"
    case Some(OneYearOptOutFollowedByMandated) => "singleYearFollowedByMandated"
    case Some(OneYearOptOutFollowedByAnnual)   => "singleYearFollowedByAnnual"
    case Some(NextYearOptOut)                  => "nextYearOptOut"
    case None                                  => "noState"
  }

  def isCurrentYear: Boolean = taxYear match {
    case _: CurrentOptOutTaxYear => true
    case _                       => false
  }

  val messageSuffix = s"$taxYearMessageSuffix.$optOutStateMessageSuffix"
}
