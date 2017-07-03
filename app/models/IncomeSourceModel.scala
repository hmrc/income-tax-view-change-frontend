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

import play.api.libs.json.{Json, OFormat}

case class IncomeSourceModel(
                              businessDetails: Option[BusinessIncomeModel],
                              propertyDetails: Option[PropertyIncomeModel]
                                   )

object IncomeSourceModel {
  val format: OFormat[IncomeSourceModel] = Json.format[IncomeSourceModel]

}


case class BusinessIncomeModel(selfEmploymentId: String, accountingPeriod: AccountingPeriodModel, tradingName: String)

object BusinessIncomeModel {
  implicit val format: OFormat[BusinessIncomeModel] = Json.format[BusinessIncomeModel]
}


case class PropertyIncomeModel(accountingPeriod: AccountingPeriodModel)

object PropertyIncomeModel {
  implicit val format: OFormat[PropertyIncomeModel] = Json.format[PropertyIncomeModel]
}


