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

package common.testConstants

import common.models.core.AccountingPeriodModel
import common.models.incomeSourceDetails.{IncomeSourceDetailsModel, IncomeSourceDetailsResponse, PropertyDetailsModel}
import common.testConstants.BaseIntegrationTestConstants.*

object IncomeSourceIntegrationTestConstants {

  val ukPropertyIncomeType = Some("uk-property")

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

  val multipleBusinessesAndPropertyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1,
      business2
    ),
    properties = List(property),
    yearOfMigration = Some("2018")
  )

  val multipleBusinessesAndUkProperty: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1,
      business2
    ),
    properties = List(ukProperty),
    yearOfMigration = Some("2018")
  )

  val singleBusinessResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  val multipleBusinessesResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    businesses = List(
      business1,
      business2
    ),
    properties = Nil,
    yearOfMigration = Some("2019")
  )

}


