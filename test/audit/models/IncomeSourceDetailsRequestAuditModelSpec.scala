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

import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import auth.MtdItUserWithNino
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name

class IncomeSourceDetailsRequestAuditModelSpec extends WordSpecLike with MustMatchers {

  val transactionName = "income-source-details-request"
  val auditType = "incomeSourceDetailsRequest"
  val mtdUserWithNino: MtdItUserWithNino[_] = MtdItUserWithNino(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    saUtr = Some("saUtr"),
    credId = Some("credId"),
    userType = Some("agent"),
    arn = Some("arn"))(FakeRequest())

  val incomeSourceDetailsRequestAuditFull: IncomeSourceDetailsRequestAuditModel =
    IncomeSourceDetailsRequestAuditModel(
      mtdItUser = MtdItUserWithNino(
        saUtr = Some("saUtr"),
        nino = "nino",
        mtditid = "mtditid",
        arn = Some("arn"),
        userType = Some("Agent"),
        credId = Some("credId"),
        userName = Some(Name(Some("firstName"), Some("lastName")))
      )(FakeRequest())
    )

  val incomeSourceDetailsRequestAuditMin: IncomeSourceDetailsRequestAuditModel =
    IncomeSourceDetailsRequestAuditModel(
      mtdItUser = MtdItUserWithNino(
        mtditid = "mtditid",
        nino = "nino",
        userName = None,
        saUtr = None,
        credId = None,
        userType = None,
        arn = None
      )(FakeRequest())
    )

  "IncomeSourceDetailsRequestAudit(testMtdUserNino)" should {

    s"have the correct transaction name of '$transactionName'" in {
      incomeSourceDetailsRequestAuditFull.transactionName mustBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      incomeSourceDetailsRequestAuditFull.auditType mustBe auditType
    }

    "have the correct details for the audit event" when {
      "the income source details request audit has all detail" when {
        "there are MtdItUserWithNino details" in {
          incomeSourceDetailsRequestAuditFull.detail mustBe Json.obj(
            "saUtr" -> "saUtr",
            "nationalInsuranceNumber" -> "nino",
            "agentReferenceNumber" -> "arn",
            "userType" -> "Agent",
            "credId" -> "credId",
            "mtditid" -> "mtditid"
          )
        }
      }

      "the income source details request audit has minimal details" in {
        incomeSourceDetailsRequestAuditMin.detail mustBe Json.obj(
          "nationalInsuranceNumber" -> "nino",
          "mtditid" -> "mtditid"
        )
      }
    }


  }
}
