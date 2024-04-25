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

package models.nextUpdates

import models.incomeSourceDetails.TaxYear

import java.time.LocalDate

case class NextUpdatesViewModel(allDeadlines: Seq[DeadlineViewModel], optOutMessage: OptOutMessage)

case class OptOutStatus()

case class CanOptOutOfPreviousYear() extends OptOutStatus

case class CanOptOutOfCurrentYear() extends OptOutStatus

case class CanOptOutOfNextYear() extends OptOutStatus



case class OptOutMessage(showOptOut: Boolean = true, yearFrom: Int = 2023, yearTo: Int = 2024)

case class DeadlineViewModel(obligationType: ObligationType,
                             standardAndCalendar: Boolean,
                             deadline: LocalDate,
                             standardQuarters: Seq[NextUpdateModelWithIncomeType],
                             calendarQuarters: Seq[NextUpdateModelWithIncomeType]) {}
