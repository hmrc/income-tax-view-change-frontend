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

import auth.MtdItUser
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name

class TaxYearOverviewRequestAuditModelSpec extends WordSpecLike with MustMatchers {

  val transactionName: String = "tax-year-overview-request"
  val auditType: String = "TaxYearOverviewRequest"

  def taxYearOverviewRequestAuditFull(userType: Option[String] = Some("Agent"),
                                      agentReferenceNumber: Option[String]): TaxYearOverviewRequestAuditModel =
    TaxYearOverviewRequestAuditModel(
      mtdItUser = MtdItUser(
        mtditid = "mtditid",
        nino = "nino",
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel("mtditid", None, Nil, None),
        saUtr = Some("saUtr"),
        credId = Some("credId"),
        userType = userType,
        arn = agentReferenceNumber
      )(FakeRequest()),
      agentReferenceNumber = agentReferenceNumber
    )

  "TaxYearOverviewRequestAuditModel(mtdItUser, agentReferenceNumber)" should {

    s"have the correct transaction name of '$transactionName'" in {
      taxYearOverviewRequestAuditFull(
        agentReferenceNumber = Some("1")
      ).transactionName mustBe transactionName
    }

    s"have the correct audit tyoe of '$auditType'" in {
      taxYearOverviewRequestAuditFull(
        agentReferenceNumber = Some("1")
      ).auditType mustBe auditType
    }

    "have the correct details for the Audit event" when {
      "the user type is Individual" in {
        taxYearOverviewRequestAuditFull(
          userType = Some("Individual"),
          agentReferenceNumber = None
        ).detail mustBe Json.obj(
          "nationalInsuranceNumber" -> "nino",
          "mtditid" -> "mtditid",
          "saUtr" -> "saUtr",
          "credId" -> "credId",
          "userType" -> "Individual"
        )
      }

      "the user type is Agent" in {
        taxYearOverviewRequestAuditFull(
          userType = Some("Agent"),
          agentReferenceNumber = Some("agentReferenceNumber")
        ).detail mustBe Json.obj(
          "nationalInsuranceNumber" -> "nino",
          "mtditid" -> "mtditid",
          "agentReferenceNumber" -> "agentReferenceNumber",
          "saUtr" -> "saUtr",
          "credId" -> "credId",
          "userType" -> "Agent"
        )
      }
    }
  }

}
