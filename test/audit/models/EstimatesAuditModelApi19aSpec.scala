/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import audit.models.EstimatesAuditing.EstimatesAuditModelApi19a

import utils.TestSupport

class EstimatesAuditModelApi19aSpec extends TestSupport {

  val transactionName = "estimates-page-view-api-19a"
  val auditType = "estimatesPageView"

  "The EstimatesAuditModelApi19a" should {

    val fullEstimatesAuditModel = EstimatesAuditModelApi19a(testMtdItUser, testCalcModel)

    s"have the correct transaction name of '$transactionName'" in {
      fullEstimatesAuditModel.transactionName shouldBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      fullEstimatesAuditModel.auditType shouldBe auditType
    }

    "have the correct details for the audit event" when {

      "the annual and current estimates are available" in {
        fullEstimatesAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "annualEstimate" -> "987.65",
          "currentEstimate" -> "123.45"
        )
      }

      "the annual estimate is unavailable" in {
        val estimatesAuditModel = EstimatesAuditModelApi19a(testMtdItUser, testCalcModelNoAnnualEstimate)

        estimatesAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "currentEstimate" -> "123.45"
        )
      }

      "the current estimate is unavailable" in {
        val estimatesAuditModel = EstimatesAuditModelApi19a(testMtdItUser, testCalcModelNoDisplayAmount)

        estimatesAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "annualEstimate" -> "987.65"
        )
      }

      "no estimates are available" in {
        val estimatesAuditModel = EstimatesAuditModelApi19a(testMtdItUser, testCalcModelEmpty)

        estimatesAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtditid,
          "nino" -> testNino
        )
      }
    }
  }
}
