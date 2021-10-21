/*
 * Copyright 2021 HM Revenue & Customs
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

import testConstants.BaseTestConstants._
import testConstants.CalcBreakdownTestConstants._
import audit.models.EstimatesAuditing.EstimatesAuditModel
import testUtils.TestSupport

class EstimatesAuditModelSpec extends TestSupport {

  val transactionName: String = "estimates-page-view"
  val auditType: String = "estimatesPageView"

  "The EstimatesAuditModel" should {

    lazy val testEstimatesAuditModel = EstimatesAuditModel(testMtdItUser, testCalcDisplayModel)

    s"Have the correct transaction name of '$transactionName'" in {
      testEstimatesAuditModel.transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditType'" in {
      testEstimatesAuditModel.auditType shouldBe auditType
    }

    "Have the correct details for the audit event" in {
      testEstimatesAuditModel.detail shouldBe Seq(
        "mtditid" -> testMtditid,
        "nino" -> testNino,
        "hasBusiness" -> "true",
        "hasProperty" -> "true",
        "bizAccPeriodStart" -> "2017-06-01",
        "bizAccPeriodEnd" -> "2018-05-30",
        "propAccPeriodStart" -> "2017-04-06",
        "propAccPeriodEnd" -> "2018-04-05"
      )
    }
  }
}
