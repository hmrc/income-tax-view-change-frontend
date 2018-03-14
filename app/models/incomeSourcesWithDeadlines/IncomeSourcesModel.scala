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
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import play.api.libs.json.{Format, Json}
import utils.ImplicitDateFormatter._

abstract class IncomeModel {
  def accountingPeriod: AccountingPeriodModel
  def reportDeadlines: ReportDeadlinesResponseModel
}

sealed trait IncomeSourcesResponseModel
case object IncomeSourcesError extends IncomeSourcesResponseModel
case class IncomeSourcesModel(
                               businessIncomeSources: List[BusinessIncomeModel],
                               propertyIncomeSource: Option[PropertyIncomeModel]) extends IncomeSourcesResponseModel {

  val incomeSources: List[IncomeModel] = businessIncomeSources ++ propertyIncomeSource

  val hasPropertyIncome: Boolean = propertyIncomeSource.nonEmpty
  val hasBusinessIncome: Boolean = businessIncomeSources.nonEmpty
  val hasBothIncomeSources: Boolean = hasPropertyIncome && hasBusinessIncome

  val orderedTaxYears: List[Int] = incomeSources.map(_.accountingPeriod.determineTaxYear).sortWith(_ < _).distinct
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
    incomeSources.filter(_.accountingPeriod.determineTaxYear == year).map(_.accountingPeriod.start).min

  val sortedBusinesses: List[(BusinessIncomeModel, Int)] = businessIncomeSources.sortBy(_.selfEmploymentId.substring(4)).zipWithIndex
}

case class PropertyIncomeModel(accountingPeriod: AccountingPeriodModel, reportDeadlines: ReportDeadlinesResponseModel) extends IncomeModel
case class BusinessIncomeModel(
                                selfEmploymentId: String,
                                tradingName: String,
                                cessationDate: Option[LocalDate],
                                accountingPeriod: AccountingPeriodModel,
                                reportDeadlines: ReportDeadlinesResponseModel) extends IncomeModel

object BusinessIncomeModel {
  implicit val format: Format[BusinessIncomeModel] = Json.format[BusinessIncomeModel]
}

object PropertyIncomeModel {
  implicit val format: Format[PropertyIncomeModel] = Json.format[PropertyIncomeModel]
}

object IncomeSourcesModel {
 implicit val format: Format[IncomeSourcesModel] = Json.format[IncomeSourcesModel]
}



