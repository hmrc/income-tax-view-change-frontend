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
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckPropertyViewModel}
import play.api.libs.json.{JsObject, Json}
import testConstants.BaseTestConstants.testSelfEmploymentId
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

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
    businessCountryName = Some("United Kingdom"),
    addressId = None
  )

  val createForeignPropertyViewModel = CheckPropertyViewModel(
    tradingStartDate = LocalDate.of(2022, 1, 1),
    incomeSourceType = ForeignProperty)

  val createUKPropertyViewModel = CheckPropertyViewModel(
    tradingStartDate = LocalDate.of(2022, 1, 1),
    incomeSourceType = UkProperty)

  def getCreateIncomeSourceAuditModel(incomeSourceType: IncomeSourceType, mtdUserRole: MTDUserRole, isError: Boolean, isTrigMig: Boolean = false): CreateIncomeSourceAuditModel = {
    (incomeSourceType, mtdUserRole, isError) match {
      case (SelfEmployment, MTDIndividual, true) => CreateIncomeSourceAuditModel(incomeSourceType, createBusinessViewModel, Some(failureCategory), Some(failureReason), None, isTrigMig)
      case (SelfEmployment, MTDIndividual, false) => CreateIncomeSourceAuditModel(incomeSourceType, createBusinessViewModel, None, None, Some(CreateIncomeSourceResponse(incomeSourceId)), isTrigMig)
      case (SelfEmployment, ur, false) => CreateIncomeSourceAuditModel(incomeSourceType, createBusinessViewModel, None, None, Some(CreateIncomeSourceResponse(incomeSourceId)), isTrigMig)(agentUserConfirmedClient(ur == MTDSupportingAgent))
      case _ => CreateIncomeSourceAuditModel(incomeSourceType, createForeignPropertyViewModel, None, None, Some(CreateIncomeSourceResponse(incomeSourceId)), isTrigMig)
    }
  }

  val seAuditDetails: (Boolean, Boolean) => JsObject = (isSuccess, isTrigMig) => {
    val outcome = if (isSuccess) {
      Json.obj(
        "isSuccessful" -> true
      )
    } else {
      Json.obj(
        "isSuccessful" -> false,
        "failureCategory" -> "API_FAILURE",
        "failureReason" -> "Failure Reason"
      )
    }
    Json.obj(
      "outcome" -> outcome,
      "journeyType" -> "SE",
      "dateStarted" -> "2022-01-01",
      "businessName" -> "someBusinessName",
      "businessDescription" -> "someBusinessTrade",
      "addressLine1" -> "2 Test Lane",
      "addressLine2" -> "Test Unit",
      "addressTownOrCity" -> "Test City",
      "addressPostcode" -> "TE5 7TT",
      "addressCountry" -> "GB",
      "isTriggeredMigration" -> isTrigMig
    ) ++ {
      if (isSuccess) Json.obj("addedIncomeSourceID" -> "XA00001234") else Json.obj()
    }
  }

  val detailIndividualSE = commonAuditDetails(Individual) ++ seAuditDetails(true, false)
  val detailIndividualSETrigMig = commonAuditDetails(Individual) ++ seAuditDetails(true, true)

  val detailAgentSE: Boolean => JsObject = isSupportingAgent => commonAuditDetails(Agent, isSupportingAgent) ++ seAuditDetails(true, false)
  val detailAgentSETrigMig: Boolean => JsObject = isSupportingAgent => commonAuditDetails(Agent, isSupportingAgent) ++ seAuditDetails(true, true)

  val detailOutcomeError = commonAuditDetails(Individual) ++ seAuditDetails(false, false)
  val detailOutcomeErrorTrigMig = commonAuditDetails(Individual) ++ seAuditDetails(false, true)

  val detailProperty = commonAuditDetails(Individual) ++ Json.obj(
    "outcome" -> Json.obj("isSuccessful" -> true),
    "journeyType" -> "UKPROPERTY",
    "addedIncomeSourceID" -> "XA00001234",
    "dateStarted" -> "2022-01-01",
    "isTriggeredMigration" -> false
  )
  val detailPropertyTrigMig = commonAuditDetails(Individual) ++ Json.obj(
    "outcome" -> Json.obj("isSuccessful" -> true),
    "journeyType" -> "UKPROPERTY",
    "addedIncomeSourceID" -> "XA00001234",
    "dateStarted" -> "2022-01-01",
    "isTriggeredMigration" -> true
  )

  "CeaseIncomeSourceAuditModel" should {
    s"have the correct transaction name of - $transactionName" in {
      getCreateIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = false).transactionName shouldBe transactionName
    }
  }

  s"have the correct audit event type of - $auditType" in {
    getCreateIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = false).auditType shouldBe auditType
  }

  "have the correct detail for the audit event" when {
    "user is an Individual and when income source type is Self Employment" in {
      assertJsonEquals(getCreateIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = false).detail, detailIndividualSE)
    }
    "user is an Individual and when income source type is Self Employment and is triggered migration journey" in {
      assertJsonEquals(getCreateIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = false, isTrigMig = true).detail, detailIndividualSETrigMig)
    }

    "user is an primary Agent and when income source type is Self Employment" in {
      assertJsonEquals(getCreateIncomeSourceAuditModel(SelfEmployment, MTDPrimaryAgent, isError = false).detail, detailAgentSE(false))
    }

    "user is an supporting Agent and when income source type is Self Employment" in {
      assertJsonEquals(getCreateIncomeSourceAuditModel(SelfEmployment, MTDSupportingAgent, isError = false).detail, detailAgentSE(true))
    }

    "error while updating income source" in {
      assertJsonEquals(getCreateIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = true).detail, detailOutcomeError)
    }

    "user is an Individual and when income source type is Property" in {
      assertJsonEquals(getCreateIncomeSourceAuditModel(UkProperty, MTDIndividual, isError = false).detail, detailProperty)
    }
    "user is an Individual and when income source type is Property and is triggered migration journey" in {
      assertJsonEquals(getCreateIncomeSourceAuditModel(UkProperty, MTDIndividual, isError = false, isTrigMig = true).detail, detailPropertyTrigMig)
    }
  }
}