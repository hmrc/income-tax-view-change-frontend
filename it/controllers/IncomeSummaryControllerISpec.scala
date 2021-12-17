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

import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.calculation.{CalculationItem, ListCalculationItems}
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalcDataIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.{IncomeSummaryMessages => messages}

class IncomeSummaryControllerISpec extends ComponentSpecBase {

  "Calling the IncomeSummaryController.showIncomeSummary(taxYear)" when {

    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response, " should {
      "return the correct income summary page" in {

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
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
        val res = IncomeTaxViewChangeFrontend.getIncomeSummary(testYear)

        verifyIncomeSourceDetailsCall(testMtditid, 0)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        res should have(
          httpStatus(OK),
          pageTitle(messages.incomeSummaryTitle),
          elementTextBySelector("h1")(messages.incomeSummaryHeading)
        )
      }
    }

    unauthorisedTest("/calculation/" + testYear + "/income")

  }
}
