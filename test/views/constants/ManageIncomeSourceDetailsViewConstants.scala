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

package views.constants

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.BusinessDetailsTestConstants._
import views.messages.ManageIncomeSourceDetailsViewMessages.expectedAddress

object ManageIncomeSourceDetailsViewConstants {

  val selfEmploymentViewModel: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = SelfEmployment,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val selfEmploymentViewModelOneYearCrystallised: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(true),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = SelfEmployment,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val selfEmploymentViewModelCYUnknown: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = None
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = None
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails4),
      incomeSourceType = SelfEmployment,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val selfEmploymentViewModelWithUnknowns: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = None,
      address = None,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = None,
        secondYear = None
      ),
      latencyDetails = None,
      incomeSourceType = SelfEmployment,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val ukViewModel: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = Some(testStartDate),
      address = None,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = UkProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val ukViewModelOneYearQuarterly: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = Some(testStartDate),
      address = None,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails4),
      incomeSourceType = UkProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val ukPropertyViewModelOneYearCrystallised: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(true),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = UkProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val ukPropertyViewModelCYUnknown: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = None
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = None
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetailsCYUnknown),
      incomeSourceType = UkProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val ukViewModelUnknowns: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = None,
      address = None,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = None,
        secondYear = None
      ),
      latencyDetails = None,
      incomeSourceType = UkProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val foreignViewModel: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = Some(testStartDate),
      address = None,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = ForeignProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val foreignPropertyViewModelOneYearCrystallised: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(true),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = ForeignProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val foreignPropertyLatencyYearTwoUnknown: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = None
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(false),
        secondYear = None
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetailsCYUnknown),
      incomeSourceType = ForeignProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )

  val foreignViewModelUnknowns: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = None,
      address = None,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsAnnual = LatencyYearsAnnual(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = None,
        secondYear = None
      ),
      latencyDetails = None,
      incomeSourceType = ForeignProperty,
      currentTaxYearEnd = getCurrentTaxYearEnd.getYear
    )
}
