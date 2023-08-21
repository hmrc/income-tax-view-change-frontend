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
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, noPropertyOrBusinessResponse}

class AddIncomeSourceStartDateControllerISpec extends ComponentSpecBase {
  val addBusinessStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness.url
  val addBusinessStartDateSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness.url
  val addBusinessStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusiness.url
  val prefix: String = "add-business-start-date"
  val continueButtonText: String = messagesAPI("base.continue")

  val addUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url
  val addUKPropertyStartDateSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKProperty.url
  val checkUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showUKProperty.url

  val hintTextUkProperty: String = messagesAPI("incomeSources.add.UKPropertyStartDate.hint") + " " +
    messagesAPI("dateForm.hint")

  val foreignPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty.url
  val foreignPropertyStartDateSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignProperty.url
  val foreignPropertyStartDateCheckUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignProperty.url

  val hintTextForeignProperty: String = messagesAPI("incomeSources.add.foreignProperty.startDate.hint") + " " +
    messagesAPI("incomeSources.add.foreignProperty.startDate.hintExample")

  s"calling GET $addBusinessStartDateShowUrl" should {
    "render the Add Business Start Date Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $addBusinessStartDateShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(s"$prefix.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateSubmitUrl" should {
    s"redirect to $addBusinessStartDateCheckShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map(
            "income-source-start-date.day" -> Seq("1"),
            "income-source-start-date.month" -> Seq("1"),
            "income-source-start-date.year" -> Seq("2022")
          )
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-start-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateCheckShowUrl)
        )
      }
    }
    s"return a BAD_REQUEST" when {
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map(
            "income-source-start-date.day" -> Seq("$"),
            "income-source-start-date.month" -> Seq("%"),
            "income-source-start-date.year" -> Seq("&")
          )
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-start-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-start-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$prefix.error.invalid"))
        )
      }
    }
  }
  s"calling GET $addUKPropertyStartDateShowUrl" should {
    "render the Add UK Property Business Start Date" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addUKPropertyStartDateShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-start-date")
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.UKPropertyStartDate.heading"),
          elementTextByID("income-source-start-date-hint")(hintTextUkProperty),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addUKPropertyStartDateSubmitUrl" should {
    s"redirect to $checkUKPropertyStartDateShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-start-date.day" -> Seq("1"), "income-source-start-date.month" -> Seq("1"),
            "income-source-start-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-start-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkUKPropertyStartDateShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-start-date.day" -> Seq("aa"), "income-source-start-date.month" -> Seq("02"),
            "income-source-start-date.year" -> Seq("2023"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-start-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-start-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.add.UKPropertyStartDate.error.invalid"))
        )
      }
    }
  }
  s"calling GET $foreignPropertyStartDateShowUrl" should {
    "render the foreign property start date page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyStartDateShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-start-date")
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.foreignProperty.startDate.heading"),
          elementTextByID("income-source-start-date-hint")(hintTextForeignProperty),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyStartDateSubmitUrl" should {
    s"redirect to $foreignPropertyStartDateCheckUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-start-date.day" -> Seq("1"), "income-source-start-date.month" -> Seq("1"),
            "income-source-start-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyStartDateCheckUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-start-date.day" -> Seq("aa"), "income-source-start-date.month" -> Seq("02"),
            "income-source-start-date.year" -> Seq("2023"))
        }

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date")(formData)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-start-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.add.foreignProperty.startDate.error.invalid"))
        )
      }
    }
  }
}
