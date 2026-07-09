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

package businessDetails.testConstants

import common.models.core.{AccountingPeriodModel, CessationModel}
import common.models.incomeSourceDetails.PropertyDetailsModel
import common.testConstants.BaseIntegrationTestConstants.*

import java.time.LocalDate

object PropertyDetailsIntegrationTestConstants {

  val propertyTradingStartDate = Some(LocalDate.parse((startYear - 1).toString + "-01-01"))
  val ukPropertyIncomeType = Some("uk-property")
  val foreignPropertyIncomeType = Some("foreign-property")


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
    latencyDetails = Some(testLatencyDetails3)
  )

}