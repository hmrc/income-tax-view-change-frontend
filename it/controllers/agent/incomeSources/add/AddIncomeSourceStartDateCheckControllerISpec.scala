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

package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, clientDetailsWithStartDate, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

class AddIncomeSourceStartDateCheckControllerISpec extends ComponentSpecBase {
  val testDate: String = "2020-11-1"
  val addBusinessStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusinessAgent.url
  val addBusinessTradeShowUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.showAgent().url
  val addBusinessStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent.url
  val addBusinessStartDateCheckSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent.url
  val continueButtonText: String = messagesAPI("base.continue")
  val incomeSourcePrefix: String = "start-date-check"
  val soleTraderBusinessPrefix: String = SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix
  val ukPropertyPrefix: String = UkProperty.addIncomeSourceStartDateCheckMessagesPrefix
  val foreignPropertyPrefix: String = ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix
  val testAddBusinessStartDate: Map[String, String] = Map(SessionKeys.addBusinessStartDate -> "2022-10-10")

  val foreignPropertyStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignPropertyAgent.url
  val foreignPropertyStartDateCheckSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignPropertyAgent.url
  val foreignPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent().url
  val foreignPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent.url

  val testAddForeignPropertyStartDate: Map[String, String] = Map(SessionKeys.foreignPropertyStartDate -> "2022-10-10")

  val testAddUKPropertyStartDate: Map[String, String] = Map(SessionKeys.addUkPropertyStartDate -> "2022-10-10")
  val dateText: String = "10 October 2022"

  val checkUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showUKPropertyAgent.url
  val checkUKPropertyStartDateSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitUKPropertyAgent.url
  val addUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent.url
  val ukPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.showAgent().url

  s"calling GET $addBusinessStartDateCheckShowUrl" should {
    "render the Add Business Start Date Check Page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckShowUrl")

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDateCheck(clientDetailsWithStartDate ++ testAddBusinessStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("dateForm.check.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateCheckSubmitUrl" should {
    s"redirect to $addBusinessTradeShowUrl" when {
      "form response is Yes" in {

        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(Some("Yes"))(clientDetailsWithConfirmation ++ testAddBusinessStartDate)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessTradeShowUrl)
        )
      }
    }
    s"redirect to $addBusinessStartDateShowUrl" when {
      "form response is No" in {

        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(Some("No"))(clientDetailsWithConfirmation ++ testAddBusinessStartDate)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateShowUrl)
        )
      }
    }
    "return a BAD_REQUEST" when {
      "form is empty" in {

        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(None)(clientDetailsWithConfirmation ++ testAddBusinessStartDate)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$incomeSourcePrefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$soleTraderBusinessPrefix.error"))
        )
      }
    }
    "return INTERNAL_SERVER_ERROR" when {
      "impossible entry given" in {

        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheck(Some("@"))(clientDetailsWithConfirmation ++ testAddBusinessStartDate)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
  s"calling GET $foreignPropertyStartDateCheckShowUrl" should {
    "render the foreign property start date check page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyStartDateCheckShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-start-date-check",
          testAddForeignPropertyStartDate ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("dateForm.check.heading"),
          elementTextByID(s"$incomeSourcePrefix-hint")(dateText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyStartDateCheckSubmitUrl" should {
    s"redirect to $foreignPropertyAccountingMethodShowUrl" when {
      "form is filled correctly with input Yes" in {

        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheck(Some("Yes"))(clientDetailsWithConfirmation ++ testAddForeignPropertyStartDate)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyAccountingMethodShowUrl)
        )
      }
      "form is filled correctly with input No" in {

        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheck(Some("No"))(clientDetailsWithConfirmation ++ testAddForeignPropertyStartDate)


        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyStartDateShowUrl)
        )
      }
      "form is filled incorrectly" in {

        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheck(None)(clientDetailsWithConfirmation ++ testAddForeignPropertyStartDate)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$incomeSourcePrefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$foreignPropertyPrefix.error"))
        )
      }
    }
  }
  s"calling GET $checkUKPropertyStartDateShowUrl" should {
    "render the Check UK Property Business Start Date page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $checkUKPropertyStartDateShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-start-date-check",
          testAddUKPropertyStartDate ++ clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("radioForm.checkDate.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $checkUKPropertyStartDateSubmitUrl" should {
    s"redirect to $ukPropertyAccountingMethodShowUrl" when {
      "user selects 'yes' the date entered is correct" in {

        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.postAddUKPropertyStartDateCheck(Some("Yes"))(testAddUKPropertyStartDate ++ clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(ukPropertyAccountingMethodShowUrl)
        )
      }
      s"redirect to $addUKPropertyStartDateShowUrl" when {
        "user selects 'no' the date entered is not correct" in {
          stubAuthorisedAgentUser(authorised = true)
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.postAddUKPropertyStartDateCheck(Some("No"))(testAddUKPropertyStartDate ++ clientDetailsWithConfirmation)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(addUKPropertyStartDateShowUrl)
          )
        }
      }
      s"return BAD_REQUEST $checkUKPropertyStartDateShowUrl" when {
        "user does not select anything" in {

          stubAuthorisedAgentUser(authorised = true)
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.postAddUKPropertyStartDateCheck(Some(""))(testAddUKPropertyStartDate ++ clientDetailsWithConfirmation)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID(s"$incomeSourcePrefix-error")(messagesAPI("base.error-prefix") + " " +
              messagesAPI(s"$ukPropertyPrefix.error"))
          )
        }
      }
    }
  }
}
