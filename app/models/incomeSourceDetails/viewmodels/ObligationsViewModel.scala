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

import exceptions.MissingFieldException

import models.incomeSourceDetails.TaxYear

import java.time.LocalDate

final case class ObligationsViewModel(quarterlyObligationsDates: Seq[Seq[DatesModel]],
                                      finalDeclarationDates: Seq[DatesModel], currentTaxYear: Int, showPrevTaxYears: Boolean) {

  def getOverdueObligationsMessageComponents(currentDate: LocalDate, isBusinessHistoric: Boolean): OverdueObligationsMessageComponents = {
    val overdueAnnual = getNumberOfOverdueAnnualObligations(currentDate)
    val overdueQuarterly = getNumberOfOverdueQuarterlyObligations(currentDate)

    if (isBusinessHistoric) {
      val nonHistoricOverdueObligations = getNumberOfNonHistoricOverdueObligations(currentDate)
      if (nonHistoricOverdueObligations > 1) {
        OverdueObligationsMessageComponents("obligation.inset.multiple-historic-overdue.text",
          Seq(getNumberOfNonHistoricOverdueObligations(currentDate).toString, (currentTaxYear - 2).toString, (currentTaxYear - 1).toString))
      } else if (nonHistoricOverdueObligations == 1) {
        OverdueObligationsMessageComponents("obligation.inset.single-historic-overdue.text",
          Seq((currentTaxYear - 2).toString, (currentTaxYear - 1).toString))
      }else {
        OverdueObligationsMessageComponents("", Seq())
      }
    } else {
      handleNonHistoricCases(overdueAnnual, overdueQuarterly)
    }
  }

  def getNumberOfOverdueAnnualObligations(currentDate: LocalDate): Int = {
    finalDeclarationDates.count(_.inboundCorrespondenceDue.isBefore(currentDate))
  }

  def getNumberOfOverdueQuarterlyObligations(currentDate: LocalDate): Int = {
    quarterlyObligationsDates.flatMap(_.filter(_.inboundCorrespondenceDue.isBefore(currentDate))).size
  }

  private def getNumberOfNonHistoricOverdueObligations(currentDate: LocalDate): Int = {
    val minusOneTaxYear: TaxYear = TaxYear(currentTaxYear - 2, currentTaxYear - 1)
    val nonHistoricOverdueAnnualObligations: Int = getNumberOfOverdueAnnualObligations(currentDate) -
      finalDeclarationDates.count(_.inboundCorrespondenceFrom.isBefore(minusOneTaxYear.toFinancialYearStart))
    val nonHistoricOverdueQuarterlyObligations: Int = getNumberOfOverdueQuarterlyObligations(currentDate) -
      quarterlyObligationsDates.flatten.count(_.inboundCorrespondenceFrom.isBefore(minusOneTaxYear.toFinancialYearStart))

    nonHistoricOverdueAnnualObligations + nonHistoricOverdueQuarterlyObligations
  }

  private def handleNonHistoricCases(overdueAnnual: Int, overdueQuarterly: Int): OverdueObligationsMessageComponents = {
    def onlyAnnual(numberOfOverdueAnnualObligations: Int): OverdueObligationsMessageComponents = {
      if (numberOfOverdueAnnualObligations > 1) {
        OverdueObligationsMessageComponents("obligation.inset.multiple-annual-overdue.text", Seq(numberOfOverdueAnnualObligations.toString))
      } else {
        OverdueObligationsMessageComponents("obligation.inset.single-annual-overdue.text", Seq())
      }
    }

    def onlyQuarterly(numberOfOverdueQuarterlyObligations: Int): OverdueObligationsMessageComponents = {
      if (quarterlyObligationsDates.flatMap(_.filter(_.inboundCorrespondenceTo.isBefore(
        TaxYear.makeTaxYearWithEndYear(currentTaxYear).toFinancialYearStart
      ))).nonEmpty) {
        if (numberOfOverdueQuarterlyObligations > 1) {
          OverdueObligationsMessageComponents(
            "obligation.inset.multiple-tax-years-multiple-quarterly-overdue.text",
            Seq(numberOfOverdueQuarterlyObligations.toString)
          )
        } else {
          OverdueObligationsMessageComponents(
            "obligation.inset.multiple-tax-years-single-quarterly-overdue.text",
            Seq(numberOfOverdueQuarterlyObligations.toString)
          )
        }
      } else if (numberOfOverdueQuarterlyObligations > 1) {
        OverdueObligationsMessageComponents(
          "obligation.inset.multiple-quarterly-overdue.text",
          Seq(numberOfOverdueQuarterlyObligations.toString, (numberOfOverdueQuarterlyObligations * 3).toString, (currentTaxYear - 1).toString, currentTaxYear.toString)
        )
      } else {
        OverdueObligationsMessageComponents(
          "obligation.inset.single-quarterly-overdue.text",
          Seq((currentTaxYear - 1).toString, currentTaxYear.toString)
        )
      }
    }

    (overdueAnnual, overdueQuarterly) match {
      case (a, 0) if a > 0 =>
        onlyAnnual(a)
      case (0, q) if q > 0 =>
        onlyQuarterly(q)
      case (a, q) if a > 0 && q > 0 =>
        OverdueObligationsMessageComponents("hybrid", Seq())
      case _ =>
        OverdueObligationsMessageComponents("", Seq())
    }
  }

  def getFirstUpcomingQuarterlyDate(currentDate: LocalDate): Option[DatesModel] = {
    quarterlyObligationsDates.flatten
      .filter(_.inboundCorrespondenceFrom.isAfter(currentDate))
      .sortBy(_.inboundCorrespondenceFrom)
      .headOption
  }

  def getFinalDeclarationDate(currentDate: LocalDate): Option[DatesModel] = {
    finalDeclarationDates.find(_.inboundCorrespondenceDue.isAfter(currentDate))
  }
}

final case class DatesModel(inboundCorrespondenceFrom: LocalDate, inboundCorrespondenceTo: LocalDate,
                            inboundCorrespondenceDue: LocalDate, periodKey: String, isFinalDec: Boolean, obligationType: String)

final case class OverdueObligationsMessageComponents(messageKey: String, args: Seq[String])