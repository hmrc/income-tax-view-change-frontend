/*
 * Copyright 2018 HM Revenue & Customs
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

package models.incomeSourcesWithDeadlines

import java.time.LocalDate

import models._
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, PropertyDetailsModel}
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import play.api.libs.json.{Format, Json}
import utils.ImplicitDateFormatter._

abstract class IncomeModelWithDeadlines {
  def reportDeadlines: ReportDeadlinesResponseModel
}

sealed trait IncomeSourcesWithDeadlinesResponse
case object IncomeSourcesWithDeadlinesError extends IncomeSourcesWithDeadlinesResponse
case class IncomeSourcesWithDeadlinesModel(
                                            businessIncomeSources: List[BusinessIncomeWithDeadlinesModel],
                                            propertyIncomeSource: Option[PropertyIncomeWithDeadlinesModel]) extends IncomeSourcesWithDeadlinesResponse {

  val incomeSources: List[IncomeModelWithDeadlines] = businessIncomeSources ++ propertyIncomeSource

  val accountingPeriods: List[AccountingPeriodModel] =
    businessIncomeSources.map(_.incomeSource.accountingPeriod) ++ propertyIncomeSource.map(_.incomeSource.accountingPeriod)

  val hasPropertyIncome: Boolean = propertyIncomeSource.nonEmpty
  val hasBusinessIncome: Boolean = businessIncomeSources.nonEmpty
  val hasBothIncomeSources: Boolean = hasPropertyIncome && hasBusinessIncome

  val orderedTaxYears: List[Int] = accountingPeriods.map(_.determineTaxYear).sortWith(_ < _).distinct
  val earliestTaxYear: Option[Int] = orderedTaxYears.headOption
  val lastTaxYear: Option[Int] = orderedTaxYears.lastOption

  val allReportDeadlinesErrored: Boolean = !incomeSources.map(_.reportDeadlines).exists {
    case _: ReportDeadlinesModel => true
    case _ => false
  }

  val allReportDeadlinesErroredForAllIncomeSources: Boolean = hasBothIncomeSources && allReportDeadlinesErrored

  val hasBusinessReportErrors: Boolean = businessIncomeSources.map(_.reportDeadlines).exists {
    case _: ReportDeadlinesErrorModel => true
    case _ => false
  }

  val hasPropertyReportErrors: Boolean = propertyIncomeSource.map(_.reportDeadlines).exists {
    case _: ReportDeadlinesErrorModel => true
    case _ => false
  }

  def earliestAccountingPeriodStart(year: Int): LocalDate =
    accountingPeriods.filter(_.determineTaxYear == year).map(_.start).min

  val sortedBusinesses: List[(BusinessIncomeWithDeadlinesModel, Int)] =
    businessIncomeSources.sortBy(_.incomeSource.incomeSourceId.substring(4)).zipWithIndex

  def findBusinessById(id: Int): Option[BusinessDetailsModel] = sortedBusinesses.find(_._2 == id).map(_._1.incomeSource)
}

case class PropertyIncomeWithDeadlinesModel(
                                             incomeSource: PropertyDetailsModel,
                                             reportDeadlines: ReportDeadlinesResponseModel) extends IncomeModelWithDeadlines
case class BusinessIncomeWithDeadlinesModel(
                                             incomeSource: BusinessDetailsModel,
                                             reportDeadlines: ReportDeadlinesResponseModel) extends IncomeModelWithDeadlines

object BusinessIncomeWithDeadlinesModel {
  implicit val format: Format[BusinessIncomeWithDeadlinesModel] = Json.format[BusinessIncomeWithDeadlinesModel]
}

object PropertyIncomeWithDeadlinesModel {
  implicit val format: Format[PropertyIncomeWithDeadlinesModel] = Json.format[PropertyIncomeWithDeadlinesModel]
}

object IncomeSourcesWithDeadlinesModel {
 implicit val format: Format[IncomeSourcesWithDeadlinesModel] = Json.format[IncomeSourcesWithDeadlinesModel]
}



