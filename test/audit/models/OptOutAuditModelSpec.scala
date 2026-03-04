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
import models.itsaStatus.ITSAStatus.{Annual, NoStatus, Voluntary}
import play.api.http.Status.OK
import play.api.libs.json.Json
import services.reportingObligations.optOut.OptOutProposition
import services.reportingObligations.optOut.OptOutTestSupport
import testConstants.BaseTestConstants.testNino
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class OptOutAuditModelSpec extends TestSupport {

  val optOutProposition: OptOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()

  implicit val user: MtdItUser[_] = tsTestUser

  "OptOutAuditModelSpec.generateOptOutAudit" when {

    "user opt out of quarterly reporting is submitted" should {

      "generated OptOutAuditModel should contain all the correct data" in {

        val intentTextYear: TaxYear = TaxYear(22, 23)
        val resolvedResponse: ITSAStatusUpdateResponse = ITSAStatusUpdateResponseSuccess(OK)
        val auditModel: OptOutAuditModel = OptOutAuditModel.generateOptOutAudit(optOutProposition, intentTextYear, resolvedResponse)
        val expectedOutcome: Outcome = Outcome(isSuccessful = true, None, None)

        auditModel.nino shouldEqual user.nino
        auditModel.currentYear shouldEqual TaxYear(2023, 2024).formatAsShortYearRange
        auditModel.auditType shouldEqual "OptOutQuarterlyReportingRequest"
        auditModel.transactionName shouldEqual "opt-out-quarterly-reporting-request"
        auditModel.outcome shouldEqual expectedOutcome

      }

      "createOutcome builds an appropriate Outcome" in {

        val intentTextYear: TaxYear = TaxYear(22, 23)
        val resolvedResponse: ITSAStatusUpdateResponse = ITSAStatusUpdateResponseFailure.defaultFailure()
        val auditModel: OptOutAuditModel = OptOutAuditModel.generateOptOutAudit(optOutProposition, intentTextYear, resolvedResponse)
        val expectedOutcome: Outcome = Outcome(isSuccessful = false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason"))

        auditModel.outcome shouldEqual expectedOutcome
      }
    }
  }

  "OptOutAuditModelSpec" when {

    "given OptOutAuditModel" should {

      "write correctly to json" in {

        val taxYear: TaxYear = TaxYear(22, 23)

        val expectedOutcome: Outcome = Outcome(isSuccessful = true, None, None)

        val user = tsTestUser

        val auditModel: OptOutAuditModel =
          OptOutAuditModel(
            saUtr = user.saUtr,
            credId = user.credId,
            userType = user.userType,
            agentReferenceNumber = user.arn,
            mtditid = user.mtditid,
            nino = testNino,
            outcome = expectedOutcome,
            optOutRequestedFromTaxYear = taxYear.previousYear.formatAsShortYearRange,
            currentYear = taxYear.formatAsShortYearRange,
            `beforeITSAStatusCurrentYear-1` = Voluntary,
            beforeITSAStatusCurrentYear = NoStatus,
            `beforeITSAStatusCurrentYear+1` = NoStatus,
            `afterAssumedITSAStatusCurrentYear-1` = Annual,
            afterAssumedITSAStatusCurrentYear = NoStatus,
            `afterAssumedITSAStatusCurrentYear+1` = Annual,
            `currentYear-1Crystallised` = false
          )

        val actual = Json.toJson(auditModel)

        val expectedJson = commonAuditDetails(Individual) ++
          Json.obj(
            "outcome" -> Json.obj("isSuccessful" -> true),
            "optOutRequestedFromTaxYear" -> "21-22",
            "currentYear" -> "22-23",
            "beforeITSAStatusCurrentYear-1" -> "MTD Voluntary",
            "beforeITSAStatusCurrentYear" -> "No Status",
            "beforeITSAStatusCurrentYear+1" -> "No Status",
            "afterAssumedITSAStatusCurrentYear-1" -> "Annual",
            "afterAssumedITSAStatusCurrentYear" -> "No Status",
            "afterAssumedITSAStatusCurrentYear+1" -> "Annual",
            "currentYear-1Crystallised" -> false
          )

        actual shouldBe expectedJson
      }

      "read correctly from json" in {

        val taxYear: TaxYear = TaxYear(22, 23)

        val expectedOutcome: Outcome = Outcome(isSuccessful = true, None, None)

        val actual =
          {commonAuditDetails(Individual) ++ Json.obj(
            "outcome" -> Json.obj("isSuccessful" -> true),
            "optOutRequestedFromTaxYear" -> "21-22",
            "currentYear" -> "22-23",
            "beforeITSAStatusCurrentYear-1" -> "MTD Voluntary",
            "beforeITSAStatusCurrentYear" -> "No Status",
            "beforeITSAStatusCurrentYear+1" -> "No Status",
            "afterAssumedITSAStatusCurrentYear-1" -> "Annual",
            "afterAssumedITSAStatusCurrentYear" -> "No Status",
            "afterAssumedITSAStatusCurrentYear+1" -> "Annual",
            "currentYear-1Crystallised" -> false
          )}.as[OptOutAuditModel]

        val expectedModel =
          OptOutAuditModel(
            saUtr = user.saUtr,
            credId = user.credId,
            userType = user.userType,
            agentReferenceNumber = user.arn,
            mtditid = user.mtditid,
            nino = testNino,
            outcome = expectedOutcome,
            optOutRequestedFromTaxYear = taxYear.previousYear.formatAsShortYearRange,
            currentYear = taxYear.formatAsShortYearRange,
            `beforeITSAStatusCurrentYear-1` = Voluntary,
            beforeITSAStatusCurrentYear = NoStatus,
            `beforeITSAStatusCurrentYear+1` = NoStatus,
            `afterAssumedITSAStatusCurrentYear-1` = Annual,
            afterAssumedITSAStatusCurrentYear = NoStatus,
            `afterAssumedITSAStatusCurrentYear+1` = Annual,
            `currentYear-1Crystallised` = false
          )

        actual shouldBe expectedModel
      }
    }
  }
}