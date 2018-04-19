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

import assets.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import assets.BusinessDetailsIntegrationTestConstants.b1TradingName
import assets.IncomeSourceIntegrationTestConstants._
import assets.PropertyDetailsIntegrationTestConstants._
import assets.messages.AccountDetailsMessages._
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, GenericStubMethods}
import play.api.http.Status.{OK, SEE_OTHER}
import utils.ImplicitDateFormatter._


/*
 TODO - Put messages in messages file
 TODO - Move unauthorised test to some BaseMethod file
 TODO - Move 'isAuthorisedUser(true)' and 'stubUserDetails()' to ComponentSpecBase & remove 'with GenericStubMethods'
 */

class AccountDetailsControllerISpec extends ComponentSpecBase with GenericStubMethods {

  "Calling the AccountDetailsController.getAccountDetails" when {

    "isAuthorisedUser with an active enrolment and has at least 1 business and property" should {

      "return the correct page with a valid total" in {
        isAuthorisedUser(true)
        stubUserDetails()

        And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        When("I call GET /report-quarterly/income-and-expenses/view/account-details")
        val res = IncomeTaxViewChangeFrontend.getAccountDetails

        Then("I verify the Income Source Details has been successfully wiremocked")
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        verifyReportDeadlinesCall(testSelfEmploymentId)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitle(accountTitle),
          elementTextByID(id = "page-heading")(accountHeading),
          elementTextByID(id = "your-businesses")(businessHeading),
          elementTextByID(id = "business-link-1")(b1TradingName),
          elementTextByID(id = "your-properties")(propertyHeading),
          elementTextByID(id = "reporting-period")(reportingPeriod(propertyAccountingStart,propertyAccountingEnd))
        )
      }
    }

    "unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When("I call GET /report-quarterly/income-and-expenses/view/account-details")
        val res = IncomeTaxViewChangeFrontend.getAccountDetails

        Then("the http response for an unauthorised user is returned")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }
}
