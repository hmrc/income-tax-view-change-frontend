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

package testConstants

import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.PropertyDetailsModel
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseIntegrationTestConstants.{getCurrentTaxYearEnd, testLatencyDetails3, testPropertyIncomeId}
import testConstants.BusinessDetailsIntegrationTestConstants.endYear

import java.time.LocalDate

object PropertyDetailsIntegrationTestConstants {

  val startYear = getCurrentTaxYearEnd.getYear - 5
  val propertyAccountingStart = startYear.toString + "-01-01"
  val propertyAccountingStartLocalDate = LocalDate.of(startYear, 1, 1)
  val propertyAccountingEnd = startYear.toString + "-12-31"
  val propertyAccounringEndLocalDate = LocalDate.of(startYear, 12, 31)
  val propertyIncomeType = Some("property-unspecified")
  val propertyTradingStartDate = Some(LocalDate.parse((startYear - 1).toString + "-01-01"))
  val ukPropertyIncomeType = Some("uk-property")
  val foreignPropertyIncomeType = Some("foreign-property")

  def propertyAccountingStartLocalDateOfCurrentYear(year: Int) = LocalDate.of(year, 1, 1)

  def propertyAccounringEndLocalDateOfCurrentYear(year: Int) = LocalDate.of(year, 12, 31)


  val property: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    propertyIncomeType,
    propertyTradingStartDate,
    None,
    None,
    cashOrAccruals = false
  )

  val oldProperty: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1)),
    propertyIncomeType,
    propertyTradingStartDate,
    None,
    None,
    cashOrAccruals = false
  )

  def propertyWithCurrentYear(endYear: Int): PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDateOfCurrentYear(endYear),
      end = propertyAccounringEndLocalDateOfCurrentYear(endYear)
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDateOfCurrentYear(endYear)),
    propertyIncomeType,
    propertyTradingStartDate,
    None,
    None,
    cashOrAccruals = false
  )

  val ukProperty: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    ukPropertyIncomeType,
    propertyTradingStartDate,
    None,
    None,
    cashOrAccruals = false
  )

  val ukPropertyWithUnknowns: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    ukPropertyIncomeType,
    None,
    None,
    None,
    cashOrAccruals = false
  )

  val foreignProperty: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    foreignPropertyIncomeType,
    propertyTradingStartDate,
    None,
    None,
    cashOrAccruals = false
  )

  val foreignPropertyWithUnknowns: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    foreignPropertyIncomeType,
    None,
    None,
    None,
    cashOrAccruals = false
  )

  val propertySuccessResponse: JsValue = Json.obj(
    "incomeSourceId" -> testPropertyIncomeId,
    "accountingPeriod" -> Json.obj(
      "start" -> propertyAccountingStart,
      "end" -> propertyAccountingEnd
    )
  )

  val ukPropertyAudit: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    incomeSourceType = ukPropertyIncomeType,
    tradingStartDate = propertyTradingStartDate,
    None,
    cessation = None,
    cashOrAccruals = false,
    latencyDetails = Some(testLatencyDetails3)
  )

  val foreignPropertyAudit: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    incomeSourceType = foreignPropertyIncomeType,
    tradingStartDate = propertyTradingStartDate,
    None,
    cessation = None,
    cashOrAccruals = false,
    latencyDetails = Some(testLatencyDetails3)
  )

  val ceasedForeignProperty: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    incomeSourceType = foreignPropertyIncomeType,
    tradingStartDate = propertyTradingStartDate,
    contextualTaxYear = None,
    cessation = Some(CessationModel(Some(LocalDate.of(endYear, 12, 31)))),
    cashOrAccruals = false,
    latencyDetails = Some(testLatencyDetails3)
  )

  val ceasedUkProperty: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    incomeSourceType = ukPropertyIncomeType,
    tradingStartDate = propertyTradingStartDate,
    contextualTaxYear = None,
    cessation = Some(CessationModel(Some(LocalDate.of(endYear, 12, 31)))),
    cashOrAccruals = false,
    latencyDetails = Some(testLatencyDetails3)
  )

}