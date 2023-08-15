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

package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys
import helpers.ComponentSpecBase
import helpers.agent.SessionCookieCrumbler
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.ukPropertyOnlyResponse

class AddBusinessStartDateCheckControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {
  val testDate: String = "2020-11-10"
  val addBusinessStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.submit().url
  val addBusinessTradeShowUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.show().url
  val addBusinessStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness.url
  val addBusinessStartDateCheckSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.submit().url
  val continueButtonText: String = messagesAPI("base.continue")
  val prefix: String = "add-business-start-date-check"
  val csrfToken: String = "csrfToken"
  val testAddBusinessStartDate: Map[String, String] = Map(SessionKeys.addBusinessStartDate -> "2022-10-10")

  s"calling GET $addBusinessStartDateCheckShowUrl" should {
    "render the Add Business Start Date Check Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckShowUrl")

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
          .postAddBusinessStartDateCheck(Some("Yes"))(testAddBusinessStartDate)

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
          .postAddBusinessStartDateCheck(Some("No"))(testAddBusinessStartDate)

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
          .postAddBusinessStartDateCheck(None)(testAddBusinessStartDate)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$prefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$prefix.radio.error"))
        )
      }
    }
    "return INTERNAL_SERVER_ERROR" when {
      "form is filled incorrectly" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheck(Some("@INVALID@"))(testAddBusinessStartDate)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
