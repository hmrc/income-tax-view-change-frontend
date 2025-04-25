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
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import models.core.IncomeSourceId.mkIncomeSourceId
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseTestConstants._
import testConstants.UpdateIncomeSourceTestConstants.failureResponse
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class CeaseIncomeSourceAuditModelSpec extends TestSupport {

  val transactionName = enums.TransactionName.CeaseIncomeSource.name
  val auditType = enums.AuditType.CeaseIncomeSource.name
  val cessationDate = "2022-08-01"
  val hcWithDeviceID = headerCarrier.copy(deviceID = Some("some device id"))

  def getCeaseIncomeSourceAuditModel(incomeSourceType: IncomeSourceType, mtdUserRole: MTDUserRole, isError: Boolean): CeaseIncomeSourceAuditModel = {
    (incomeSourceType, mtdUserRole, isError) match {
      case (SelfEmployment, MTDIndividual, true) => CeaseIncomeSourceAuditModel(incomeSourceType, cessationDate, mkIncomeSourceId(testSelfEmploymentId), Some(failureResponse))
      case (SelfEmployment, MTDIndividual, false) => CeaseIncomeSourceAuditModel(incomeSourceType, cessationDate, mkIncomeSourceId(testSelfEmploymentId), None)
      case (SelfEmployment, ur, false) => CeaseIncomeSourceAuditModel(incomeSourceType, cessationDate, mkIncomeSourceId(testSelfEmploymentId), None)(agentUserConfirmedClient(ur == MTDSupportingAgent))
      case _ => CeaseIncomeSourceAuditModel(incomeSourceType, cessationDate, mkIncomeSourceId(testPropertyIncomeId), None)
    }
  }

  lazy val detailsAuditDataSuccess: (MTDUserRole, IncomeSourceType) => JsValue = (mtdUserRole, incomeSourceType) => {
    val journeyDetails = if(incomeSourceType == SelfEmployment) Json.obj(
      "journeyType" -> "SE",
      "incomeSourceID" -> "XA00001234",
      "businessName" -> "nextUpdates.business"
      )
    else Json.obj(
      "journeyType" -> "UKPROPERTY",
      "incomeSourceID" -> "1234"
    )
    val (af, isSupportingAgent) = mtdUserRole match {
      case MTDIndividual => (Individual, false)
      case ur => (Agent, ur == MTDSupportingAgent)
    }
    commonAuditDetails(af, isSupportingAgent) ++ Json.obj(
      "outcome" -> Json.obj("isSuccessful" -> true),
      "dateBusinessStopped" -> "2022-08-01"
    ) ++ journeyDetails
  }

  lazy val detailsAuditDataFailure: MTDUserRole => JsValue = mtdUserRole => {
    val (af, isSupportingAgent) = mtdUserRole match {
      case MTDIndividual => (Individual, false)
      case ur => (Agent, ur == MTDSupportingAgent)
    }
    commonAuditDetails(af, isSupportingAgent) ++ Json.obj(
      "outcome" -> Json.obj(
        "isSuccessful" -> false,
        "failureCategory" -> "API_FAILURE",
        "failureReason" -> "Error message"
      ),
      "journeyType" -> "SE",
      "dateBusinessStopped" -> "2022-08-01",
      "incomeSourceID" -> "XA00001234",
      "businessName" -> "nextUpdates.business"
    )
  }

  "CeaseIncomeSourceAuditModel" should {
    s"have the correct transaction name of '$transactionName'" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = false).transactionName shouldBe transactionName
    }
  }

  s"have the correct audit event type of '$auditType'" in {
    getCeaseIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = false).auditType shouldBe auditType
  }

  "have the correct detail for the audit event" when {
    "user is an Individual and when income source type is Self Employment" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = false).detail shouldBe detailsAuditDataSuccess(MTDIndividual, SelfEmployment)
    }

    "user is an primary Agent and when income source type is Self Employment" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, MTDPrimaryAgent, isError = false).detail shouldBe detailsAuditDataSuccess(MTDPrimaryAgent, SelfEmployment)
    }

    "user is an supporting Agent and when income source type is Self Employment" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, MTDSupportingAgent, isError = false).detail shouldBe detailsAuditDataSuccess(MTDSupportingAgent, SelfEmployment)
    }

    "error while updating income source" in {
      getCeaseIncomeSourceAuditModel(SelfEmployment, MTDIndividual, isError = true).detail shouldBe detailsAuditDataFailure(MTDIndividual)
    }

    "user is an Individual and when income source type is Property" in {
      getCeaseIncomeSourceAuditModel(UkProperty, MTDIndividual, isError = false).detail shouldBe detailsAuditDataSuccess(MTDIndividual, UkProperty)
    }
  }
}
