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

import models.incomeSourceDetails.{ChosenReportingMethod, TaxYear}

import java.time.LocalDate

final case class ObligationsViewModel(
                                       quarterlyObligationsDates: Seq[Seq[DatesModel]],
                                       finalDeclarationDates: Seq[DatesModel],
                                       currentTaxYear: Int,
                                       showPrevTaxYears: Boolean
                                     ) {

  def reportingMethod(latencyIndicator1: Option[String], latencyIndicator2: Option[String]): ChosenReportingMethod =
    (latencyIndicator1, latencyIndicator2) match {
      case (Some("Q"), Some("Q")) => ChosenReportingMethod.Quarterly
      case (Some("A"), Some("Q")) => ChosenReportingMethod.AnnualQuarterly
      case (Some("Q"), Some("A")) => ChosenReportingMethod.QuarterlyAnnual
      case (Some("A"), Some("A")) => ChosenReportingMethod.Annual
      case (None, None) => ChosenReportingMethod.DefaultAnnual
      case _ => ChosenReportingMethod.Unknown
    }


  def getOverdueObligationsMessageComponents(currentDate: LocalDate, isBusinessHistoric: Boolean): OverdueObligationsMessageComponents = {
    if (isBusinessHistoric) {
      val nonHistoricOverdueObligations = getNumberOfNonHistoricOverdueObligations(currentDate)
      if (nonHistoricOverdueObligations > 1) {
        OverdueObligationsMessageComponents("obligation.inset.multiple.historic.overdue.text",
          List(getNumberOfNonHistoricOverdueObligations(currentDate).toString, (currentTaxYear - 2).toString, (currentTaxYear - 1).toString))
      } else if (nonHistoricOverdueObligations == 1) {
        OverdueObligationsMessageComponents("obligation.inset.single.historic.overdue.text",
          List((currentTaxYear - 2).toString, (currentTaxYear - 1).toString))
      } else {
        OverdueObligationsMessageComponents("", Nil)
      }
    } else {
      val overdueAnnual = getNumberOfOverdueAnnualObligations(currentDate).min(1) // Will only be a maximum of 1 overdue annual if non historic
      val overdueQuarterly = getNumberOfOverdueQuarterlyObligations(currentDate)
      handleNonHistoricCases(overdueAnnual, overdueQuarterly)
    }
  }

  private def getNumberOfOverdueAnnualObligations(currentDate: LocalDate): Int = {
    finalDeclarationDates.count(_.inboundCorrespondenceDue.isBefore(currentDate))
  }

  private def getNumberOfOverdueQuarterlyObligations(currentDate: LocalDate): Int = {
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
    def onlyQuarterly(numberOfOverdueQuarterlyObligations: Int): OverdueObligationsMessageComponents = {
      if (quarterlyObligationsDates.flatMap(_.filter(_.inboundCorrespondenceTo.isBefore(
        TaxYear.makeTaxYearWithEndYear(currentTaxYear).toFinancialYearStart
      ))).nonEmpty) {
        if (numberOfOverdueQuarterlyObligations > 1) {
          OverdueObligationsMessageComponents(
            "obligation.inset.multiple.tax.years.multiple.quarterly.overdue.text",
            List(numberOfOverdueQuarterlyObligations.toString)
          )
        } else {
          OverdueObligationsMessageComponents(
            "obligation.inset.multiple.tax.years.single.quarterly.overdue.text",
            Nil
          )
        }
      } else if (numberOfOverdueQuarterlyObligations > 1) {
        OverdueObligationsMessageComponents(
          "obligation.inset.multiple.quarterly.overdue.text",
          List(numberOfOverdueQuarterlyObligations.toString, (numberOfOverdueQuarterlyObligations * 3).toString, (currentTaxYear - 1).toString, currentTaxYear.toString)
        )
      } else {
        OverdueObligationsMessageComponents(
          "obligation.inset.single.quarterly.overdue.text",
          List((currentTaxYear - 1).toString, currentTaxYear.toString)
        )
      }
    }

    (overdueAnnual, overdueQuarterly) match {
      case (1, 0) =>
        OverdueObligationsMessageComponents("obligation.inset.single.annual.overdue.text", Nil)
      case (0, q) if q > 0 =>
        onlyQuarterly(q)
      case (1, q) if q > 0 =>
        OverdueObligationsMessageComponents("hybrid", Nil)
      case _ =>
        OverdueObligationsMessageComponents("", Nil)
    }
  }

  def getFirstUpcomingQuarterlyDate(currentDate: LocalDate): Option[DatesModel] = {
    quarterlyObligationsDates.flatten
      .filter(_.inboundCorrespondenceDue.isAfter(currentDate))
      .sortBy(_.inboundCorrespondenceDue)
      .headOption
  }

  def getQuarterlyObligationTaxYear(quarterlyObligation: DatesModel): Int = {
    if (quarterlyObligation.inboundCorrespondenceFrom.isBefore(TaxYear.makeTaxYearWithEndYear(currentTaxYear).toFinancialYearStart)) {
      currentTaxYear - 1
    } else if (quarterlyObligation.inboundCorrespondenceFrom.isAfter(TaxYear.makeTaxYearWithEndYear(currentTaxYear).toFinancialYearEnd)) {
      currentTaxYear + 1
    } else {
      currentTaxYear
    }
  }

  def getFinalDeclarationDate(currentDate: LocalDate): Option[DatesModel] = {
    finalDeclarationDates.find(_.inboundCorrespondenceDue.isAfter(currentDate))
  }
}

final case class DatesModel(
                             inboundCorrespondenceFrom: LocalDate,
                             inboundCorrespondenceTo: LocalDate,
                             inboundCorrespondenceDue: LocalDate,
                             periodKey: String,
                             isFinalDec: Boolean,
                             obligationType: String
                           )

final case class OverdueObligationsMessageComponents(messageKey: String, args: List[String])