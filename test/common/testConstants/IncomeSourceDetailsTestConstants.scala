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

package common.testConstants

import common.enums.TriggeredMigration.Channel.HmrcUnconfirmed
import common.models.core.AddressModel
import common.models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import common.testConstants.BaseTestConstants.*
import common.testConstants.BusinessDetailsTestConstants.*

import java.time.LocalDate

object IncomeSourceDetailsTestConstants {

  val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
  val singleBusinessIncomeWithYearOfMigration = IncomeSourceDetailsModel("AA123456A", testMtditid, Some("2018"), List(business1), Nil, channel = HmrcUnconfirmed.getValue)
  val singleBusinessIncomeNoYearOfMigration = IncomeSourceDetailsModel("AA123456A", testMtditid, None, List(business1), Nil)
  val singleBusinessIncomeUnconfirmed = singleBusinessIncomeNoYearOfMigration.copy(channel = HmrcUnconfirmed.getValue)
  val dualBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1, business1), Nil)
  val singleBusinessIncomeNoLatency = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1NoLatency), Nil)
  val singleBusinessIncomeWithLatency2019 = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(businessWithLatency2019), Nil)
  val singleBusinessIncome2023 = IncomeSourceDetailsModel(testNino, testMtditid, Some("2023"), List(businessWithLatency1), Nil)
  val singleBusinessIncome2023WithUnknowns = IncomeSourceDetailsModel(testNino, testMtditid, Some("2023"), List(businessWithLatencyAndUnknowns), Nil)
  val singleBusinessIncome2024 = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), List(businessWithLatency4), Nil)
  val incomeSourceWithOneYearInLatency = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), List(businessWithOneYearInLatency), Nil)
  val incomeSourceWithNoLatencyDetails = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), Nil, Nil)
  val incomeSourceWithBothYearsInLatency = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), List(businessWithBothYearsInLatency), Nil)
  val singleBusinessIncomeNotMigrated = IncomeSourceDetailsModel(testNino, testMtditid, None, List(business1), Nil)
  val singleBusinessIncomeWithCurrentYear = IncomeSourceDetailsModel(testNino, testMtditid, Some(fixedDate.getYear.toString), List(business1), Nil)
  val businessIncome2018and2019 = IncomeSourceDetailsModel(testNino, testMtditid, None, List(business2018, business2019), Nil)
  val propertyIncomeOnly = IncomeSourceDetailsModel(testNino, testMtditid, None, List(), List(propertyDetails))
  val businessAndPropertyAligned = IncomeSourceDetailsModel(testNino, testMtditid, Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    List(alignedBusiness), List(propertyDetails))
  val singleBusinessAndPropertyMigrat2019 = IncomeSourceDetailsModel(testNino, testMtditid, Some(testMigrationYear2019), List(alignedBusiness), List(propertyDetails))
  val noIncomeDetails = IncomeSourceDetailsModel(nino = testNino, mtdbsa = testMtditid, yearOfMigration = None, businesses = List(), properties = Nil)
  val errorResponse = IncomeSourceDetailsError(testErrorStatus, testErrorMessage)
  val businessIncome2018and2019AndProp = IncomeSourceDetailsModel(testNino, testMtditid, None, List(business2018, business2019), List(propertyDetails))
  val businessInternational = IncomeSourceDetailsModel(testNino, testMtditid, None, List(business1International), List(propertyDetails))
  val oldUserDetails = IncomeSourceDetailsModel(testNino, testMtditid, Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    List(oldUseralignedBusiness), List(propertyDetails))
  val preSanitised = IncomeSourceDetailsModel(testNino, testMtditid, Some((fixedDate.getYear - 1).toString), List(business2018, alignedBusiness), List(propertyDetails))

  val businessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusiness), List())
  val businessIncome2 = IncomeSourceDetailsModel(testNino, testMtditid2, Some("2018"), List(soleTraderBusiness2), List())
  val businessIncome3 = IncomeSourceDetailsModel(testNino, testMtditid2, Some("2018"), List(soleTraderBusiness, soleTraderBusiness2), List())
  val businessIncome4 = IncomeSourceDetailsModel(testNino, testMtditid2, Some("2018"), List(soleTraderBusiness3), List())

  val addressModel1: Option[AddressModel] = Some(AddressModel(
    addressLine1 = Some("Line 1"),
    addressLine2 = Some("Line 2"),
    addressLine3 = Some("Line 3"),
    addressLine4 = Some("Line 4"),
    postCode = Some("LN1 1NL"),
    countryCode = Some("NI")
  ))
  val addressModel2: Option[AddressModel] = Option(AddressModel(
    addressLine1 = Some("A Line 1"),
    addressLine2 = None,
    addressLine3 = Some("A Line 3"),
    addressLine4 = None,
    postCode = Some("LN2 2NL"),
    countryCode = Some("GB")
  ))

  def getCurrentTaxEndYear(currentDate: LocalDate): Int = {
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }
}
