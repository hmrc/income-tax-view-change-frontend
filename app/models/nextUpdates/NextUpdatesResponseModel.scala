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

package models.nextUpdates

import auth.MtdItUser
import models.incomeSourceDetails.QuarterTypeElection.orderingByTypeName
import models.incomeSourceDetails.{PropertyDetailsModel, QuarterReportingType, QuarterTypeCalendar, QuarterTypeStandard}
import play.api.libs.json._

import java.time.LocalDate


sealed trait NextUpdatesResponseModel

case class ObligationsModel(obligations: Seq[NextUpdatesModel]) extends NextUpdatesResponseModel {
  def allDeadlinesWithSource(previous: Boolean = false)(implicit mtdItUser: MtdItUser[_]): Seq[NextUpdateModelWithIncomeType] = {
    val deadlines = obligations.flatMap { deadlinesModel =>
      mtdItUser.incomeSources.properties.find(_.incomeSourceId == deadlinesModel.identification) match {
        case Some(property) if property.incomeSourceType.contains("foreign-property") =>
          deadlinesModel.obligations.map {
            deadline => Some(NextUpdateModelWithIncomeType(s"nextUpdates.propertyIncome.Foreign", deadline))
          }
        case Some(property) if property.incomeSourceType.contains("uk-property") => //
          deadlinesModel.obligations.map {
            deadline => Some(NextUpdateModelWithIncomeType(s"nextUpdates.propertyIncome.UK", deadline))
          }
        case Some(_: PropertyDetailsModel) =>
          deadlinesModel.obligations.map {
            deadline => Some(NextUpdateModelWithIncomeType(s"nextUpdates.propertyIncome", deadline))
          }
        case _ =>
          if (mtdItUser.incomeSources.businesses.exists(_.incomeSourceId == deadlinesModel.identification)) deadlinesModel.obligations.map {
            deadline =>
              Some(NextUpdateModelWithIncomeType(mtdItUser.incomeSources.businesses.find(_.incomeSourceId == deadlinesModel.identification)
                .get.tradingName.getOrElse("nextUpdates.business"), deadline))
          } else if (deadlinesModel.obligations.forall(ob => ob.obligationType == "Crystallised"))
            deadlinesModel.obligations.map {
            deadline => Some(NextUpdateModelWithIncomeType("nextUpdates.crystallisedAll", deadline))
          } else None
      }
    }.flatten

    if (previous) deadlines.sortBy(_.obligation.dateReceived.map(_.toEpochDay)).reverse else deadlines.sortBy(_.obligation.due.toEpochDay)
  }

  def allQuarterly(implicit mtdItUser: MtdItUser[_]): Seq[NextUpdateModelWithIncomeType] =
    allDeadlinesWithSource()(mtdItUser).filter(_.obligation.obligationType == "Quarterly")

  def allEops(implicit mtdItUser: MtdItUser[_]): Seq[NextUpdateModelWithIncomeType] =
    allDeadlinesWithSource()(mtdItUser).filter(_.obligation.obligationType == "EOPS")

  def allCrystallised(implicit mtdItUser: MtdItUser[_]): Seq[NextUpdateModelWithIncomeType] =
    allDeadlinesWithSource()(mtdItUser).filter(_.obligation.obligationType == "Crystallised")

  def obligationsByDate(implicit mtdItUser: MtdItUser[_]): Seq[(LocalDate, Seq[NextUpdateModelWithIncomeType])] =
    allDeadlinesWithSource().groupBy(_.obligation.due).toList.sortWith((x, y) => x._1.isBefore(y._1))

  def getPeriodForQuarterly(obligation: NextUpdateModelWithIncomeType): QuarterReportingType = {
    val dayOfMonth = obligation.obligation.start.getDayOfMonth
    if (dayOfMonth < 6) QuarterTypeCalendar else QuarterTypeStandard
  }

  def groupByQuarterPeriod(obligations: Seq[NextUpdateModelWithIncomeType]): Map[Option[QuarterReportingType], Seq[NextUpdateModelWithIncomeType]] = {
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

case class NextUpdatesModel(identification: String, obligations: List[NextUpdateModel]) {
  val currentQuarterlyDeadlines: List[NextUpdateModel] = obligations.filter(_.obligationType == "Quarterly").sortBy(_.start.toEpochDay)
  val currentEOPsDeadlines: List[NextUpdateModel] = obligations.filter(_.obligationType == "EOPS").sortBy(_.start.toEpochDay)
  val currentCrystDeadlines: List[NextUpdateModel] = obligations.filter(_.obligationType == "Crystallised").sortBy(_.start.toEpochDay)
}

case class NextUpdateModel(start: LocalDate,
                           end: LocalDate,
                           due: LocalDate,
                           obligationType: String,
                           dateReceived: Option[LocalDate],
                           periodKey: String) extends NextUpdatesResponseModel

case class NextUpdateModelWithIncomeType(incomeType: String, obligation: NextUpdateModel)

case class NextUpdatesErrorModel(code: Int, message: String) extends NextUpdatesResponseModel

object NextUpdateModel {
  implicit val format: Format[NextUpdateModel] = Json.format[NextUpdateModel]
}

object NextUpdatesModel {
  implicit val format: Format[NextUpdatesModel] = Json.format[NextUpdatesModel]
}

object NextUpdatesErrorModel {
  implicit val format: Format[NextUpdatesErrorModel] = Json.format[NextUpdatesErrorModel]
}
