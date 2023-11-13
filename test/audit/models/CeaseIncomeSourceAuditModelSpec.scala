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

import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment, UkProperty}
import play.api.libs.json.Json
import testConstants.BaseTestConstants.{testPropertyIncomeId, testSelfEmploymentId}
import testConstants.UpdateIncomeSourceTestConstants.failureResponse
import testUtils.TestSupport

class CeaseIncomeSourceAuditModelSpec extends TestSupport {

  val transactionName = enums.TransactionName.CeaseIncomeSource.name
  val auditType = enums.AuditType.CeaseIncomeSource.name
  val cessationDate = "2022-08-01"
  val hcWithDeviceID = headerCarrier.copy(deviceID = Some("some device id"))

  def getCeaseIncomeSourceAuditModel(incomeSourceType: IncomeSourceType, isAgent: Boolean, isError: Boolean): CeaseIncomeSourceAuditModel = {
    (incomeSourceType, isAgent, isError) match {
      case (SelfEmployment, false, true) => CeaseIncomeSourceAuditModel(incomeSourceType, cessationDate, testSelfEmploymentId, Some(failureResponse))
      case (SelfEmployment, false, false) => CeaseIncomeSourceAuditModel(incomeSourceType, cessationDate, testSelfEmploymentId, None)
      case (SelfEmployment, true, false) => CeaseIncomeSourceAuditModel(incomeSourceType, cessationDate, testSelfEmploymentId, None)(agentUserConfirmedClient(), headerCarrier)
      case _ => CeaseIncomeSourceAuditModel(incomeSourceType, cessationDate, testPropertyIncomeId, None)
    }
  }


  val detailIndividualSE = Json.parse(
    """{
      |    "nationalInsuranceNumber": "AB123456C",
      |    "mtditid": "XAIT0000123456",
      |    "nino": "AB123456C",
      |    "saUtr": "testSaUtr",
      |    "credId": "testCredId",
      |    "userType": "Individual",
      |    "outcome": {
      |        "isSuccessful": true
      |    },
      |    "journeyType": "SE",
      |    "dateBusinessStopped": "2022-08-01",
      |    "incomeSourceID": "XA00001234",
      |    "businessName": "nextUpdates.business"
      |}""".stripMargin)

  val detailAgentSE = Json.parse(
    """{
      |    "nationalInsuranceNumber": "AA111111A",
      |    "mtditid": "XAIT00000000015",
      |    "nino": "AA111111A",
      |    "agentReferenceNumber": "XAIT0000123456",
      |    "saUtr": "1234567890",
      |    "credId": "testCredId",
      |    "userType": "Agent",
      |    "outcome": {
      |        "isSuccessful": true
      |    },
      |    "journeyType": "SE",
      |    "dateBusinessStopped": "2022-08-01",
      |    "incomeSourceID": "XA00001234",
      |    "businessName": "nextUpdates.business"
      |}""".stripMargin)

  val detailOutcomeError = Json.parse(
    """{
      |    "nationalInsuranceNumber": "AB123456C",
      |    "mtditid": "XAIT0000123456",
      |    "nino": "AB123456C",
      |    "saUtr": "testSaUtr",
      |    "credId": "testCredId",
      |    "userType": "Individual",
      |    "outcome": {
      |        "isSuccessful": false,
      |        "failureCategory": "INTERNAL_SERVER_ERROR",
      |        "failureReason": "Error message"
      |    },
      |    "journeyType": "SE",
      |    "dateBusinessStopped": "2022-08-01",
      |    "incomeSourceID": "XA00001234",
      |    "businessName": "nextUpdates.business"
      |}""".stripMargin)

  val detailProperty = Json.parse(
    """{
      |    "nationalInsuranceNumber": "AB123456C",
      |    "mtditid": "XAIT0000123456",
      |    "nino": "AB123456C",
      |    "saUtr": "testSaUtr",
      |    "credId": "testCredId",
      |    "userType": "Individual",
      |    "outcome": {
      |        "isSuccessful": true
      |    },
      |    "journeyType": "UKPROPERTY",
      |    "dateBusinessStopped": "2022-08-01",
      |    "incomeSourceID": "1234"
      |}""".stripMargin)


  "CeaseIncomeSourceAuditModel" should {
    s"have the correct transaction name of '$transactionName'" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, isAgent = false, isError = false).transactionName shouldBe transactionName
    }
  }

  s"have the correct audit event type of '$auditType'" in {
    getCeaseIncomeSourceAuditModel(SelfEmployment, isAgent = false, isError = false).auditType shouldBe auditType
  }

  "have the correct detail for the audit event" when {
    "user is an Individual and when income source type is Self Employment" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, isAgent = false, isError = false).detail shouldBe detailIndividualSE
    }

    "user is an Agent and when income source type is Self Employment" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, isAgent = true, isError = false).detail shouldBe detailAgentSE
    }

    "error while updating income source" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, isAgent = false, isError = true).detail shouldBe detailOutcomeError
    }

    "user is an Individual and when income source type is Property" in {
      getCeaseIncomeSourceAuditModel(UkProperty, isAgent = false, isError = false).detail shouldBe detailProperty
    }
  }
}
