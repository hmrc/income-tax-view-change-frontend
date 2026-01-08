/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class AdjustPaymentsOnAccountAuditModelSpec extends TestSupport {

  val transactionName: String = enums.TransactionName.AdjustPaymentsOnAccount.name
  val auditType: String = enums.AuditType.AdjustPaymentsOnAccount.name

  def getAdjustPaymentsOnAccountAuditModel(isSuccessful: Boolean,isAgent: Boolean): AdjustPaymentsOnAccountAuditModel = {
    val typeOfUser = if(isAgent) getAgentUser(FakeRequest()) else getIndividualUser(FakeRequest())
    AdjustPaymentsOnAccountAuditModel(isSuccessful = isSuccessful,
      previousPaymentOnAccountAmount = 3000.00,
      requestedPaymentOnAccountAmount = 2500.00,
      adjustmentReasonCode = "001",
      adjustmentReasonDescription = "My main income will be lower",
      isDecreased = true)(typeOfUser)
  }

  lazy val detailsAuditDataSuccess: AffinityGroup => JsValue = af =>
    commonAuditDetails(af) ++ Json.obj(
      "outcome" -> Json.obj("isSuccessful" -> true),
      "previousPaymentOnAccountAmount" -> 3000.00,
      "requestedPaymentOnAccountAmount" -> 2500.00,
      "adjustmentReasonCode" -> "001",
      "adjustmentReasonDescription" -> "My main income will be lower",
      "isDecreased" -> true
    )

  lazy val detailsAuditDataFailure: AffinityGroup => JsValue = af =>
    commonAuditDetails(af) ++ Json.obj(
      "outcome" -> Json.obj(
        "isSuccessful" -> false,
        "failureCategory" -> "API_FAILURE",
        "failureReason" -> "API 1773 returned errors - not able to update payments on account"
      ),
      "previousPaymentOnAccountAmount" -> 3000.00,
      "requestedPaymentOnAccountAmount" -> 2500.00,
      "adjustmentReasonCode" -> "001",
      "adjustmentReasonDescription" -> "My main income will be lower",
      "isDecreased" -> true
    )

  "AdjustPaymentsOnAccountAuditModel for a successful API post" should {
    s"have the correct transaction name of - $transactionName" in {
      getAdjustPaymentsOnAccountAuditModel(true,isAgent = false).transactionName shouldBe transactionName
    }

    s"have the correct audit event type of - $auditType" in {
      getAdjustPaymentsOnAccountAuditModel(true,isAgent = false).auditType shouldBe auditType
    }

    "have the correct detail for the audit event" in {
      assertJsonEquals(getAdjustPaymentsOnAccountAuditModel(true,isAgent = false).detail, detailsAuditDataSuccess(Individual))
    }

    "have the correct agent detail for the audit event" in {
      getAdjustPaymentsOnAccountAuditModel(true,isAgent = true).detail shouldBe detailsAuditDataSuccess(Agent)
    }
  }

  "AdjustPaymentsOnAccountAuditModel for an unsuccessful API post" should {
    s"have the correct transaction name of - $transactionName" in {
      getAdjustPaymentsOnAccountAuditModel(false,isAgent = false).transactionName shouldBe transactionName
    }

    s"have the correct audit event type of - $auditType" in {
      getAdjustPaymentsOnAccountAuditModel(false,isAgent = false).auditType shouldBe auditType
    }

    "have the correct detail for the audit event" in {
      assertJsonEquals(getAdjustPaymentsOnAccountAuditModel(false,isAgent = false).detail, detailsAuditDataFailure(Individual))
    }

    "have the correct detail for the agent audit event" in {
      getAdjustPaymentsOnAccountAuditModel(false,isAgent = true).detail shouldBe detailsAuditDataFailure(Agent)
    }
  }
}