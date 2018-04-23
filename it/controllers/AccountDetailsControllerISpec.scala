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
import assets.IncomeSourceIntegrationTestConstants._
import assets.ReportDeadlinesIntegrationTestConstants.multipleReportDeadlinesDataSuccessModel
import config.FrontendAppConfig
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, GenericStubMethods}
import play.api.http.Status.{OK, SEE_OTHER}

class AccountDetailsControllerISpec extends ComponentSpecBase with GenericStubMethods {

  lazy val appConfig = app.injector.instanceOf[FrontendAppConfig]

  "Calling the AccountDetailsController.getAccountDetails" when {

    "The Account Details Feature is Enabled" when {

      "isAuthorisedUser with an active enrolment and has at least 1 business and property" should {

        "return the correct page with a valid total" in {

          And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/account-details")
          val res = IncomeTaxViewChangeFrontend.getAccountDetails

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle("Account details"),
            elementTextByID(id = "page-heading")("Account details"),
            elementTextByID(id = "your-businesses")("Your businesses"),
            elementTextByID(id = "business-link-1")("business"),
            elementTextByID(id = "your-properties")("Your properties"),
            elementTextByID(id = "reporting-period")("Reporting period: 6 April - 5 April")
          )
        }
      }

      unauthorisedTest("/account-details")
    }
  }

  "The Account Details Feature is Disabled" when {

    "Authorised" should {

      "Redirect to Home Page" in {

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
