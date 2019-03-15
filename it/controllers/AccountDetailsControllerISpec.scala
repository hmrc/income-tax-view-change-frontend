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

import assets.BaseIntegrationTestConstants.{testMtditid, testPropertyIncomeId, testSelfEmploymentId}
import assets.BusinessDetailsIntegrationTestConstants.b1TradingName
import assets.IncomeSourceIntegrationTestConstants._
import assets.PropertyDetailsIntegrationTestConstants._
import assets.ReportDeadlinesIntegrationTestConstants.multipleReportDeadlinesDataSuccessModel
import assets.messages.{AccountDetailsMessages => messages}
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.ComponentSpecBase

import config.FrontendAppConfig
import play.api.http.Status.{OK, SEE_OTHER}
import implicits.ImplicitDateFormatter._

class AccountDetailsControllerISpec extends ComponentSpecBase {

  lazy val appConfig = app.injector.instanceOf[FrontendAppConfig]

  "Calling the AccountDetailsController.getAccountDetails" when {

    "The Account Details Feature is Enabled" when {

      "isAuthorisedUser with an active enrolment and has at least 1 business and property" should {

        "return the correct page with a valid total" in {

          And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          When("I call GET /report-quarterly/income-and-expenses/view/account-details")
          val res = IncomeTaxViewChangeFrontend.getAccountDetails

          verifyIncomeSourceDetailsCall(testMtditid)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.accountTitle),
            elementTextByID(id = "page-heading")(messages.accountHeading),
            elementTextByID(id = "your-businesses")(messages.businessHeading),
            elementTextByID(id = "business-link-1")(b1TradingName),
            elementTextByID(id = "your-properties")(messages.propertyHeading),
            elementTextByID(id = "reporting-period")(messages.reportingPeriod(propertyAccountingStart,propertyAccountingEnd))
          )
        }
      }

      unauthorisedTest("/account-details")
    }
  }

  "The Account Details Feature is Disabled" when {

    "Authorised" should {

      "Redirect to Home Page" in {

        When("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        appConfig.features.accountDetailsEnabled(false)

        When(s"I call GET /report-quarterly/income-and-expenses/view/account-details")
        val res = IncomeTaxViewChangeFrontend.getAccountDetails

        Then("I should be redirected to the Home Page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }
    }

    unauthorisedTest("/account-details")
  }
}
