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

import java.time.LocalDate
import assets.ChargeListTestConstants._
import assets.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testSaUtr, testUserType}
import auth.MtdItUser
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.libs.json.Json
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Name

class WhatYouOweResponseAuditModelSpec extends TestSupport {

  val transactionName = "what-you-owe-response"
  val auditEvent = "WhatYouOweResponse"


  val dueDateInFuture = LocalDate.now().plusDays(45).toString
  val dueDateIsSoon = LocalDate.now().plusDays(1).toString
  val dueDateInPast = LocalDate.now().minusDays(10).toString

  val outStandingCharges = LocalDate.now().minusMonths(13).toString

  def testWhatYouOweResponseAuditModel(userType: Option[String] = Some("Agent")): WhatYouOweResponseAuditModel = WhatYouOweResponseAuditModel(
    user = MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel(testMtditid, None, List.empty, None),
      saUtr = Some(testSaUtr),
      credId = Some(testCredId),
      userType = userType,
      arn = if (userType.contains("Agent")) Some(testArn) else None
    ), whatYouOwePartialData
  )

  "The WhatYouOweResponseAuditModel" should {

    s"Have the correct transaction name of '$transactionName'" in {
      testWhatYouOweResponseAuditModel().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testWhatYouOweResponseAuditModel().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" in {
      testWhatYouOweResponseAuditModel(Some("Individual")).detail shouldBe Json.obj(
        "userType" -> "Individual",
        "saUtr" -> testSaUtr,
        "nationalInsuranceNumber" -> testNino,
        "credId" -> testCredId,
        "mtditid" -> testMtditid,
        "charges" -> Json.arr(
          Json.obj(
            "chargeType" -> "Payment on account 1 of 2",
            "dueDate" -> dueDateInPast,
            "outstandingAmount" -> 50
          ),
          Json.obj(
            "chargeType" -> "Payment on account 2 of 2",
            "dueDate" -> dueDateIsSoon,
            "outstandingAmount" -> 75
          ),
          Json.obj(
            "chargeType" -> "Payment on account 1 of 2",
            "dueDate" -> dueDateInFuture,
            "outstandingAmount" -> 50
          ),
          Json.obj("accruingInterest" -> 12.67,
            "chargeType" -> "Remaining balance",
            "dueDate" -> outStandingCharges,
            "outstandingAmount" -> 123456.67
          )
        )
      )
    }
  }
}
