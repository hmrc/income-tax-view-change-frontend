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
import assets.BaseIntegrationTestConstants._
import assets.CalcBreakdownIntegrationTestConstants.calculationDataSuccessModel
import assets.CalcDataIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.messages.{TaxDueSummaryMessages => messages}
import audit.models.TaxCalculationDetailsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, TxmEventsApproved}
import enums.Crystallised
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.calculation.{CalcDisplayModel, Calculation, CalculationItem, ListCalculationItems}
import play.api.http.Status._
import play.api.test.FakeRequest


class TaxDueSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None,
    multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  "Calling the TaxDueSummaryController.showTaxDueSummary(taxYear)" when {

    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response, " +
      "return the correct tax due summary page with the TxMEventsApproved FS enabled" in {
        enable(TxmEventsApproved)

        And("I wiremock stub a successful TaxDue Details response with single Business and Property income")
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

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
        val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

      val expectedCalculation = estimatedCalculationFullJson.as[Calculation]
      verifyAuditEvent(TaxCalculationDetailsResponseAuditModel(testUser, CalcDisplayModel("", 1, expectedCalculation, Crystallised), testYearInt))

        res should have(
          httpStatus(OK),
          pageTitle(messages.taxDueSummaryTitle),
          elementTextBySelector("h1")(messages.taxDueSummaryHeading ++ " " + "Tax calculation")
        )
      }
    }

    unauthorisedTest("/calculation/" + testYear + "/tax-due")

}
