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

package models.optout

import models.incomeSourceDetails.TaxYear
import models.optout.OneYearOptOutCheckpointViewModel.noQuarterlyUpdates
import services.optout.{NextYearOptOut, OneYearOptOutFollowedByAnnual, OptOutState}

sealed trait OptOutCheckpointViewModel {
  val startYear: String;
  val endYear: String;
}

object OneYearOptOutCheckpointViewModel {
  val noQuarterlyUpdates = 0
}

case class OneYearOptOutCheckpointViewModel(intent: TaxYear, state: Option[OptOutState],
                                            quarterlyUpdates: Option[Int] = Some(noQuarterlyUpdates)) extends OptOutCheckpointViewModel {
  val startYear: String = intent.startYear.toString
  val endYear: String = intent.endYear.toString
  val showFutureChangeInfo: Boolean = state.contains(OneYearOptOutFollowedByAnnual) || state.contains(NextYearOptOut)
  val whereQuarterlyUpdatesAreMade: Option[Int] = quarterlyUpdates.filter(_ > noQuarterlyUpdates)
}

case class MultiYearOptOutCheckpointViewModel(intent: TaxYear) extends OptOutCheckpointViewModel {
  val startYear: String = intent.startYear.toString
  val endYear: String = intent.endYear.toString
}