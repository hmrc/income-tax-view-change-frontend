/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import assets.BaseTestConstants.testPropertyIncomeId
import assets.ReportDeadlinesTestConstants.fakeReportDeadlinesModel
import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.{PropertiesRentedModel, PropertyDetailsModel}
import models.reportDeadlines.ReportDeadlineModel

object PropertyDetailsTestConstants {

  val testPropertyAccountingPeriod = AccountingPeriodModel(LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))

  val testCessation = CessationModel(Some(LocalDate.of(2018, 1, 1)), Some("It was a stupid idea anyway"))

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
    paperless = None,
    firstAccountingPeriodEndDate = None
  )

  val ceasedPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = testPropertyAccountingPeriod,
    propertiesRented = Some(PropertiesRentedModel(
      uk = Some(2),
      eea = None,
      nonEea = None,
      total = Some(2)
    )),
    cessation = Some(testCessation),
    contactDetails = None,
    paperless = None,
    firstAccountingPeriodEndDate = None
  )

  val openCrystallised: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2017, 10, 31),
    periodKey = "#003",
    dateReceived = None,
    obligationType = "Crystallised"
  ))
}
