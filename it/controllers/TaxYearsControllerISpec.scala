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

import config.featureswitch.{FeatureSwitching, R7bTxmEvents}
import helpers.ComponentSpecBase
import helpers.servicemocks._
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.MyTaxYearsMessages.taxYearsTitle

class TaxYearsControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "Calling the TaxYearsController.viewTaxYears" when {

    "isAuthorisedUser with an active enrolment and income source has retrieved successfully" when {

      "no firstAccountingPeriodEndDate does not exists for both business and property" should {

        "return 500 Internal Server " in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/tax-years")
          val res = IncomeTaxViewChangeFrontend.getTaxYears

          verifyIncomeSourceDetailsCall(testMtditid)

          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }

      "income sources has firstAccountingPeriodEndDate and hence valid tax years" should {

        "return 200 OK " in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)

          When(s"I call GET /report-quarterly/income-and-expenses/view/tax-years")
          val res = IncomeTaxViewChangeFrontend.getTaxYears

          verifyIncomeSourceDetailsCall(testMtditid)

          Then("The view should have the correct headings and all tax years display")
          res should have(
            httpStatus(OK),
            pageTitleIndividual(taxYearsTitle),
            nElementsWithClass("govuk-summary-list__row")(6),
            elementTextBySelectorList("dl", "div:nth-child(1)", "dt")(
              expectedValue = s"6 April ${getCurrentTaxYearEnd.getYear - 1} to 5 April ${getCurrentTaxYearEnd.getYear}"
            )
          )
        }
      }
    }
  }

  unauthorisedTest("/tax-years")

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getTaxYears)
    }
  }
}
