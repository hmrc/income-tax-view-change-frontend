/*
 * Copyright 2025 HM Revenue & Customs
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

package audit.reporting_obligations


import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent}
import play.api.libs.json.Json
import testUtils.TestSupport


class ReportingObligationsAuditModelSpec extends TestSupport {


  "ReportingObligationsAuditModel" when {

    ".details()" when {

      "is MTDIndividual and agent fields are None" should {

        "return the correct json" in {

          val details =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "fakeAuditType",
              credId = Some("fakeCredId"),
              mtditid = "fakeMtditid",
              nino = "fakeNino",
              saUtr = None,
              userType = MTDIndividual,
              grossIncomeThreshold = "£50,000",
              crystallisationStatusForPreviousTaxYear = true,
              itsaStatusTable =
                List(
                  ItsaStatusTableDetails(
                    taxYearPeriod = "CurrentTaxYear",
                    taxYear = "2025",
                    usingMakingTaxDigitalForIncomeTax = Some("Yes"),
                    userCurrentItsaStatus = "Voluntary",
                  )
                ),
              links = List("OptOut2025Single")
            ).detail

          val expected = Json.obj(
            "auditType" -> "fakeAuditType",
            "credId" -> "fakeCredId",
            "mtditid" -> "fakeMtditid",
            "nino" -> "fakeNino",
            "userType" -> "MTDIndividual",
            "grossIncomeThreshold" -> "£50,000",
            "crystallisationStatusForPreviousTaxYear" -> true,
            "itsaStatusTable" -> Json.arr(
              Json.obj(
                "taxYearPeriod" -> "CurrentTaxYear",
                "taxYear" -> "2025",
                "usingMakingTaxDigitalForIncomeTax" -> "Yes",
                "userCurrentItsaStatus" -> "Voluntary"
              )
            ),
            "links" -> Json.arr("OptOut2025Single")
          )

          details shouldBe expected
        }
      }

      "is an MTDPrimaryAgent and agent fields are present" should {

        "return the correct json" in {

          val details =
            ReportingObligationsAuditModel(
              agentReferenceNumber = Some("fakeagentReferenceNumber"),
              auditType = "fakeAuditType",
              credId = Some("fakeCredId"),
              mtditid = "fakeMtditid",
              nino = "fakeNino",
              saUtr = Some("fakeSAUTR"),
              userType = MTDPrimaryAgent,
              grossIncomeThreshold = "£50,000",
              crystallisationStatusForPreviousTaxYear = true,
              itsaStatusTable =
                List(
                  ItsaStatusTableDetails(
                    taxYearPeriod = "CurrentTaxYear",
                    taxYear = "2025",
                    usingMakingTaxDigitalForIncomeTax = Some("Yes"),
                    userCurrentItsaStatus = "Voluntary",
                  )
                ),
              links = List("OptOut2025Single")
            ).detail

          val expected = Json.obj(
            "agentReferenceNumber" -> "fakeagentReferenceNumber",
            "auditType" -> "fakeAuditType",
            "credId" -> "fakeCredId",
            "mtditid" -> "fakeMtditid",
            "nino" -> "fakeNino",
            "saUtr" -> "fakeSAUTR",
            "userType" -> "MTDPrimaryAgent",
            "grossIncomeThreshold" -> "£50,000",
            "crystallisationStatusForPreviousTaxYear" -> true,
            "itsaStatusTable" -> Json.arr(
              Json.obj(
                "taxYearPeriod" -> "CurrentTaxYear",
                "taxYear" -> "2025",
                "usingMakingTaxDigitalForIncomeTax" -> "Yes",
                "userCurrentItsaStatus" -> "Voluntary"
              )
            ),
            "links" -> Json.arr("OptOut2025Single")
          )

          details shouldBe expected
        }
      }

      "is an MTDSupportingAgent and agent fields are present" should {

        "return the correct json" in {

          val details =
            ReportingObligationsAuditModel(
              agentReferenceNumber = Some("fakeagentReferenceNumber"),
              auditType = "fakeAuditType",
              credId = Some("fakeCredId"),
              mtditid = "fakeMtditid",
              nino = "fakeNino",
              saUtr = Some("fakeSAUTR"),
              userType = MTDSupportingAgent,
              grossIncomeThreshold = "£50,000",
              crystallisationStatusForPreviousTaxYear = true,
              itsaStatusTable =
                List(
                  ItsaStatusTableDetails(
                    taxYearPeriod = "CurrentTaxYear",
                    taxYear = "2025",
                    usingMakingTaxDigitalForIncomeTax = Some("Yes"),
                    userCurrentItsaStatus = "Voluntary",
                  )
                ),
              links = List("OptOut2025Single")
            ).detail

          val expected = Json.obj(
            "agentReferenceNumber" -> "fakeagentReferenceNumber",
            "auditType" -> "fakeAuditType",
            "credId" -> "fakeCredId",
            "mtditid" -> "fakeMtditid",
            "nino" -> "fakeNino",
            "saUtr" -> "fakeSAUTR",
            "userType" -> "MTDSupportingAgent",
            "grossIncomeThreshold" -> "£50,000",
            "crystallisationStatusForPreviousTaxYear" -> true,
            "itsaStatusTable" -> Json.arr(
              Json.obj(
                "taxYearPeriod" -> "CurrentTaxYear",
                "taxYear" -> "2025",
                "usingMakingTaxDigitalForIncomeTax" -> "Yes",
                "userCurrentItsaStatus" -> "Voluntary"
              )
            ),
            "links" -> Json.arr("OptOut2025Single")
          )

          details shouldBe expected
        }
      }

      "there are multiple table rows and links" should {

        "return the correct json" in {

          val details =
            ReportingObligationsAuditModel(
              agentReferenceNumber = Some("fakeagentReferenceNumber"),
              auditType = "fakeAuditType",
              credId = Some("fakeCredId"),
              mtditid = "fakeMtditid",
              nino = "fakeNino",
              saUtr = Some("fakeSAUTR"),
              userType = MTDSupportingAgent,
              grossIncomeThreshold = "£50,000",
              crystallisationStatusForPreviousTaxYear = true,
              itsaStatusTable =
                List(
                  ItsaStatusTableDetails(
                    taxYearPeriod = "CurrentTaxYear",
                    taxYear = "2025",
                    usingMakingTaxDigitalForIncomeTax = Some("Yes"),
                    userCurrentItsaStatus = "Voluntary",
                  ),
                  ItsaStatusTableDetails(
                    taxYearPeriod = "NextTaxYear",
                    taxYear = "2026",
                    usingMakingTaxDigitalForIncomeTax = Some("Yes"),
                    userCurrentItsaStatus = "Voluntary",
                  )
                ),
              links = List("OptOut2025Onwards", "OptOut2026SingleTaxYear")
            ).detail

          val expected = Json.obj(
            "agentReferenceNumber" -> "fakeagentReferenceNumber",
            "auditType" -> "fakeAuditType",
            "credId" -> "fakeCredId",
            "mtditid" -> "fakeMtditid",
            "nino" -> "fakeNino",
            "saUtr" -> "fakeSAUTR",
            "userType" -> "MTDSupportingAgent",
            "grossIncomeThreshold" -> "£50,000",
            "crystallisationStatusForPreviousTaxYear" -> true,
            "itsaStatusTable" -> Json.arr(
              Json.obj(
                "taxYearPeriod" -> "CurrentTaxYear",
                "taxYear" -> "2025",
                "usingMakingTaxDigitalForIncomeTax" -> "Yes",
                "userCurrentItsaStatus" -> "Voluntary"
              ),
              Json.obj(
                "taxYearPeriod" -> "NextTaxYear",
                "taxYear" -> "2026",
                "usingMakingTaxDigitalForIncomeTax" -> "Yes",
                "userCurrentItsaStatus" -> "Voluntary"
              )
            ),
            "links" -> Json.arr("OptOut2025Onwards", "OptOut2026SingleTaxYear")
          )

          details shouldBe expected
        }
      }
    }
  }
}
