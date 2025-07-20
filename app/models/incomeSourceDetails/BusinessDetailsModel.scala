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

import models.core.{AccountingPeriodModel, AddressModel, CessationModel}
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class BusinessDetailsModel(incomeSourceId: String,
                                incomeSource: Option[String],
                                accountingPeriod: Option[AccountingPeriodModel],
                                tradingName: Option[String],
                                firstAccountingPeriodEndDate: Option[LocalDate],
                                tradingStartDate: Option[LocalDate],
                                contextualTaxYear: Option[String],
                                cessation: Option[CessationModel],
                                cashOrAccruals: Option[Boolean],
                                address: Option[AddressModel] = None,
                                latencyDetails: Option[LatencyDetails] = None,
                                quarterTypeElection: Option[QuarterTypeElection] = None) {

  def isCeased: Boolean = cessation.exists(_.date.nonEmpty)

  def isOngoingSoleTraderBusiness(id: String): Boolean = !isCeased && id == incomeSourceId

  def getTradingStartDateForCessation: LocalDate = (tradingStartDate, contextualTaxYear) match {
    case (Some(startDate), _) => startDate
    case (None, Some(taxYear)) => TaxYear(taxYear.toInt - 1, taxYear.toInt).toFinancialYearStart
    case (None, None) => LocalDate.MIN
  }
}

object BusinessDetailsModel {
  implicit val format: Format[BusinessDetailsModel] = Json.format[BusinessDetailsModel]
}
