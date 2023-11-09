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
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessNameForm
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.AddIncomeSourceData.businessNameField
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

import scala.concurrent.ExecutionContext

class AddBusinessNameControllerISpec extends ComponentSpecBase {

  val addBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.showAgent().url
  val addBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitAgent().url
  val changeBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.changeBusinessNameAgent().url
  val changeBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitChangeAgent().url
  val addBusinessStartDateUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url
  val checkBusinessDetailsUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url
  val addIncomeSourceUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
  val incomeSourcesUrl: String = controllers.routes.HomeController.showAgent.url

  val prefix: String = "add-business-name"
  val htmlTitle = messagesAPI("htmlTitle.agent")
  val formHint: String = messagesAPI("add-business-name.p1") + " " +
    messagesAPI("add-business-name.p2")
  val continueButtonText: String = messagesAPI("base.continue")
  val testBusinessName: String = "Test Business"
  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)


  s"calling GET $addBusinessNameShowUrl" should {
    "render the Add Business Name page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addBusinessNameShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessName(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("add-business-name.heading"),
          elementTextByID("business-name-hint > p")(formHint),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        stubAuthorisedAgentUser(true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${addBusinessNameShowUrl}")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessName(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourcesUrl)
        )
      }
    }
  }

  s"calling POST ${addBusinessNameSubmitUrl}" should {
    s"303 SEE_OTHER and redirect to $addBusinessStartDateUrl" when {
      "User is authorised and business name is valid" in {
        stubAuthorisedAgentUser(true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("Test Business")
          )
        }

        When(s"I call POST ${addBusinessNameSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-name", clientDetailsWithConfirmation)(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateUrl)
        )

        sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(Some(testBusinessName))
      }
    }
    "show error when form is filled incorrectly" in {
      stubAuthorisedAgentUser(true)

      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          BusinessNameForm.businessName -> Seq("£££")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-name", clientDetailsWithConfirmation)(formData)

      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-name.form.error.invalidNameFormat"))
      )
    }
  }


  s"calling GET $changeBusinessNameShowUrl" should {
    "render the Add Business Name page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $changeBusinessNameShowUrl")
        val result = IncomeTaxViewChangeFrontend.getChangeBusinessName(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("add-business-name.heading"),
          elementTextByID("business-name-hint > p")(formHint),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        stubAuthorisedAgentUser(true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${changeBusinessNameShowUrl}")
        val result = IncomeTaxViewChangeFrontend.getChangeBusinessName(clientDetailsWithConfirmation)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourcesUrl)
        )
      }
    }
  }

  s"calling POST ${changeBusinessNameSubmitUrl}" should {
    s"303 SEE_OTHER and redirect to $checkBusinessDetailsUrl" when {
      "User is authorised and business name is valid" in {
        stubAuthorisedAgentUser(true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("Test Business")
          )
        }

        When(s"I call POST ${changeBusinessNameSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-name", clientDetailsWithConfirmation)(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkBusinessDetailsUrl)
        )

        sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(Some(testBusinessName))
      }
    }
    "show error when form is filled incorrectly" in {
      stubAuthorisedAgentUser(true)
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          BusinessNameForm.businessName -> Seq("£££")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-name", clientDetailsWithConfirmation)(formData)
      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-name.form.error.invalidNameFormat"))
      )
    }
  }
}
