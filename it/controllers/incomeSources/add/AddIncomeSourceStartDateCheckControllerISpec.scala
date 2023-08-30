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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.utils.SessionKeys
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, clientDetailsWithStartDate, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

class AddIncomeSourceStartDateCheckControllerISpec extends ComponentSpecBase {
  val testDate: String = "2020-11-1"
  val addBusinessStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val addBusinessTradeShowUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent = false, isChange = false).url
  val addBusinessStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val addBusinessStartDateCheckSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val continueButtonText: String = messagesAPI("base.continue")
  val incomeSourcePrefix: String = "start-date-check"
  val soleTraderBusinessPrefix: String = SelfEmployment.addStartDateCheckMessagesPrefix
  val ukPropertyPrefix: String = UkProperty.addStartDateCheckMessagesPrefix
  val foreignPropertyPrefix: String = ForeignProperty.addStartDateCheckMessagesPrefix
  val testAddBusinessStartDate: Map[String, String] = Map(SessionKeys.addBusinessStartDate -> "2022-10-10")
  val addBusinessStartDateCheckChangeSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = true).url
  val addBusinessStartDateCheckChangeShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true).url
  val addBusinessStartDateCheckDetailsShowUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url


  val foreignPropertyStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url
  val foreignPropertyStartDateCheckSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url
  val foreignPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(ForeignProperty.key).url
  val foreignPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url
  val addForeignPropertyStartDateCheckChangeShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = true).url
  val addForeignPropertyStartDateCheckChangeSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = true).url
  val addForeignPropertyStartDateCheckDetailsShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.show().url


  val testAddForeignPropertyStartDate: Map[String, String] = Map(SessionKeys.foreignPropertyStartDate -> "2022-10-10")

  val testAddUKPropertyStartDate: Map[String, String] = Map(SessionKeys.addUkPropertyStartDate -> "2022-10-10")
  val dateText: String = "10 October 2022"

  val checkUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false).url
  val checkUKPropertyStartDateSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false).url
  val addUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false).url
  val ukPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(UkProperty.key).url
  val addUKPropertyStartDateCheckChangeSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = true).url
  val addUKPropertyStartDateCheckChangeShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = true).url
  val addUKPropertyStartDateCheckDetailsShowUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show().url


  s"calling GET $addBusinessStartDateCheckShowUrl" should {
    "render the Add Business Start Date Check Page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckShowUrl")

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDateCheck(clientDetailsWithStartDate ++ testAddBusinessStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
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

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(Some("Yes"))(clientDetailsWithConfirmation ++ testAddBusinessStartDate)

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

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(Some("No"))(clientDetailsWithConfirmation ++ testAddBusinessStartDate)

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

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(None)(clientDetailsWithConfirmation ++ testAddBusinessStartDate)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$incomeSourcePrefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$soleTraderBusinessPrefix.error"))
        )
      }
    }
    "return INTERNAL_SERVER_ERROR" when {
      "invalid entry given" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheck(Some("@"))(clientDetailsWithConfirmation ++ testAddBusinessStartDate)

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }
  s"calling GET $foreignPropertyStartDateCheckShowUrl" should {
    "render the foreign property start date check page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyStartDateCheckShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-start-date-check",
          testAddForeignPropertyStartDate ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID(s"$incomeSourcePrefix-hint")(dateText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyStartDateCheckSubmitUrl" should {
    s"redirect to $foreignPropertyAccountingMethodShowUrl" when {
      "form is filled correctly with input Yes" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheck(Some("Yes"))(clientDetailsWithConfirmation ++ testAddForeignPropertyStartDate)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyAccountingMethodShowUrl)
        )
      }
      "form is filled correctly with input No" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheck(Some("No"))(clientDetailsWithConfirmation ++ testAddForeignPropertyStartDate)


        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyStartDateShowUrl)
        )
      }
      "form is filled incorrectly" in {

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

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $checkUKPropertyStartDateShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-start-date-check",
          testAddUKPropertyStartDate ++ clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("radioForm.checkDate.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $checkUKPropertyStartDateSubmitUrl" should {
    s"redirect to $ukPropertyAccountingMethodShowUrl" when {
      "user selects 'yes' the date entered is correct" in {

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
  s"calling GET $addBusinessStartDateCheckChangeShowUrl" should {
    s"render the Start Date Check Change Page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckChangeShowUrl")

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDateCheckChange(testAddBusinessStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling GET $addForeignPropertyStartDateCheckChangeShowUrl" should {
    s"render the Start Date Check Change Page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addForeignPropertyStartDateCheckChangeShowUrl")

        val result = IncomeTaxViewChangeFrontend.getAddForeignPropertyStartDateCheckChange(testAddForeignPropertyStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling GET $addUKPropertyStartDateCheckChangeShowUrl" should {
    s"render the Start Date Check Change Page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addUKPropertyStartDateCheckChangeSubmitUrl")

        val result = IncomeTaxViewChangeFrontend.getAddUKPropertyStartDateCheckChange(testAddUKPropertyStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateCheckChangeSubmitUrl" should {
    s"render the Check Business Details Page" when {
      "User selects 'Yes" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckChangeSubmitUrl")

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheckChange(Some("Yes"))(testAddBusinessStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateCheckDetailsShowUrl)
        )
      }
    }
  }
  s"calling POST $addForeignPropertyStartDateCheckChangeSubmitUrl" should {
    s"render the Check Foreign Property Details Page" when {
      "User selects 'Yes" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        When(s"I call GET $addForeignPropertyStartDateCheckChangeSubmitUrl")

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheckChange(Some("Yes"))(testAddForeignPropertyStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addForeignPropertyStartDateCheckDetailsShowUrl)
        )
      }
    }
  }
  s"calling POST $addUKPropertyStartDateCheckChangeSubmitUrl" should {
    s"render the Check UK Property Details Page" when {
      "User selects 'Yes" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addUKPropertyStartDateCheckChangeSubmitUrl")

        val result = IncomeTaxViewChangeFrontend.postAddUKPropertyStartDateCheckChange(Some("Yes"))(testAddUKPropertyStartDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addUKPropertyStartDateCheckDetailsShowUrl)
        )
      }
    }
  }
}
