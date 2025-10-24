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

package testConstants.incomeSources

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import models.UIJourneySessionData
import models.core.{AddressModel, IncomeSourceId}
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.{CeaseIncomeSourcesViewModel, CheckCeaseIncomeSourceDetailsViewModel}
import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants._
import testConstants.PropertyDetailsTestConstants._

import java.time.LocalDate

object IncomeSourceDetailsTestConstants {
  val businessesAndPropertyIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(business1, business2), List(propertyDetails))
  val businessesAndPropertyIncomeCeased = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(ceasedBusiness), List(ceasedPropertyDetails))
  val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
  val dualBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1, business1), Nil)
  val singleBusinessWithNoCashOrAccuralsFlag = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(businessWithNoCashOrAccrualsFlag), Nil)
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
  val noIncomeDetails = IncomeSourceDetailsModel(testNino, testMtditid, None, List(), Nil)
  val errorResponse = IncomeSourceDetailsError(testErrorStatus, testErrorMessage)
  val businessIncome2018and2019AndProp = IncomeSourceDetailsModel(testNino, testMtditid, None, List(business2018, business2019), List(propertyDetails))
  val oldUserDetails = IncomeSourceDetailsModel(testNino, testMtditid, Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    List(oldUseralignedBusiness), List(propertyDetails))
  val preSanitised = IncomeSourceDetailsModel(testNino, testMtditid, Some((fixedDate.getYear - 1).toString), List(business2018, alignedBusiness), List(propertyDetails))

  val businessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusiness), List())
  val businessIncome2 = IncomeSourceDetailsModel(testNino, testMtditid2, Some("2018"), List(soleTraderBusiness2), List())
  val businessIncome3 = IncomeSourceDetailsModel(testNino, testMtditid2, Some("2018"), List(soleTraderBusiness, soleTraderBusiness2), List())
  val businessIncome4 = IncomeSourceDetailsModel(testNino, testMtditid2, Some("2018"), List(soleTraderBusiness3), List())
  val ukPropertyIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(), List(ukPropertyDetails))
  val ukPropertyIncomeWithCeasedUkPropertyIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(), List(ukPropertyDetails))
  val ukPropertyWithSoleTraderBusiness = IncomeSourceDetailsModel(testNino, testMtditid, None, List(business2018), List(ukPropertyDetails, ceasedUKPropertyDetailsCessation2020))
  val ukPlusForeignPropertyWithSoleTraderIncomeSource = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusiness), List(ukPropertyDetails, foreignPropertyDetails))
  val ukForeignSoleTraderIncomeSourceBeforeEarliestStartDate = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusiness3), List(ukPropertyDetails3BeforeEarliest, foreignPropertyDetailsBeforeEarliest))
  val ukForeignSoleTraderIncomeSourceBeforeContextualTaxYear = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusiness4), List(ukPropertyDetailsBeforeContextualTaxYear, foreignPropertyDetailsBeforeContextualTaxYear))
  val ukPropertyAndSoleTraderBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusiness), List(ukPropertyDetails))
  val ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusiness, ceasedBusiness), List(ukPropertyDetails, foreignPropertyDetails))
  val soleTraderWithStartDate2005 = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusinessWithStartDate2005), List())
  val ukPropertyAndSoleTraderBusinessIncomeNoTradingName = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(soleTraderBusinessNoTradingName), List(ukPropertyDetails))
  val singleUKPropertyIncome2023 = IncomeSourceDetailsModel(testNino, testMtditid, Some("2023"), Nil, List(ukPropertyWithLatencyDetails1))
  val singleForeignPropertyIncome2023 = IncomeSourceDetailsModel(testNino, testMtditid, Some("2023"), Nil, List(foreignPropertyWithLatencyDetails1))
  val singleUKPropertyIncome2024 = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), Nil, List(ukPropertyWithLatencyDetails2))
  val singleForeignPropertyIncome2024 = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), Nil, List(foreignPropertyWithLatencyDetails2))
  val ukPlusForeignPropertyAndSoleTraderNoLatency = IncomeSourceDetailsModel(testNino, testMtditid, Some("2023"), List(soleTraderBusiness), List(ukPropertyDetails, foreignPropertyDetails))
  val ukPlusForeignPropertyAndSoleTraderWithLatency = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), List(businessWithLatency2), List(ukPropertyWithLatencyDetails2, foreignPropertyWithLatencyDetails2))
  val ukPlusForeignPropertyAndSoleTraderWithLatencyExpired = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), List(businessWithLatency2019), List(ukPropertyWithLatencyPeriodExpired, foreignPropertyWithLatencyPeriodExpired))
  val ukPlusForeignPropertyAndSoleTraderWithLatencyAnnual = IncomeSourceDetailsModel(testNino, testMtditid, Some("2024"), List(businessWithOneYearInLatency2023), List(ukPropertyWithLatencyDetails2, foreignPropertyWithLatencyDetails2))
  val ukPlusForeignPropertyAndSoleTrader2023WithUnknowns = IncomeSourceDetailsModel(testNino, testMtditid, Some("2023"), List(businessWithLatencyAndUnknowns), List(ukPropertyWithLatencyDetailsAndUnknowns, foreignPropertyWithLatencyDetailsAndUnknowns))
  val twoActiveUkPropertyBusinesses = IncomeSourceDetailsModel(testNino, testMtditid, Some("2023"), List(), List(ukPropertyDetails2, ukPropertyDetails))
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

  val foreignPropertyAndCeasedBusinessesIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(ceasedBusiness2), List(foreignPropertyDetails, ceasedUKPropertyDetailsCessation2020, ceasedForeignPropertyDetailsCessation2023))
  val foreignPropertyAndCeasedBusinessIncomeNoStartDate = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(ceasedBusiness, ceasedBusiness2), List(foreignPropertyDetailsNoStartDate))
  val foreignPropertyAndCeasedPropertyIncomeWithNoIncomeSourceType = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(), List(foreignPropertyDetails, ceasedUKPropertyDetailsCessation2020, ceasedForeignPropertyDetailsNoIncomeSourceType))


  val foreignPropertyIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, List(foreignPropertyDetails))
  val twoActiveForeignPropertyIncomes = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, List(foreignPropertyDetails, foreignPropertyDetails2))
  val foreignPropertyIncomeWithCeasedForiegnPropertyIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, List(foreignPropertyDetails, ceasedForeignPropertyDetailsCessation2023))
  val twoActiveUkPropertyIncomes = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, List(uKPropertyDetails, uKPropertyDetails2))

  val ceaseBusinessDetailsModel = CeaseIncomeSourcesViewModel(
    soleTraderBusinesses = List(ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2),
    ukProperty = Some(ceaseUkPropertyDetailsViewModel),
    foreignProperty = Some(ceaseForeignPropertyDetailsViewModel),
    ceasedBusinesses = Nil,
    displayStartDate = true)

  val checkCeaseBusinessDetailsModel = CheckCeaseIncomeSourceDetailsViewModel(
    incomeSourceId = IncomeSourceId(testSelfEmploymentId),
    tradingName = Some(testTradeName),
    trade = Some(testIncomeSource),
    address = Some(address),
    businessEndDate = LocalDate.of(2022, 1, 1),
    SelfEmployment
  )

  val checkCeaseUkPropertyDetailsModel = CheckCeaseIncomeSourceDetailsViewModel(
    incomeSourceId = IncomeSourceId(testSelfEmploymentId),
    tradingName = None,
    trade = None,
    address = None,
    businessEndDate = LocalDate.of(2022, 1, 1),
    UkProperty
  )

  val checkCeaseForeignPropertyDetailsModel = CheckCeaseIncomeSourceDetailsViewModel(
    incomeSourceId = IncomeSourceId(testSelfEmploymentId),
    tradingName = None,
    trade = None,
    address = None,
    businessEndDate = LocalDate.of(2022, 1, 1),
    ForeignProperty
  )

  def getCurrentTaxEndYear(currentDate: LocalDate): Int = {
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val emptyUIJourneySessionData: IncomeSourceJourneyType => UIJourneySessionData = journeyType => {
    journeyType.operation.operationType match {
      case "ADD" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = journeyType.toString,
          addIncomeSourceData = Some(AddIncomeSourceData())
        )
      case "MANAGE" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = journeyType.toString,
          manageIncomeSourceData = Some(ManageIncomeSourceData())
        )
      case "CEASE" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = journeyType.toString,
          ceaseIncomeSourceData = Some(CeaseIncomeSourceData())
        )
    }
  }

  def notCompletedUIJourneySessionData(journeyType: IncomeSourceJourneyType): UIJourneySessionData = {
    journeyType.operation.operationType match {
      case "ADD" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = journeyType.toString,
          addIncomeSourceData = Some(AddIncomeSourceData(
            businessName = Some("A Business Name"),
            businessTrade = Some("A Trade"),
            dateStarted = Some(LocalDate.of(2022, 1, 1)),
            accountingPeriodStartDate = Some(LocalDate.of(2022, 4, 5)),
            accountingPeriodEndDate = Some(LocalDate.of(2023, 4, 6)),
            incomeSourceId = Some(testSelfEmploymentId),
            address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"))),
            countryCode = Some("A Country"),
            incomeSourcesAccountingMethod = Some("cash"),
            reportingMethodTaxYear1 = None,
            reportingMethodTaxYear2 = None,
            incomeSourceCreatedJourneyComplete = None
          ))
        )
      case "MANAGE" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = journeyType.toString,
          manageIncomeSourceData = Some(ManageIncomeSourceData(
            incomeSourceId = Some(testSelfEmploymentId),
            journeyIsComplete = None
          ))
        )
      case "CEASE" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = journeyType.toString,
          ceaseIncomeSourceData = Some(CeaseIncomeSourceData(
            incomeSourceId = if (journeyType.businessType == SelfEmployment) Some(testSelfEmploymentId) else None,
            endDate = Some(LocalDate.of(2022, 10, 10)),
            ceaseIncomeSourceDeclare = Some("true"),
            journeyIsComplete = None
          ))
        )
    }
  }

  val addedIncomeSourceUIJourneySessionData: IncomeSourceType => UIJourneySessionData = (incomeSourceType: IncomeSourceType) =>
    UIJourneySessionData(testSessionId, IncomeSourceJourneyType(Add, incomeSourceType).toString,
      addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  val completedUIJourneySessionData: IncomeSourceJourneyType => UIJourneySessionData = (journeyType: IncomeSourceJourneyType) => {
    journeyType.operation.operationType match {
      case "ADD" => UIJourneySessionData(testSessionId, journeyType.toString,
        addIncomeSourceData = Some(notCompletedUIJourneySessionData(journeyType).addIncomeSourceData.get.copy(incomeSourceCreatedJourneyComplete = Some(true))))
      case "MANAGE" => UIJourneySessionData(testSessionId, journeyType.toString,
        manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId),
          taxYear = Some(2023), reportingMethod = Some("annual"), journeyIsComplete = Some(true))))
      case "CEASE" => UIJourneySessionData(testSessionId, journeyType.toString,
        ceaseIncomeSourceData = Some(CeaseIncomeSourceData(journeyIsComplete = Some(true))))
    }
  }
}
