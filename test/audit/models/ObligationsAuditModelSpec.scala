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

import auth.MtdItUser
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import play.api.libs.json.Json
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testSaUtr}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.obligationsViewModelSimple
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class ObligationsAuditModelSpec extends TestSupport {

  val transactionName = "obligations"
  val auditEvent = "Obligations"

  val obligationsViewModel: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDatesYearOne = Seq(DatesModel(
      LocalDate.of(2022, 1, 6),
      LocalDate.of(2023, 4, 5),
      LocalDate.of(2024, 5, 5),
      "Quarterly",
      false,
    )),
    Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = false
  )

  val obligationsViewModelEmpty: ObligationsViewModel = ObligationsViewModel(
    Seq.empty, Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = false
  )

  def testObligationsAuditModel(userType: Option[AffinityGroup] = Some(Agent), incomeSourceType: IncomeSourceType,
                                obligations: ObligationsViewModel, businessName: String, reportingMethod: String): ObligationsAuditModel = ObligationsAuditModel(
    incomeSourceType = incomeSourceType,
    obligations = obligations,
    businessName = businessName,
    reportingMethod = reportingMethod,
    taxYear = TaxYear(2023, 2024)
  )(user = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(Name(Some("firstName"), Some("lastName"))),
    incomeSources = IncomeSourceDetailsModel(testMtditid, None, List.empty, List.empty),
    btaNavPartial = None,
    saUtr = Some(testSaUtr),
    credId = Some(testCredId),
    userType = userType,
    arn = if (userType.contains(Agent)) Some(testArn) else None
  ))

  "The ObligationsAuditModel" should {
    s"Have the correct transaction name of '$transactionName'" in {
      testObligationsAuditModel(userType = Some(Individual),
        incomeSourceType = SelfEmployment,
        obligations = obligationsViewModel,
        businessName = "businessName",
        reportingMethod = "quarterly").transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testObligationsAuditModel(userType = Some(Individual),
        incomeSourceType = SelfEmployment,
        obligations = obligationsViewModel,
        businessName = "businessName",
        reportingMethod = "quarterly").auditType shouldBe auditEvent
    }
    "Have the correct audit contents of obligations information when all obligations information is defined" in {
      testObligationsAuditModel(userType = Some(Individual),
        incomeSourceType = SelfEmployment,
        obligations = obligationsViewModelSimple,
        businessName = "businessName",
        reportingMethod = "quarterly").detail shouldBe Json.obj(
        "nationalInsuranceNumber" -> "AB123456C",
        "mtditid" -> "XAIT0000123456",
        "saUtr" -> "testSaUtr",
        "credId" -> "testCredId",
        "userType" -> "Individual",
        "journeyType" -> "SE",
        "incomeSourceInfo" -> Json.obj(
          "businessName" -> "businessName",
          "reportingMethod" -> "Quarterly",
          "taxYear" -> "2023-2024"
        ),

        "quarterlyUpdates" -> Json.arr(
          Json.obj(
            "taxYear" -> "2023-2024",
            "quarter" -> Json.arr(
              Json.obj(
                "startDate" -> "2023-01-06",
                "endDate" -> "2023-04-05",
                "deadline" -> "2023-05-05"
              )
            ),
          ),
          Json.obj(
            "taxYear" -> "2024-2025",
            "quarter" -> Json.arr(
              Json.obj(
                "startDate" -> "2024-01-06",
                "endDate" -> "2024-04-05",
                "deadline" -> "2024-05-05"
              )
            ),
          )
        ),
        "EOPstatement" -> Json.arr(
          Json.obj(
            "taxYearStartDate" -> "2023-04-06",
            "taxYearEndDate" -> "2024-04-05",
            "deadline" -> "2025-01-31"
          )),
        "finalDeclaration" -> Json.arr(
          Json.obj(
            "taxYearStartDate" -> "2023-04-06",
            "taxYearEndDate" -> "2024-04-05",
            "deadline" -> "2025-01-31"
          )),
      )
    }
    "Display that the fields for audit are removed from the audit when the obligations information non-existent" in {
      testObligationsAuditModel(userType = Some(Individual),
        incomeSourceType = SelfEmployment,
        obligations = obligationsViewModelEmpty,
        businessName = "businessName",
        reportingMethod = "quarterly").detail shouldBe Json.obj(
        "nationalInsuranceNumber" -> "AB123456C",
        "mtditid" -> "XAIT0000123456",
        "saUtr" -> "testSaUtr",
        "credId" -> "testCredId",
        "userType" -> "Individual",
        "journeyType" -> "SE",
        "incomeSourceInfo" -> Json.obj(
          "businessName" -> "businessName",
          "reportingMethod" -> "Quarterly",
          "taxYear" -> "2023-2024"
        )
      )
    }
  }
}
