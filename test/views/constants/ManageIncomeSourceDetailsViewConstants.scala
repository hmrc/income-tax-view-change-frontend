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
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import models.incomeSourceDetails.{LatencyYearsCrystallised, LatencyYearsQuarterly, QuarterTypeCalendar, QuarterTypeStandard}
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
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = SelfEmployment,
      quarterReportingType = Some(QuarterTypeStandard)
    )

  val selfEmploymentViewModelOneYearCrystallised: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(true),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = SelfEmployment,
      quarterReportingType = Some(QuarterTypeStandard)
    )

  val selfEmploymentViewModelCYUnknown: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = None
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails4),
      incomeSourceType = SelfEmployment,
      quarterReportingType = Some(QuarterTypeStandard)
    )

  val selfEmploymentViewModelWithUnknowns: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = None,
      address = None,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = None,
        secondYear = None
      ),
      latencyDetails = None,
      incomeSourceType = SelfEmployment,
      quarterReportingType = None
    )

  val ukViewModel: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = Some(testStartDate),
      address = None,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = UkProperty,
      quarterReportingType = Some(QuarterTypeCalendar)
    )

  val ukViewModelOneYearQuarterly: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = Some(testStartDate),
      address = None,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails4),
      incomeSourceType = UkProperty,
      quarterReportingType = Some(QuarterTypeCalendar)
    )

  val ukPropertyViewModelOneYearCrystallised: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(true),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = UkProperty,
      quarterReportingType = Some(QuarterTypeStandard)
    )

  val ukPropertyViewModelCYUnknown: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = None
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetailsCYUnknown),
      incomeSourceType = UkProperty,
      quarterReportingType = Some(QuarterTypeStandard)
    )

  val ukViewModelUnknowns: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = None,
      address = None,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = None,
        secondYear = None
      ),
      latencyDetails = None,
      incomeSourceType = UkProperty,
      quarterReportingType = None
    )

  val foreignViewModel: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = Some(testStartDate),
      address = None,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = ForeignProperty,
      quarterReportingType = Some(QuarterTypeCalendar)
    )

  val foreignPropertyViewModelOneYearCrystallised: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = Some(true)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(true),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetails3),
      incomeSourceType = ForeignProperty,
      quarterReportingType = Some(QuarterTypeStandard)
    )

  val foreignPropertyLatencyYearTwoUnknown: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = Some(testTradeName),
      tradingName = Some(testTradeName),
      tradingStartDate = Some(testStartDate),
      address = expectedAddress,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(true),
        secondYear = None
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyDetails = Some(testLatencyDetailsCYUnknown),
      incomeSourceType = ForeignProperty,
      quarterReportingType = Some(QuarterTypeStandard)
    )

  val foreignViewModelUnknowns: ManageIncomeSourceDetailsViewModel =
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = None,
      address = None,
      isTraditionalAccountingMethod = Some(false),
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = Some(false),
        secondYear = Some(false)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = None,
        secondYear = None
      ),
      latencyDetails = None,
      incomeSourceType = ForeignProperty,
      quarterReportingType = None
    )


}
