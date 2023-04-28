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

package models.incomeSourceDetails

import java.time.LocalDate
import models.core.{AccountingPeriodModel, AddressModel, CessationModel, ContactDetailsModel}
import play.api.libs.json.{Format, Json}

case class BusinessDetailsModel(incomeSourceId: Option[String],
                                accountingPeriod: Option[AccountingPeriodModel],
                                tradingName: Option[String],
                                firstAccountingPeriodEndDate: Option[LocalDate],
                                tradingStartDate: Option[LocalDate],
                                cessation: Option[CessationModel])

object BusinessDetailsModel {
  implicit val format: Format[BusinessDetailsModel] = Json.format[BusinessDetailsModel]
}
