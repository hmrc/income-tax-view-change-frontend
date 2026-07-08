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

import businessDetails.models.incomeSourceDetails.viewmodels.DatesModel
import businessDetails.testConstants.PropertyDetailsIntegrationTestConstants.*
import common.models.core.{AccountingPeriodModel, AddressModel, CessationModel}
import common.models.incomeSourceDetails.*
import common.models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import common.testConstants.BaseIntegrationTestConstants.*
import common.testConstants.IncomeSourceIntegrationTestConstants.ukProperty

import java.time.LocalDate

object BusinessDetailsIntegrationTestConstants {
  
  val b2CessationDate = LocalDate.of(endYear, 12, 31)

  val b3TradingName = "thirdBusiness"
  val b2AccountingStart = LocalDate.of(endYear, 1, 1)
  val b2AccountingEnd = LocalDate.of(endYear, 12, 31)

  val ceasedBusinessTradingName = "ceasedBusiness"
  val testBusinessAddress: AddressModel = AddressModel(
    addressLine1 = Some("64 Zoo Lane"),
    addressLine2 = Some("Happy Place"),
    addressLine3 = Some("Magical Land"),
    addressLine4 = Some("England"),
    postCode = Some("ZL1 064"),
    countryCode = Some("GB")
  )

  val businessWithId = BusinessDetailsModel(
    incomeSourceId = "ID",
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = Some(b1TradingName),
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(b1TradingStart),
    contextualTaxYear = None,
    cessation = None,
    address = Some(address)
  )

  val business1WithAddress2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = Some(b1TradingName),
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(b1TradingStart),
    contextualTaxYear = None,
    cessation = None,
    address = Some(testBusinessAddress)
  )

  val business3 = BusinessDetailsModel(
    incomeSourceId = otherTestSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(
      start = b2AccountingStart,
      end = b2AccountingEnd
    )),
    tradingName = Some(b3TradingName),
    firstAccountingPeriodEndDate = Some(b2AccountingEnd),
    tradingStartDate = Some(b2TradingStart),
    contextualTaxYear = None,
    cessation = Some(CessationModel(Some(LocalDate.of(2020, 1, 1)))),
    address = Some(address)
  )

  val business3WithUnknowns: BusinessDetailsModel = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = None,
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = None,
    contextualTaxYear = None,
    cessation = None,
    address = None
  )

  val ceasedBusiness1 = BusinessDetailsModel(
    incomeSourceId = otherTestSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(
      start = b2AccountingStart,
      end = b2AccountingEnd
    )),
    tradingName = Some(ceasedBusinessTradingName),
    firstAccountingPeriodEndDate = Some(b2AccountingEnd),
    tradingStartDate = Some(b2TradingStart),
    contextualTaxYear = None,
    cessation = Some(CessationModel(Some(b2CessationDate))),
    address = Some(address)
  )

  val businessUnknownAddressName = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = None,
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = None,
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(b1TradingStart),
    contextualTaxYear = None,
    cessation = None,
    address = None
  )

  val businessWithLatencyForManageYourDetailsAudit = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(testDate),
    contextualTaxYear = None,
    cessation = None,
    address = expectedAddress,
    latencyDetails = Some(testLatencyDetails3)
  )
  
  val datesModelSeq2022: Seq[DatesModel] = Seq(
    DatesModel(
      LocalDate.of(2022, 1, 6),
      LocalDate.of(2022, 4, 5),
      LocalDate.of(2022, 5, 5),
      "#001",
      false,
      obligationType = "Quarterly"
    )
  )

  val datesModelSeq2023: Seq[DatesModel] = Seq(
    DatesModel(
      LocalDate.of(2023, 1, 6),
      LocalDate.of(2023, 4, 5),
      LocalDate.of(2023, 5, 5),
      "#001",
      false,
      obligationType = "Quarterly"
    )
  )

  val testQuarterlyObligationDates: Seq[Seq[DatesModel]] = Seq(datesModelSeq2022, datesModelSeq2023)

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel("123", List(
      SingleObligationModel(
        LocalDate.of(2022, 1, 6),
        LocalDate.of(2022, 4, 5),
        LocalDate.of(2022, 5, 5),
        "Quarterly",
        None,
        "#001",
        StatusFulfilled
      ),
      SingleObligationModel(
        LocalDate.of(2022, 1, 6),
        LocalDate.of(2022, 4, 5),
        LocalDate.of(2022, 5, 5),
        "Quarterly",
        None,
        "#002",
        StatusFulfilled
      )
    ))
  ))

  val singleBusinessResponse2: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1WithAddress2),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  def singleBusinessResponseInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  def singleBusinessResponseInLatencyPeriod2(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1WithAddress2.copy(latencyDetails = Some(latencyDetails))),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  def singleBusinessResponseWithUnknownsInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business3WithUnknowns.copy(latencyDetails = Some(latencyDetails))),
    properties = Nil,
    yearOfMigration = Some("2018")
  )

  def singleUKPropertyResponseInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = List(ukProperty.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def singleUKForeignPropertyResponseInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(businessWithId.copy(latencyDetails = Some(latencyDetails))),
    properties = List(ukProperty.copy(latencyDetails = Some(latencyDetails)), foreignProperty.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def singleUKPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = List(ukPropertyWithUnknowns.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def singleForeignPropertyResponseInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = List(foreignProperty.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  def singleForeignPropertyResponseWithUnknownsInLatencyPeriod(latencyDetails: LatencyDetails): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1.copy(latencyDetails = Some(latencyDetails))),
    properties = List(foreignPropertyWithUnknowns.copy(latencyDetails = Some(latencyDetails))),
    yearOfMigration = Some("2018")
  )

  val multipleBusinessesWithBothPropertiesAndCeasedBusiness: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1,
      business2,
      business3
    ),
    properties = List(ukProperty, foreignProperty),
    yearOfMigration = Some("2018")
  )
  
  val propertyOnlyBusiness: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(ukProperty, foreignProperty),
    yearOfMigration = Some("2018")
  )

  val foreignPropertyAndCeasedBusiness: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      ceasedBusiness1
    ),
    properties = List(foreignProperty),
    yearOfMigration = Some("2018")
  )

  val allCeasedBusinesses: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      ceasedBusiness1
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )
  
  val businessWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      businessWithLatencyForManageYourDetailsAudit
    ),
    properties = List(foreignProperty),
    yearOfMigration = Some("2018")
  )


  val ukPropertyOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(ukProperty),
    yearOfMigration = Some("2018")
  )


  val businessOnlyResponseWithUnknownAddressName: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      businessUnknownAddressName
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val businessOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val businessOnlyResponseWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1.copy(latencyDetails = Some(testLatencyDetails3))
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val businessOnlyResponseAllCeased: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      ceasedBusiness1
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val ukPropertyOnlyResponseWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(ukProperty.copy(latencyDetails = Some(testLatencyDetails3))),
    yearOfMigration = Some("2018")
  )

  val ukPropertyOnlyResponseAllCeased: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(ceasedUkProperty),
    yearOfMigration = Some("2018")
  )

  val foreignPropertyOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(foreignProperty),
    yearOfMigration = Some("2018")
  )

  val foreignPropertyOnlyResponseWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(foreignProperty.copy(latencyDetails = Some(testLatencyDetails3))),
    yearOfMigration = Some("2018")
  )

  val foreignPropertyOnlyResponseAllCeased: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(ceasedForeignProperty),
    yearOfMigration = Some("2018")
  )

  val noPropertyOrBusinessResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtditid, None,
    List(), Nil
  )

}