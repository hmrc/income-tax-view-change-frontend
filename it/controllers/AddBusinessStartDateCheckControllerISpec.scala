/*
 * Copyright 2023 HM Revenue & Customs
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

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_ACCEPTABLE, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.ukPropertyOnlyResponse

import scala.concurrent.Future

class AddBusinessStartDateCheckControllerISpec extends ComponentSpecBase {
  val addBusinessStartDateCheckShowUrl: String = controllers.routes.AddBusinessStartDateCheckController.submit().url
  val addBusinessTradeShowUrl: String = controllers.routes.AddBusinessTradeController.show().url
  val addBusinessStartDateShowUrl: String = routes.AddBusinessStartDateController.show().url
  val addBusinessStartDateCheckSubmitUrl: String = controllers.routes.AddBusinessStartDateCheckController.submit().url
  val continueButtonText: String = messagesAPI("base.continue")
  val prefix: String = "add-business-start-date-check"
  val csrfToken: String = "csrfToken"

  s"calling GET $addBusinessStartDateCheckShowUrl" should {
    "redirect to the Home Page" when {
      "IncomeSources FS is disabled" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckShowUrl")

        val testDate = "2022-01-01"

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDateCheck(testDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.show().url)
        )
      }
    }
    "render the Add Business Start Date Check Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckShowUrl")

        val testDate = "2022-01-01"

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDateCheck(testDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(s"$prefix.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateCheckSubmitUrl" should {
    s"redirect to $addBusinessTradeShowUrl" when {
      "form response is Yes" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheck(
            answer = Some("Yes"),
            additionalHeader = (SessionKeys.businessStartDate, "2022-10-10")
          )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessTradeShowUrl)
        )
      }
    }
    s"redirect to $addBusinessStartDateShowUrl" when {
      "form response is No" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheck(
            answer = Some("No"),
            additionalHeader = (SessionKeys.businessStartDate, "2022-10-10")
          )

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateShowUrl)
        )
      }
    }
    "return a BAD_REQUEST" when {
      "form is empty" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheck(
            answer = None,
            additionalHeader = (SessionKeys.businessStartDate, "2022-10-10")
          )

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$prefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$prefix.radio.error"))
        )
      }
    }
    "return NOT_ACCEPTABLE" when {
      "form is filled incorrectly" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheck(
            answer = Some("@INVALID@"),
            additionalHeader = (SessionKeys.businessStartDate, "2022-10-10")
          )

        result should have(
          httpStatus(NOT_ACCEPTABLE)
        )
      }
    }
    "return INTERNAL_SERVER_ERROR" when {
      s"headers does not contain ${SessionKeys.businessStartDate}" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheckNoHeader(Some("Yes"))

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
