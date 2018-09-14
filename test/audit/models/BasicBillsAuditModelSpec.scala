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
import audit.models.BillsAuditing.BasicBillsAuditModel

import utils.TestSupport

class BasicBillsAuditModelSpec extends TestSupport {

  val transactionName = "view-bills-page"
  val auditType = "billsPageView"

  "The BasicBillsAuditModel" should {

    val fullBillsAuditModel = BasicBillsAuditModel(testMtdItUser, testCalcModel)

    s"have the correct transaction name of '$transactionName'" in {
      fullBillsAuditModel.transactionName shouldBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      fullBillsAuditModel.auditType shouldBe auditType
    }

    "have the correct details for the audit event" when {

      "the current bill is available" in {
        fullBillsAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "currentBill" -> "123.45"
        )
      }

      "the current bill is not available" in {
        val billsAuditModel = BasicBillsAuditModel(testMtdItUser, testCalcModelNoDisplayAmount)

        billsAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtditid,
          "nino" -> testNino
        )
      }
    }
  }
}
