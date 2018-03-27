/*
 * Copyright 2018 HM Revenue & Customs
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

package assets

import assets.BaseTestConstants.testPropertyIncomeId
import assets.ReportDeadlinesTestConstants._
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{PropertiesRentedModel, PropertyDetailsModel}
import models.incomeSourcesWithDeadlines.PropertyIncomeWithDeadlinesModel
import utils.ImplicitDateFormatter

object PropertyDetailsTestConstants extends ImplicitDateFormatter {

  val testPropertyAccountingPeriod = AccountingPeriodModel("2017-04-06", "2018-04-05")

  val propertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = testPropertyAccountingPeriod,
    propertiesRented = Some(PropertiesRentedModel(
      uk = Some(2),
      eea = None,
      nonEea = None,
      total = Some(2)
    )),
    cessation = None,
    contactDetails = None,
    paperless = None
  )

  val propertyIncomeModel = PropertyIncomeWithDeadlinesModel(
    propertyDetails,
    obligationsDataSuccessModel
  )
}