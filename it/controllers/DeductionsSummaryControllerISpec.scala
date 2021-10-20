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

import java.time.LocalDateTime

import testConstants.BaseIntegrationTestConstants._
import testConstants.CalcDataIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.{DeductionsSummaryMessages => messages}
import audit.models.AllowanceAndDeductionsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.TxmEventsApproved
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.servicemocks._
import models.calculation.{Calculation, CalculationItem, ListCalculationItems}
import play.api.http.Status._
import play.api.test.FakeRequest

class DeductionsSummaryControllerISpec extends ComponentSpecBase {

  "Calling the DeductionsSummaryController.showDeductionsSummary(taxYear)" when {

    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response, " should {
      "return the correct income summary page" in {

        And("I wiremock stub a successful Deductions Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

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
        enable(TxmEventsApproved)
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

        val testUser: MtdItUser[_] = MtdItUser(
          testMtditid, testNino, userName = None, multipleBusinessesAndPropertyResponse,
          Some("1234567890"), Some("12345-credId"), Some(testUserTypeIndividual), arn = None
        )(FakeRequest())

        And("Audit TXM events have been fired with TxmApproved FS true")
        val expectedAllowancesAndDeductionsTrue = estimatedCalculationFullJson.as[Calculation].allowancesAndDeductions
        verifyAuditEvent(AllowanceAndDeductionsResponseAuditModel(testUser, expectedAllowancesAndDeductionsTrue, true))


        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/income")
        disable(TxmEventsApproved)

        val res2 = IncomeTaxViewChangeFrontend.getDeductionsSummary(testYear)

        Then("I see Allowances and deductions page")
        res2 should have(
          httpStatus(OK),
          pageTitle(messages.deductionsSummaryTitle),
        )

        And("Audit TXM events have been fired with TxmApproved FS false")
        val expectedAllowancesAndDeductionsFalse = estimatedCalculationFullJson.as[Calculation].allowancesAndDeductions.copy(giftOfInvestmentsAndPropertyToCharity = None)
        verifyAuditEvent(AllowanceAndDeductionsResponseAuditModel(testUser, expectedAllowancesAndDeductionsFalse, false))
      }
    }

    unauthorisedTest("/calculation/" + testYear + "/deductions")

  }
}
