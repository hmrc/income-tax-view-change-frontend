/*
 * Copyright 2020 HM Revenue & Customs
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

import models.incomeSourceDetails.{BusinessDetailsModel, PropertyDetailsModel}
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import play.api.libs.json.{Format, Json}

abstract class IncomeModelWithDeadlines {
  def reportDeadlines: ReportDeadlinesModel
}

sealed trait IncomeSourcesWithDeadlinesResponse
case object IncomeSourcesWithDeadlinesError extends IncomeSourcesWithDeadlinesResponse
case class IncomeSourcesWithDeadlinesModel(
                                            businessIncomeSources: List[BusinessIncomeWithDeadlinesModel],
                                            propertyIncomeSource: Option[PropertyIncomeWithDeadlinesModel],
                                            crystallisedDeadlinesModel: Option[CrystallisedDeadlinesModel]) extends IncomeSourcesWithDeadlinesResponse

case class CrystallisedDeadlinesModel(reportDeadlines: ReportDeadlinesModel)

case class PropertyIncomeWithDeadlinesModel(
                                             incomeSource: PropertyDetailsModel,
                                             reportDeadlines: ReportDeadlinesModel) extends IncomeModelWithDeadlines
case class BusinessIncomeWithDeadlinesModel(
                                             incomeSource: BusinessDetailsModel,
                                             reportDeadlines: ReportDeadlinesModel) extends IncomeModelWithDeadlines

object BusinessIncomeWithDeadlinesModel {
  implicit val format: Format[BusinessIncomeWithDeadlinesModel] = Json.format[BusinessIncomeWithDeadlinesModel]
}

object PropertyIncomeWithDeadlinesModel {
  implicit val format: Format[PropertyIncomeWithDeadlinesModel] = Json.format[PropertyIncomeWithDeadlinesModel]
}

object IncomeSourcesWithDeadlinesModel {
 implicit val format: Format[IncomeSourcesWithDeadlinesModel] = Json.format[IncomeSourcesWithDeadlinesModel]
}

object CrystallisedDeadlinesModel{
  implicit val format: Format[CrystallisedDeadlinesModel] = Json.format[CrystallisedDeadlinesModel]
}



