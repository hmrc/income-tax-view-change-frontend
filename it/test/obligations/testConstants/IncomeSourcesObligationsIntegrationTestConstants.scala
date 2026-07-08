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

package obligations.testConstants

import common.models.core.{AccountingPeriodModel, CessationModel}
import common.models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, IncomeSourceDetailsResponse, PropertyDetailsModel}
import common.models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import common.testConstants.BaseIntegrationTestConstants.*
import common.testConstants.IncomeSourceIntegrationTestConstants.ukProperty

import java.time.LocalDate

object IncomeSourcesObligationsIntegrationTestConstants {
  val taxYear: Int = 2022

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel("123", List(SingleObligationModel(
      LocalDate.of(taxYear, 1, 6),
      LocalDate.of(taxYear, 4, 5),
      LocalDate.of(taxYear, 5, 5),
      "Quarterly",
      None,
      "#001",
      StatusFulfilled),
      SingleObligationModel(
        LocalDate.of(taxYear, 1, 6),
        LocalDate.of(taxYear, 4, 5),
        LocalDate.of(taxYear, 5, 5),
        "Quarterly",
        None,
        "#002",
        StatusFulfilled
      )
    ))
  ))

  val businessOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val businessAndPropertyResponseWoMigration: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1),
    properties = List(property),
    yearOfMigration = None
  )

  val ceasedBusinessTradingName = "ceasedBusiness"
  val b2CessationDate = LocalDate.of(endYear, 12, 31)


  val ukPropertyOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(ukProperty),
    yearOfMigration = Some("2018")
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

  val foreignPropertyIncomeType = Some("foreign-property")

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

  val businessWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      businessWithLatencyForManageYourDetailsAudit
    ),
    properties = List(foreignProperty),
    yearOfMigration = Some("2018")
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
    latencyDetails = Some(testLatencyDetails3)
  )

  val propertyWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(business1),
    properties = List(foreignPropertyAudit),
    yearOfMigration = Some("2018")
  )

  val allBusinessesWithLatency: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(businessWithLatencyForManageYourDetailsAudit),
    properties = List(foreignPropertyAudit),
    yearOfMigration = Some("2018")
  )

  val propertyOnlyResponse: IncomeSourceDetailsModel =
    IncomeSourceDetailsModel(
      testNino,
      testMtditid,
      businesses = List(),
      properties = List(property),
      yearOfMigration = Some("2018")
    )

  val foreignAndSoleTraderCeasedBusiness: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(ceasedBusiness1),
    properties = List(ceasedForeignProperty),
    yearOfMigration = Some("2018")
  )

  val noPropertyOrBusinessResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    testNino,
    testMtditid, None,
    List(), Nil
  )
}
