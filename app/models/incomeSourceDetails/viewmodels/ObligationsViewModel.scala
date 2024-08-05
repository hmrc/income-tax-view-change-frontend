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

package models.incomeSourceDetails.viewmodels

import java.time.LocalDate

final case class ObligationsViewModel(quarterlyObligationsDates: Seq[Seq[DatesModel]],
                                      finalDeclarationDates: Seq[DatesModel], currentTaxYear: Int, showPrevTaxYears: Boolean) {

  def getOverdueObligationsMessageComponents(currentDate: LocalDate, isBusinessHistoric: Boolean): OverdueObligationsMessageComponents = {
    val overdueAnnual = getNumberOfOverdueAnnualObligations(currentDate)
    val overdueQuarterly = getNumberOfOverdueQuarterlyObligations(currentDate)

    if (isBusinessHistoric) {
      OverdueObligationsMessageComponents("obligation.inset.multiple-hybrid-overdue.text",
        Seq((overdueAnnual + overdueQuarterly).toString, (currentTaxYear - 2).toString, (currentTaxYear - 1).toString))
    } else {
      (overdueAnnual, overdueQuarterly) match {
        case (1, 0) =>
          OverdueObligationsMessageComponents("obligation.inset.single-annual-overdue.text", Seq())
        case (a, 0) if a > 1 =>
          OverdueObligationsMessageComponents("obligation.inset.multiple-annual-overdue.text", Seq(a.toString))
        case (0, 1) =>
          OverdueObligationsMessageComponents(
            "obligation.inset.single-quarterly-overdue.text",
            Seq((currentTaxYear - 1).toString, currentTaxYear.toString)
          )
        case (0, q) if q > 1 =>
          OverdueObligationsMessageComponents(
            "obligation.inset.multiple-quarterly-overdue.text",
            Seq(q.toString, (q * 3).toString, (currentTaxYear - 1).toString, currentTaxYear.toString)
          )
        case _ => OverdueObligationsMessageComponents("", Seq())
      }
    }
  }

  def getNumberOfOverdueAnnualObligations(currentDate: LocalDate): Int = {
    finalDeclarationDates.count(_.inboundCorrespondenceDue.isBefore(currentDate))
  }

  def getNumberOfOverdueQuarterlyObligations(currentDate: LocalDate): Int = {
    quarterlyObligationsDates.flatMap(_.filter(_.inboundCorrespondenceDue.isBefore(currentDate))).size
  }
}

final case class DatesModel(inboundCorrespondenceFrom: LocalDate, inboundCorrespondenceTo: LocalDate,
                            inboundCorrespondenceDue: LocalDate, periodKey: String, isFinalDec: Boolean, obligationType: String)

final case class OverdueObligationsMessageComponents(messageKey: String, args: Seq[String])