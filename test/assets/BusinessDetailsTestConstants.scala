/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus, testSelfEmploymentId, testSelfEmploymentId2}
import assets.ReportDeadlinesTestConstants._
import models.core._
import models.incomeSourceDetails.BusinessDetailsModel
import models.incomeSourcesWithDeadlines.BusinessIncomeWithDeadlinesModel
import models.reportDeadlines.ReportDeadlinesModel

object BusinessDetailsTestConstants {

  val testBusinessAccountingPeriod = AccountingPeriodModel(start = "2017-6-1", end = "2018-5-30")
  val test2019BusinessAccountingPeriod = AccountingPeriodModel(start = "2018-3-5", end = "2019-3-6")
  val test2018BusinessAccountingPeriod = AccountingPeriodModel(start = "2017-3-5", end = "2018-3-6")
  val testTradeName = "business"
  val testTradeName2 = "business"
  val testBizAddress = AddressModel(
    addressLine1 = "64 Zoo Lane",
    addressLine2 = Some("Happy Place"),
    addressLine3 = Some("Magical Land"),
    addressLine4 = Some("England"),
    postCode = Some("ZL1 064"),
    countryCode = "UK"
  )
  val testContactDetails = ContactDetailsModel(Some("123456789"),Some("0123456789"),Some("8008135"),Some("google@chuckNorris.com"))
  val testCessation = CessationModel(Some("2018-1-1".toLocalDate), Some("It was a stupid idea anyway"))

  val business1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = testBusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None
  )


  val business2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    accountingPeriod = testBusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName2),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None
  )

  val business2018 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = test2018BusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None
  )

  val business2019 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = test2019BusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None
  )

  val alignedBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(start = "2017-4-6", end = "2018-4-5"),
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None
  )

  val ceasedBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = testBusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = Some(testCessation),
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None
  )

  val businessErrorModel = ErrorModel(testErrorStatus, testErrorMessage)

  val obligationsDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testSelfEmploymentId, List(overdueObligation, openObligation))

  val businessIncomeModel =
    BusinessIncomeWithDeadlinesModel(
      business1,
      obligationsDataSuccessModel
    )

  val businessIncomeModel2 =
    BusinessIncomeWithDeadlinesModel(
      business2,
      obligationsDataSuccessModel.copy(identification = business2.incomeSourceId)
    )

  val business2018IncomeModel =
    BusinessIncomeWithDeadlinesModel(
      business2018,
      obligationsDataSuccessModel
    )

  val businessIncomeModelAlignedTaxYear =
    BusinessIncomeWithDeadlinesModel(
      alignedBusiness,
      obligationsDataSuccessModel
    )

  val business2019IncomeModel =
    BusinessIncomeWithDeadlinesModel(
      business2019,
      obligationsDataSuccessModel
    )
}