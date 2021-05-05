/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import assets.BaseIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.messages.{DeductionsSummaryMessages => messages}
import audit.models.{AllowanceAndDeductionsRequestAuditModel, AllowanceAndDeductionsResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.DeductionBreakdown
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.servicemocks._
import models.calculation.{Calculation, CalculationItem, ListCalculationItems}
import play.api.http.Status._
import play.api.test.FakeRequest

import java.time.LocalDateTime

class DeductionsSummaryControllerISpec extends ComponentSpecBase {

  "Calling the DeductionsSummaryController.showDeductionsSummary(taxYear)" when {

    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response, " +
      "feature switch DeductionsBreakdown is enabled" should {
      enable(DeductionBreakdown)
      "return the correct income summary page" in {

        And("I wiremock stub a successful Deductions Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I stub a successful calculation response for 2017-18")
        IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/income")
        val res = IncomeTaxViewChangeFrontend.getDeductionsSummary(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        Then("I see Allowances and deductions page")
        res should have(
          httpStatus(OK),
          pageTitle(messages.deductionsSummaryTitle),
          elementTextBySelector("h1")(messages.deductionsSummaryHeading)
        )

        And("Audit TXM events have been fired")
        val testUser: MtdItUser[_] = MtdItUser(
          testMtditid, testNino, userName = None, multipleBusinessesAndPropertyResponse,
          Some("1234567890"), Some("12345-credId"), Some(testUserTypeIndividual), arn = None
        )(FakeRequest())

        val expectedAllowancesAndDeductions = estimatedCalculationFullJson.as[Calculation].allowancesAndDeductions
        verifyAuditEvent(AllowanceAndDeductionsRequestAuditModel(testUser))
        verifyAuditEvent(AllowanceAndDeductionsResponseAuditModel(testUser, expectedAllowancesAndDeductions))
      }
    }

    unauthorisedTest("/calculation/" + testYear + "/deductions")

  }
}
