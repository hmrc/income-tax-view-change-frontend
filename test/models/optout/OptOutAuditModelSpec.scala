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

package models.optout

import audit.models.{OptOutAuditModel, Outcome}
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import models.incomeSourceDetails.TaxYear
import play.api.http.Status.OK
import services.optout.{OptOutProposition, OptOutTestSupport}
import testUtils.TestSupport

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
        auditModel.currentYear shouldEqual TaxYear(2023, 2024)
        auditModel.auditType shouldEqual "ClientDetailsConfirmed"
        auditModel.outcome shouldEqual expectedOutcome

      }

      "createOutcome builds an appropriate Outcome" in {

        val intentTextYear: TaxYear = TaxYear(22, 23)
        val resolvedResponse: ITSAStatusUpdateResponse = ITSAStatusUpdateResponseFailure.defaultFailure()
        val auditModel: OptOutAuditModel = OptOutAuditModel.generateOptOutAudit(optOutProposition, intentTextYear, resolvedResponse)
        val expectedOutcome: Outcome = Outcome(isSuccessful = false, Some("API_FAILURE"), Some("Failure reasons"))

        auditModel.outcome shouldEqual expectedOutcome

      }

    }
  }
}