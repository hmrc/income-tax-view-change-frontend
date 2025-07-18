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
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, Voluntary}
import play.api.Logger
import play.api.libs.json._

import java.time.LocalDate

sealed trait ObligationsResponseModel

case class ObligationsModel(obligations: Seq[GroupedObligationsModel]) extends ObligationsResponseModel {
  def allDeadlinesWithSource(
                              previous: Boolean = false,
                              r17ContentEnabled: Boolean = false,
                              currentYearITSAStatus: Option[ITSAStatus] = None
                            )(implicit mtdItUser: MtdItUser[_]): Seq[ObligationWithIncomeType] = {

    val isQuarterlyUser = currentYearITSAStatus.contains(ITSAStatus.Mandated) || currentYearITSAStatus.contains(ITSAStatus.Voluntary)

    val deadlines: Seq[ObligationWithIncomeType] =
      if (r17ContentEnabled) {
        if (isQuarterlyUser) {
          val businessIds = mtdItUser.incomeSources.businesses.map(_.incomeSourceId)

          obligations.flatMap { grouped =>
            if (businessIds.contains(grouped.identification)) {
              val businessName = mtdItUser.incomeSources.businesses
                .find(_.incomeSourceId == grouped.identification)
                .flatMap(_.tradingName)
                .getOrElse("nextUpdates.business")

              grouped.obligations
                .filter(_.obligationType == "Quarterly")
                .map(ob => ObligationWithIncomeType(businessName, ob))
            } else Seq.empty
          }
        } else {
          Seq.empty
        }
      } else {
        obligations.flatMap { grouped =>
          mtdItUser.incomeSources.properties.find(_.incomeSourceId == grouped.identification) match {
            case Some(property) if property.incomeSourceType.contains("foreign-property") =>
              grouped.obligations.map(ob => Some(ObligationWithIncomeType("nextUpdates.propertyIncome.Foreign", ob)))

            case Some(property) if property.incomeSourceType.contains("uk-property") =>
              grouped.obligations.map(ob => Some(ObligationWithIncomeType("nextUpdates.propertyIncome.UK", ob)))

            case Some(_: PropertyDetailsModel) =>
              grouped.obligations.map(ob => Some(ObligationWithIncomeType("nextUpdates.propertyIncome", ob)))

            case _ =>
              if (mtdItUser.incomeSources.businesses.exists(_.incomeSourceId == grouped.identification)) {
                val businessName = mtdItUser.incomeSources.businesses
                  .find(_.incomeSourceId == grouped.identification)
                  .flatMap(_.tradingName)
                  .getOrElse("nextUpdates.business")

                grouped.obligations.map(ob => Some(ObligationWithIncomeType(businessName, ob)))
              } else if (grouped.obligations.forall(_.obligationType == "Crystallisation")) {
                grouped.obligations.map(ob => Some(ObligationWithIncomeType("nextUpdates.crystallisedAll", ob)))
              } else Seq.empty
          }
        }.flatten
      }

    val nonEOPSDeadlines = deadlines.filter(_.obligation.obligationType != "EOPS")


    if (previous) nonEOPSDeadlines.sortBy(_.obligation.dateReceived.map(_.toEpochDay)).reverse else deadlines.sortBy(_.obligation.due.toEpochDay)
  }

  def allQuarterly(implicit mtdItUser: MtdItUser[_]): Seq[ObligationWithIncomeType] =
    allDeadlinesWithSource()(mtdItUser).filter(_.obligation.obligationType == "Quarterly")

  def allCrystallised(implicit mtdItUser: MtdItUser[_]): Seq[ObligationWithIncomeType] =
    allDeadlinesWithSource()(mtdItUser).filter(_.obligation.obligationType == "Crystallisation")

  def obligationsByDate(isR17ContentEnabled: Boolean,
                        currentYearITSAStatus: Option[ITSAStatus])(implicit mtdItUser: MtdItUser[_]): Seq[(LocalDate, Seq[ObligationWithIncomeType])] =
    allDeadlinesWithSource(r17ContentEnabled = isR17ContentEnabled, currentYearITSAStatus = currentYearITSAStatus)
      .groupBy(_.obligation.due).toList.sortWith((x, y) => x._1.isBefore(y._1))

  def quarterlyUpdatesCounts(implicit mtdItUser: MtdItUser[_]): Int =
    allDeadlinesWithSource()(mtdItUser)
      .filter(_.obligation.obligationType == "Quarterly")
      .count(_.obligation.status == StatusFulfilled)

  def getPeriodForQuarterly(obligation: ObligationWithIncomeType): QuarterReportingType = {
    val dayOfMonth = obligation.obligation.start.getDayOfMonth
    if (dayOfMonth < 6) QuarterTypeCalendar else QuarterTypeStandard
  }

  def isFinalDeclarationOrTaxReturnSubmitted: Boolean =
    obligations.exists(
      _.obligations.exists(
        _.status == StatusFulfilled
      )
    )

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
