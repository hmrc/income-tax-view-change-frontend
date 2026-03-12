/*
 * Copyright 2026 HM Revenue & Customs
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

package models.newHomePage

import models.obligations.SingleObligationModel

import java.time.LocalDate

final case class SubmissionDeadlinesViewModel(
                                               openObligations: Seq[SingleObligationModel],
                                               currentDate: LocalDate,
                                               nextQuarterlyUpdateDueDate: Option[LocalDate],
                                               nextTaxReturnDueDate: Option[LocalDate]
                                             ) {

  private val obligationTypeAnnual: String = "Crystallisation"
  private val obligationTypeQuarterly: String = "Quarterly"

  private def getOldestOverdueDateByObligationType(obligationType: String): Option[LocalDate] =
    openObligations
      .filter(_.obligationType == obligationType)
      .map(_.due)
      .filter(_.isBefore(currentDate))
      .sortWith(_ isBefore _).headOption

  def getNumberOfOverdueAnnualObligations: Int =
    openObligations
      .filter(_.obligationType == obligationTypeAnnual)
      .count(_.due.isBefore(currentDate))

  def getNumberOfOverdueQuarterlyObligations: Int =
    openObligations
      .filter(_.obligationType == obligationTypeQuarterly)
      .count(_.due.isBefore(currentDate))

  def getOldestAnnualOverdueDate: Option[LocalDate] =
    getOldestOverdueDateByObligationType(obligationTypeAnnual)

  def getOldestQuarterlyOverdueDate: Option[LocalDate] =
    getOldestOverdueDateByObligationType(obligationTypeQuarterly)

  def isAnnualObligations: Boolean =
    openObligations.exists(_.obligationType == obligationTypeAnnual)

  def isQuarterlyObligations: Boolean =
    openObligations.exists(_.obligationType == obligationTypeQuarterly)

  def showNextUpdatesTileContent: Boolean = openObligations.nonEmpty
}
