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

package testConstants

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.optout.{NextUpdatesOptOutViewModel, OptOutOneYearViewModel}
import services.optout._

object OptOutTestConstants {

  def getNextUpdatesOptOutViewModel: NextUpdatesOptOutViewModel = {
    val currentYear = TaxYear.forYearEnd(2024)
    val proposition = buildOptOutProposition(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Mandated, pyCrystallised = true, currentYear)
    NextUpdatesOptOutViewModel(proposition, Some(OptOutOneYearViewModel(currentYear, Some(OneYearOptOutFollowedByMandated))))

  }

  def buildOptOutProposition(cyStatus: ITSAStatus, pyStatus: ITSAStatus, nyStatus: ITSAStatus, pyCrystallised: Boolean, currentYear: TaxYear = TaxYear.forYearEnd(2024)): OptOutProposition = {
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(pyStatus, previousYear, crystallised = pyCrystallised)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(cyStatus, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(nyStatus, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

}
