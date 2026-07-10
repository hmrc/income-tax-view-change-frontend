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

package businessDetails.testConstants

import businessDetails.models.incomeSourceDetails.*
import businessDetails.models.incomeSourceDetails.viewmodels.CheckCeaseIncomeSourceDetailsViewModel
import businessDetails.models.updateIncomeSource.*
import businessDetails.testConstants.PropertyDetailsTestConstants.*
import common.enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import common.enums.JourneyType.{Add, IncomeSourceJourneyType}
import common.models.core.IncomeSourceId
import common.models.incomeSourceDetails.IncomeSourceDetailsModel
import common.testConstants.BaseTestConstants.*
import common.testConstants.BusinessDetailsTestConstants.*
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.Status
import shared.models.UIJourneySessionData
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate

object UpdateIncomeSourceTestConstants {
  val incomeSourceId = "11111111111"
  val cessationDate = "2023-04-01"
  val taxYearSpecific = TaxYearSpecific("2022", true)
  val request: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceID = incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val requestTaxYearSpecific: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceID = incomeSourceId,
    taxYearSpecific = Some(taxYearSpecific)
  )

  val requestTaxYearSpecificJson: JsValue = Json.obj(
    "nino" -> testNino,
    "incomeSourceID" -> incomeSourceId,
    "taxYearSpecific" -> Json.obj("taxYear" -> "2022", "latencyIndicator" -> true)
  )

  val requestJson: JsValue = Json.obj(
    "nino" -> testNino,
    "incomeSourceID" -> incomeSourceId,
    "cessation" -> Json.obj("cessationIndicator" -> true, "cessationDate" -> cessationDate)
  )
  val badRequest = request.copy(cessation = None)
  val errorBadResponse = UpdateIncomeSourceResponseError("BAD_REQUEST", "Dummy Message")
  val failureResponse = UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Error message")
  val badJsonResponse = UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
  val successResponse = UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")
  val successResponseJson = Json.obj("processingDate" -> "2022-01-31T09:26:17Z")

  val successHttpResponse = HttpResponse(Status.OK, Json.toJson(successResponse), Map.empty)
  val successInvalidJsonResponse = HttpResponse(Status.OK, Json.toJson(""), Map.empty)
  val badHttpResponse = HttpResponse(Status.BAD_REQUEST, "Dummy Message", Map.empty)

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
  val businessesAndPropertyIncomeCeased = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(ceasedBusiness), List(ceasedPropertyDetails))


  val foreignPropertyIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, List(foreignPropertyDetails))
  val foreignPropertyIncomeWithCeasedForiegnPropertyIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, List(foreignPropertyDetails, ceasedForeignPropertyDetailsCessation2023))
  val twoActiveUkPropertyIncomes = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, List(uKPropertyDetails, uKPropertyDetails2))

  val foreignPropertyAndCeasedBusinessesIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(ceasedBusiness2), List(foreignPropertyDetails, ceasedUKPropertyDetailsCessation2020, ceasedForeignPropertyDetailsCessation2023))
  val foreignPropertyAndCeasedBusinessIncomeNoStartDate = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(ceasedBusiness, ceasedBusiness2), List(foreignPropertyDetailsNoStartDate))
  val foreignPropertyAndCeasedPropertyIncomeWithNoIncomeSourceType = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(), List(foreignPropertyDetails, ceasedUKPropertyDetailsCessation2020, ceasedForeignPropertyDetailsNoIncomeSourceType))


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
            address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"), Some(Country(Some("GB"), Some("United Kingdom"))))),
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
          )))
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
