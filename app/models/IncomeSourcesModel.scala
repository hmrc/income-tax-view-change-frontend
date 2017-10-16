/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}
import utils.ImplicitDateFormatter._

sealed trait IncomeSourcesResponseModel
case object IncomeSourcesError extends IncomeSourcesResponseModel
case class IncomeSourcesModel(incomeSources: List[IncomeModel]) extends IncomeSourcesResponseModel {

  val hasPropertyIncome: Boolean = incomeSources.exists {
    case _: PropertyIncomeModel => true
  }
  val hasBusinessIncome: Boolean = incomeSources.exists {
    case _: BusinessIncomeModel => true
  }
  val hasBothIncomeSources: Boolean = hasPropertyIncome && hasBusinessIncome

  val orderedTaxYears: List[Int] = incomeSources.map(_.accountingPeriod.determineTaxYear).sortWith(_ < _).distinct

  val earliestTaxYear: Option[Int] = orderedTaxYears.headOption
  val lastTaxYear: Option[Int] = orderedTaxYears.lastOption

  def earliestAccountingPeriodStart(year: Int): LocalDate = incomeSources.map(_.accountingPeriod.start).min
}

object IncomeSourcesModel {
  val format: OFormat[IncomeSourcesModel] = Json.format[IncomeSourcesModel]

}

case class BusinessIncomeModel(
                                selfEmploymentId: String,
                                tradingName: String,
                                cessationDate: LocalDate,
                                accountingPeriod: AccountingPeriodModel,
                                obligations: ObligationsModel) extends IncomeModel

object BusinessIncomeModel {
  implicit val format: OFormat[BusinessIncomeModel] = Json.format[BusinessIncomeModel]
}

case class PropertyIncomeModel(accountingPeriod: AccountingPeriodModel, obligations: ObligationsModel) extends IncomeModel

object PropertyIncomeModel {
  implicit val format: OFormat[PropertyIncomeModel] = Json.format[PropertyIncomeModel]
}

abstract class IncomeModel {
  def accountingPeriod: AccountingPeriodModel
  def obligations: ObligationsModel
}


