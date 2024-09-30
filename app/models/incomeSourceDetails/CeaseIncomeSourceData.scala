/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class CeaseIncomeSourceData(
                                  incomeSourceId:           Option[String]    = None,
                                  endDate:                  Option[LocalDate] = None,
                                  ceaseIncomeSourceDeclare: Option[String]    = None,
                                  journeyIsComplete:        Option[Boolean]   = None
                                )

object CeaseIncomeSourceData {

  val incomeSourceIdField:          String = "incomeSourceId"
  val dateCeasedField:              String = "endDate"
  val ceaseIncomeSourceDeclare:     String = "ceaseIncomeSourceDeclare"
  val journeyIsCompleteField:       String = "journeyIsComplete"

  def getJSONKeyPath(name: String): String = s"ceaseIncomeSourceData.$name"

  implicit val format: OFormat[CeaseIncomeSourceData] = Json.format[CeaseIncomeSourceData]
}
