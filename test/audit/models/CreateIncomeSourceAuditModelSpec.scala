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

package audit.models

import enums.FailureCategory.ApiFailure
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import play.api.libs.json.Json
import testConstants.BaseTestConstants.testSelfEmploymentId
import testUtils.TestSupport

import java.time.LocalDate

class CreateIncomeSourceAuditModelSpec extends TestSupport {

  val transactionName = enums.TransactionName.CreateIncomeSource.name
  val auditType = enums.AuditType.CreateIncomeSource.name
  val hcWithDeviceID = headerCarrier.copy(deviceID = Some("some device id"))
  val failureCategory = ApiFailure
  val failureReason = "Failure Reason"
  val incomeSourceId = testSelfEmploymentId

  val createBusinessViewModel = CheckBusinessDetailsViewModel(
    businessName = Some("someBusinessName"),
    businessStartDate = Some(LocalDate.of(2022, 1, 1)),
    accountingPeriodEndDate = LocalDate.of(2023, 1, 11),
    businessTrade = "someBusinessTrade",
    businessPostalCode = Some("TE5 7TT"),
    businessAddressLine1 = "2 Test Lane",
    businessAddressLine2 = Some("Test Unit"),
    businessAddressLine3 = None,
    businessAddressLine4 = Some("Test City"),
    businessCountryCode = Some("GB"),
    incomeSourcesAccountingMethod = Some("cash"),
    cashOrAccrualsFlag = "CASH",
    showedAccountingMethod = false
  )

  val createForeignPropertyViewModel = CheckPropertyViewModel(
    tradingStartDate = LocalDate.of(2022, 1, 1),
    cashOrAccrualsFlag = "CASH",
    incomeSourceType = ForeignProperty)

  val createUKPropertyViewModel = CheckPropertyViewModel(
    tradingStartDate = LocalDate.of(2022, 1, 1),
    cashOrAccrualsFlag = "CASH",
    incomeSourceType = UkProperty)

  def getCreateIncomeSourceAuditModel(incomeSourceType: IncomeSourceType, isAgent: Boolean, isError: Boolean): CreateIncomeSourceAuditModel = {
    (incomeSourceType, isAgent, isError) match {
      case (SelfEmployment, false, true) => CreateIncomeSourceAuditModel(incomeSourceType, createBusinessViewModel, Some(failureCategory), Some(failureReason), None)
      case (SelfEmployment, false, false) => CreateIncomeSourceAuditModel(incomeSourceType, createBusinessViewModel, None, None, Some(CreateIncomeSourceResponse(incomeSourceId)))
      case (SelfEmployment, true, false) => CreateIncomeSourceAuditModel(incomeSourceType, createBusinessViewModel, None, None, Some(CreateIncomeSourceResponse(incomeSourceId)))(agentUserConfirmedClient())
      case _ => CreateIncomeSourceAuditModel(incomeSourceType, createForeignPropertyViewModel, None, None, Some(CreateIncomeSourceResponse(incomeSourceId)))
    }
  }


  val detailIndividualSE = Json.parse(
    """{
      |    "nationalInsuranceNumber": "AB123456C",
      |    "mtditid": "XAIT0000123456",
      |    "saUtr": "testSaUtr",
      |    "credId": "testCredId",
      |    "userType": "Individual",
      |    "outcome": {
      |        "isSuccessful": true
      |    },
      |    "journeyType": "SE",
      |    "addedIncomeSourceID": "XA00001234",
      |    "dateStarted": "2022-01-01",
      |    "businessName": "someBusinessName",
      |    "businessDescription": "someBusinessTrade",
      |    "addressLine1": "2 Test Lane",
      |    "addressLine2": "Test Unit",
      |    "addressTownOrCity": "Test City",
      |    "addressPostcode": "TE5 7TT",
      |    "addressCountry": "GB",
      |    "accountingMethod":"Cash basis accounting"
      |}""".stripMargin)


  val detailAgentSE = Json.parse(
    """{
      |    "nationalInsuranceNumber": "AA111111A",
      |    "mtditid": "XAIT00000000015",
      |    "agentReferenceNumber": "XAIT0000123456",
      |    "saUtr": "1234567890",
      |    "credId": "testCredId",
      |    "userType": "Agent",
      |    "outcome": {
      |        "isSuccessful": true
      |    },
      |    "journeyType": "SE",
      |    "addedIncomeSourceID": "XA00001234",
      |    "dateStarted": "2022-01-01",
      |    "businessName": "someBusinessName",
      |    "businessDescription": "someBusinessTrade",
      |    "addressLine1": "2 Test Lane",
      |    "addressLine2": "Test Unit",
      |    "addressTownOrCity": "Test City",
      |    "addressPostcode": "TE5 7TT",
      |    "addressCountry": "GB",
      |    "accountingMethod":"Cash basis accounting"
      |}""".stripMargin)


  val detailOutcomeError = Json.parse(
    """{
      |    "nationalInsuranceNumber": "AB123456C",
      |    "mtditid": "XAIT0000123456",
      |    "saUtr": "testSaUtr",
      |    "credId": "testCredId",
      |    "userType": "Individual",
      |    "outcome": {
      |        "isSuccessful": false,
      |        "failureCategory": "API_FAILURE",
      |        "failureReason": "Failure Reason"
      |    },
      |    "journeyType": "SE",
      |    "dateStarted": "2022-01-01",
      |    "businessName": "someBusinessName",
      |    "businessDescription": "someBusinessTrade",
      |    "addressLine1": "2 Test Lane",
      |    "addressLine2": "Test Unit",
      |    "addressTownOrCity": "Test City",
      |    "addressPostcode": "TE5 7TT",
      |    "addressCountry": "GB",
      |    "accountingMethod":"Cash basis accounting"
      |}""".stripMargin)

  val detailProperty = Json.parse(
    """{
      |    "nationalInsuranceNumber": "AB123456C",
      |    "mtditid": "XAIT0000123456",
      |    "saUtr": "testSaUtr",
      |    "credId": "testCredId",
      |    "userType": "Individual",
      |    "outcome": {
      |        "isSuccessful": true
      |    },
      |    "journeyType": "UKPROPERTY",
      |    "addedIncomeSourceID":"XA00001234",
      |    "dateStarted": "2022-01-01",
      |    "accountingMethod":"Cash basis accounting"
      |}""".stripMargin)

  "CeaseIncomeSourceAuditModel" should {
    s"have the correct transaction name of - $transactionName" in {
      getCreateIncomeSourceAuditModel(SelfEmployment, isAgent = false, isError = false).transactionName shouldBe transactionName
    }
  }

  s"have the correct audit event type of - $auditType" in {
    getCreateIncomeSourceAuditModel(SelfEmployment, isAgent = false, isError = false).auditType shouldBe auditType
  }

  "have the correct detail for the audit event" when {
    "user is an Individual and when income source type is Self Employment" in {
      getCreateIncomeSourceAuditModel(SelfEmployment, isAgent = false, isError = false).detail shouldBe detailIndividualSE
    }

    "user is an Agent and when income source type is Self Employment" in {
      getCreateIncomeSourceAuditModel(SelfEmployment, isAgent = true, isError = false).detail shouldBe detailAgentSE
    }

    "error while updating income source" in {
      getCreateIncomeSourceAuditModel(SelfEmployment, isAgent = false, isError = true).detail shouldBe detailOutcomeError
    }

    "user is an Individual and when income source type is Property" in {
      getCreateIncomeSourceAuditModel(UkProperty, isAgent = false, isError = false).detail shouldBe detailProperty
    }
  }
}