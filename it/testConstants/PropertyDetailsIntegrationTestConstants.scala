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

import java.time.LocalDate

import BaseIntegrationTestConstants.testPropertyIncomeId
import PaymentHistoryTestConstraints.getCurrentTaxYearEnd
import implicits.ImplicitDateFormatter
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.PropertyDetailsModel
import play.api.libs.json.{JsValue, Json}

object PropertyDetailsIntegrationTestConstants {

  val propertyAccountingStart = "2017-01-01"
  val propertyAccountingStartLocalDate = LocalDate.of(2017, 1, 1)
  val propertyAccountingEnd = "2017-12-31"
  val propertyAccounringEndLocalDate = LocalDate.of(2017, 12, 31)

  def propertyAccountingStartLocalDateOfCurrentYear(year: Int) = LocalDate.of(year, 1, 1)

  def propertyAccounringEndLocalDateOfCurrentYear(year: Int) = LocalDate.of(year, 12, 31)


  val property: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate)
  )

  val oldProperty: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1))
  )

  def propertyWithCurrentYear(endYear: Int): PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDateOfCurrentYear(endYear),
      end = propertyAccounringEndLocalDateOfCurrentYear(endYear)
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDateOfCurrentYear(endYear))
  )

  val propertySuccessResponse: JsValue = Json.obj(
    "incomeSourceId" -> testPropertyIncomeId,
    "accountingPeriod" -> Json.obj(
      "start" -> propertyAccountingStart,
      "end" -> propertyAccountingEnd
    )
  )

}