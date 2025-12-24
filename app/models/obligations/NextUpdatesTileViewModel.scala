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

package models.obligations

import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus

import java.time.LocalDate

case class NextUpdatesTileViewModel(dueDates: Seq[LocalDate],
                                    currentDate: LocalDate,
                                    isReportingFrequencyEnabled: Boolean,
                                    showOptInOptOutContentUpdateR17: Boolean,
                                    currentYearITSAStatus: ITSAStatus,
                                    nextQuarterlyUpdateDueDate: Option[LocalDate],
                                    nextTaxReturnDueDate: Option[LocalDate]) {

  def getNextDeadline: Option[LocalDate] = {
    dueDates.sortWith(_ isBefore _).headOption
  }

  def getNumberOfOverdueObligations: Int = {
    dueDates.count(_.isBefore(currentDate))
  }

  def showNextUpdatesTileContent: Boolean = dueDates.nonEmpty

  def isQuarterlyUser: Boolean =
    currentYearITSAStatus == ITSAStatus.Voluntary || currentYearITSAStatus == ITSAStatus.Mandated

  def isAnnualUser: Boolean =
    currentYearITSAStatus == ITSAStatus.Annual
}
