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

package models.obligations

import auth.MtdItUser
import models.incomeSourceDetails.QuarterTypeElection.orderingByTypeName
import models.incomeSourceDetails.{PropertyDetailsModel, QuarterReportingType, QuarterTypeCalendar, QuarterTypeStandard}
import play.api.libs.json._

import java.time.LocalDate

sealed trait ObligationsResponseModel

case class ObligationsModel(obligations: Seq[GroupedObligationsModel]) extends ObligationsResponseModel {
  def allDeadlinesWithSource(previous: Boolean = false)(implicit mtdItUser: MtdItUser[_]): Seq[ObligationWithIncomeType] = {
    val deadlines = obligations.flatMap { groupedObligationsModel =>
      mtdItUser.incomeSources.properties.find(_.incomeSourceId == groupedObligationsModel.identification) match {
        case Some(property) if property.incomeSourceType.contains("foreign-property") =>
          groupedObligationsModel.obligations.map {
            deadline => Some(ObligationWithIncomeType(s"nextUpdates.propertyIncome.Foreign", deadline))
          }
        case Some(property) if property.incomeSourceType.contains("uk-property") =>
          groupedObligationsModel.obligations.map {
            deadline => Some(ObligationWithIncomeType(s"nextUpdates.propertyIncome.UK", deadline))
          }
        case Some(_: PropertyDetailsModel) =>
          groupedObligationsModel.obligations.map {
            deadline => Some(ObligationWithIncomeType(s"nextUpdates.propertyIncome", deadline))
          }
        case _ =>
          if (mtdItUser.incomeSources.businesses.exists(_.incomeSourceId == groupedObligationsModel.identification)) groupedObligationsModel.obligations.map {
            deadline =>
              Some(ObligationWithIncomeType(mtdItUser.incomeSources.businesses.find(_.incomeSourceId == groupedObligationsModel.identification)
                .get.tradingName.getOrElse("nextUpdates.business"), deadline))
          } else if (groupedObligationsModel.obligations.forall(ob => ob.obligationType == "Crystallisation"))
            groupedObligationsModel.obligations.map {
              deadline => Some(ObligationWithIncomeType("nextUpdates.crystallisedAll", deadline))
            } else None
      }
    }.flatten

    val nonEOPSDeadlines = deadlines.filter(_.obligation.obligationType != "EOPS")


    if (previous) nonEOPSDeadlines.sortBy(_.obligation.dateReceived.map(_.toEpochDay)).reverse else deadlines.sortBy(_.obligation.due.toEpochDay)
  }

  def allQuarterly(implicit mtdItUser: MtdItUser[_]): Seq[ObligationWithIncomeType] =
    allDeadlinesWithSource()(mtdItUser).filter(_.obligation.obligationType == "Quarterly")

  def allCrystallised(implicit mtdItUser: MtdItUser[_]): Seq[ObligationWithIncomeType] =
    allDeadlinesWithSource()(mtdItUser).filter(_.obligation.obligationType == "Crystallisation")

  def obligationsByDate(implicit mtdItUser: MtdItUser[_]): Seq[(LocalDate, Seq[ObligationWithIncomeType])] =
    allDeadlinesWithSource().groupBy(_.obligation.due).toList.sortWith((x, y) => x._1.isBefore(y._1))

  def quarterlyUpdatesCounts(implicit mtdItUser: MtdItUser[_]): Int =
    allDeadlinesWithSource()(mtdItUser)
      .filter(_.obligation.obligationType == "Quarterly")
      .count(_.obligation.status == StatusFulfilled)

  def getPeriodForQuarterly(obligation: ObligationWithIncomeType): QuarterReportingType = {
    val dayOfMonth = obligation.obligation.start.getDayOfMonth
    if (dayOfMonth < 6) QuarterTypeCalendar else QuarterTypeStandard
  }

  def isFinalDeclarationOrTaxReturnSubmitted: Boolean = {
    obligations.exists(
      _.obligations.exists(
        _.status == StatusFulfilled
      )
    )
  }

  def groupByQuarterPeriod(obligations: Seq[ObligationWithIncomeType]): Map[Option[QuarterReportingType], Seq[ObligationWithIncomeType]] = {
    obligations.groupBy { obligation =>
        obligation.obligation.obligationType match {
          case "Quarterly" => Some(getPeriodForQuarterly(obligation))
          case _ => None //"Default"
        }
      }.view
      .mapValues(_.sortBy(_.obligation.start))
      .toSeq
      .sortBy { case (period, _) => period } // Sort by period
      .map { case (period, obligations) =>
        // Sort obligations within each period by start date
        val sortedObligations = obligations.sortBy(_.obligation.start)
        (period, sortedObligations)
      }
      .toMap
  }

}

object ObligationsModel {
  implicit val format: OFormat[ObligationsModel] = Json.format[ObligationsModel]
}

case class ObligationWithIncomeType(incomeType: String, obligation: SingleObligationModel)

case class ObligationsErrorModel(code: Int, message: String) extends ObligationsResponseModel

object ObligationsErrorModel {
  implicit val format: Format[ObligationsErrorModel] = Json.format[ObligationsErrorModel]
}
