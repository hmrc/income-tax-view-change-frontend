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

import assets.BaseIntegrationTestConstants.testSelfEmploymentId
import assets.BusinessDetailsIntegrationTestConstants.businessSuccessResponse
import assets.PropertyDetailsIntegrationTestConstants.propertySuccessResponse
import helpers.{ComponentSpecBase, GenericStubMethods}
import play.api.http.Status.{OK, SEE_OTHER}

class AccountDetailsControllerISpec extends ComponentSpecBase with GenericStubMethods {

  "Calling the AccountDetailsController.getAccountDetails" when {

    "isAuthorisedUser with an active enrolment and has at least 1 business and property" should {

      "return the correct page with a valid total" in {
        isAuthorisedUser(true)
        stubUserDetails()
        getBizDeets(businessSuccessResponse(testSelfEmploymentId))
        getPropDeets(propertySuccessResponse())

        When("I call GET /report-quarterly/income-and-expenses/view/account-details")
        val res = IncomeTaxViewChangeFrontend.getAccountDetails

        verifyBizDeetsCall()
        verifyPropDeetsCall()
        verifyBizObsCall(testSelfEmploymentId)

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
