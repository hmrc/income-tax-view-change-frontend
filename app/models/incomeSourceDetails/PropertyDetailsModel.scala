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

import models.core.{AccountingPeriodModel, CessationModel}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class PropertyDetailsModel(
                                 incomeSourceId: String,
                                 accountingPeriod: Option[AccountingPeriodModel],
                                 firstAccountingPeriodEndDate: Option[LocalDate],
                                 incomeSourceType: Option[String],
                                 tradingStartDate: Option[LocalDate],
                                 contextualTaxYear: Option[String],
                                 cessation: Option[CessationModel],
                                 latencyDetails: Option[LatencyDetails] = None,
                                 quarterTypeElection: Option[QuarterTypeElection] = None
                               ) {

  def isUkProperty: Boolean = incomeSourceType.contains("uk-property")

  def isForeignProperty: Boolean = incomeSourceType.contains("foreign-property")

  def isCeased: Boolean = cessation.exists(_.date.nonEmpty)

  def isOngoingUkProperty: Boolean = isUkProperty && !isCeased

  def isOngoingForeignProperty: Boolean = isForeignProperty && !isCeased

  def getTradingStartDateForCessation: LocalDate = (tradingStartDate, contextualTaxYear) match {
    case (Some(startDate), _) => startDate
    case (None, Some(taxYear)) => TaxYear(taxYear.toInt - 1, taxYear.toInt).toFinancialYearStart
    case (None, None) => LocalDate.MIN
  }
}

object PropertyDetailsModel {
  implicit val format: OFormat[PropertyDetailsModel] = Json.format[PropertyDetailsModel]
}
