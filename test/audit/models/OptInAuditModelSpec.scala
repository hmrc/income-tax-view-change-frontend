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

import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.Annual
import play.api.http.Status.OK
import play.api.libs.json.Json
import services.reportingObligations.signUp.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import testUtils.TestSupport

class OptInAuditModelSpec extends TestSupport {

  val currentYear: TaxYear = TaxYear.forYearEnd(2024)
  val nextYear: TaxYear = currentYear.nextYear

  val currentTaxYearOptIn: CurrentOptInTaxYear = CurrentOptInTaxYear(Annual, currentYear)
  val nextTaxYearOptIn: NextOptInTaxYear = NextOptInTaxYear(Annual, nextYear, currentTaxYearOptIn)

  val optInProposition: OptInProposition = OptInProposition(
    currentTaxYearOptIn,
    nextTaxYearOptIn
  )

  implicit val user: MtdItUser[_] = tsTestUser

  "OptInAuditModel" when {
    "user opt in quarterly reporting is submitted" should {
      "create an OptInAuditModel that contains all the correct data when successful" in {
        val intentTaxYear = TaxYear(23, 24)
        val resolvedResponse: ITSAStatusUpdateResponse = ITSAStatusUpdateResponseSuccess(OK)
        val auditModel = OptInAuditModel(optInProposition, intentTaxYear, resolvedResponse)

        auditModel.auditType shouldBe "OptInQuarterlyReportingRequest"
        auditModel.transactionName shouldBe "opt-in-quarterly-reporting-request"
       assertJsonEquals(auditModel.detail, Json.obj(
          "nino" -> tsTestUser.nino,
          "mtditid" -> tsTestUser.mtditid,
          "saUtr" -> tsTestUser.saUtr,
          "credId" -> tsTestUser.credId,
          "userType" -> tsTestUser.userType,
          "optInRequestedFromTaxYear" -> "23-24",
          "currentYear" -> "23-24",
          "beforeITSAStatusCurrentYear" -> "Annual",
          "beforeITSAStatusCurrentYear+1" -> "Annual",
          "afterAssumedITSAStatusCurrentYear" -> "Annual",
          "afterAssumedITSAStatusCurrentYear+1" -> "MTD Voluntary",
          "outcome" -> Json.obj(
            "isSuccessful" -> true
          )
       ) )
      }
      "generate a outcome when unsuccessful" in {
        val intentTaxYear = TaxYear(23, 24)
        val resolvedResponse: ITSAStatusUpdateResponse = ITSAStatusUpdateResponseFailure.defaultFailure()
        val auditModel = OptInAuditModel(optInProposition, intentTaxYear, resolvedResponse)

        assertJsonEquals(auditModel.detail, Json.obj(
          "nino" -> tsTestUser.nino,
          "mtditid" -> tsTestUser.mtditid,
          "saUtr" -> tsTestUser.saUtr,
          "credId" -> tsTestUser.credId,
          "userType" -> tsTestUser.userType,
          "optInRequestedFromTaxYear" -> "23-24",
          "currentYear" -> "23-24",
          "beforeITSAStatusCurrentYear" -> "Annual",
          "beforeITSAStatusCurrentYear+1" -> "Annual",
          "afterAssumedITSAStatusCurrentYear" -> "Annual",
          "afterAssumedITSAStatusCurrentYear+1" -> "MTD Voluntary",
          "outcome" -> Json.obj(
            "isSuccessful" -> false,
            "failureCategory" -> "INTERNAL_SERVER_ERROR",
            "failureReason" -> "Request failed due to unknown reason"
          )
        ))
      }
    }
  }
}
