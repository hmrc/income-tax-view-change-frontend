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
import enums.IncomeSourceJourney.SelfEmployment
import forms.incomeSources.add.BusinessNameForm
import forms.utils.SessionKeys.{businessName, incomeSourceId}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER, isRedirect}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class AddBusinessNameControllerISpec extends ComponentSpecBase {

  val addBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.show().url
  val addBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submit().url
  val changeBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.changeBusinessName().url
  val changeBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitChange().url
  val addBusinessStartDateUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val checkBusinessDetailsUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  val addIncomeSourceUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  val incomeSourcesUrl: String = controllers.routes.HomeController.show().url

  val addBusinessNameShowAgentUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.showAgent().url
  val addBusinessNameSubmitAgentUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitAgent().url
  val changeBusinessNameShowAgentUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.changeBusinessNameAgent().url
  val changeBusinessNameSubmitAgentUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitChangeAgent().url
  val addBusinessStartDateAgentUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url
  val checkBusinessDetailsAgentUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url
  val addIncomeSourceAgentUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
  val incomeSourcesAgentUrl: String = controllers.routes.HomeController.showAgent.url

  def getShowUrl(isAgent: Boolean): String = if (isAgent) addBusinessNameShowAgentUrl else addBusinessNameShowUrl

  def getSubmitUrl(isAgent: Boolean): String = if (isAgent) addBusinessNameSubmitAgentUrl else addBusinessNameSubmitUrl

  def getShowChangeUrl(isAgent: Boolean): String = if (isAgent) changeBusinessNameShowAgentUrl else changeBusinessNameShowUrl

  def getSubmitChangeUrl(isAgent: Boolean): String = if (isAgent) changeBusinessNameSubmitAgentUrl else changeBusinessNameSubmitUrl

  def getStartDateUrl(isAgent: Boolean): String = if (isAgent) addBusinessStartDateAgentUrl else addBusinessStartDateUrl

  def getCheckDetailsUrl(isAgent: Boolean): String = if (isAgent) checkBusinessDetailsAgentUrl else checkBusinessDetailsUrl

  def getAddIncomeSourceUrl(isAgent: Boolean): String = if (isAgent) addIncomeSourceAgentUrl else addIncomeSourceUrl

  def getIncomeSourcesUrl(isAgent: Boolean): String = if (isAgent) incomeSourcesAgentUrl else incomeSourcesUrl


  val prefix: String = "add-business-name"
  val htmlTitle = messagesAPI("htmlTitle")
  val htmlTitleAgent = messagesAPI("htmlTitle.agent")
  val dateCookie: Map[String, String] = Map(businessName -> "Test Business")
  val formHint: String = messagesAPI("add-business-name.p1") + " " +
    messagesAPI("add-business-name.p2")
  val continueButtonText: String = messagesAPI("base.continue")


  s"calling GET $addBusinessNameShowUrl and $addBusinessNameShowAgentUrl" should {
    "render the Add Business Name page" when {
      def renderPageTest(isAgent: Boolean): Unit = {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        if (isAgent) stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        When(s"I call GET ${getShowUrl(isAgent)}")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessName(isAgent, {
          if (isAgent) clientDetailsWithConfirmation else Map.empty
        })

        result should have(
          httpStatus(OK),
          if (isAgent) pageTitleAgent("add-business-name.heading") else pageTitleIndividual("add-business-name.heading"),
          elementTextByID("business-name-hint > p")(formHint),
          elementTextByID("continue-button")(continueButtonText)
        )
      }

      "User is authorised" in {
        renderPageTest(false)
      }
      "Agent is authorised" in {
        renderPageTest(true)
      }
    }

    "303 SEE_OTHER - redirect to home page" when {
      def fsDisabledTest(isAgent: Boolean): Unit = {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        if (isAgent) stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${getShowUrl(isAgent)}")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessName(isAgent, {
          if (isAgent) clientDetailsWithConfirmation else Map.empty
        })

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(getIncomeSourcesUrl(isAgent))
        )
      }

      "Income Sources FS disabled for individual" in {
        fsDisabledTest(false)
      }
      "IncomeSources FS disabled for agent" in {
        fsDisabledTest(true)
      }
    }
  }

  s"calling POST $addBusinessNameSubmitUrl and $addBusinessNameSubmitAgentUrl" should {
    s"303 SEE_OTHER and redirect to $addBusinessStartDateUrl and $addBusinessStartDateAgentUrl" when {
      def successPostTest(isAgent: Boolean): Unit = {
        enable(IncomeSources)
        if (isAgent) stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("Test Business")
          )
        }

        When(s"I call POST ${getSubmitUrl(isAgent)}")
        val result = IncomeTaxViewChangeFrontend.newPost(isAgent, "/income-sources/add/business-name", {
          if (isAgent) clientDetailsWithConfirmation else Map.empty
        })(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(getStartDateUrl(isAgent))
        )
      }

      "Individual is authorised and business name is valid" in {
        successPostTest(false)
      }
      "Agent is authorised and business name is valid" in {
        successPostTest(true)
      }
    }
    "show error" when {
      def formFillErrorTest(isAgent: Boolean) = {
        enable(IncomeSources)
        if (isAgent) stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("£££")
          )
        }

        val result = IncomeTaxViewChangeFrontend.newPost(isAgent, "/income-sources/add/business-name", {
          if (isAgent) clientDetailsWithConfirmation else Map.empty
        })(formData)
        result should have(
          httpStatus(OK),
          elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("add-business-name.form.error.invalidNameFormat"))
        )
      }

      "individual has incorrectly filled form" in {
        formFillErrorTest(false)
      }
      "agent has incorrectly filled form" in {
        formFillErrorTest(true)
      }
    }
  }

  s"calling GET $changeBusinessNameShowUrl" should {
    "render the Add Business Name page" when {
      def changeShowTest(isAgent: Boolean) = {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        if (isAgent) stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${getShowChangeUrl(isAgent)}")
        val result = IncomeTaxViewChangeFrontend.newGet(isAgent, "/income-sources/add/change-business-name", {
          if (isAgent) clientDetailsWithConfirmation else Map.empty
        })

        result should have(
          httpStatus(OK),
          if (isAgent) pageTitleAgent("add-business-name.heading") else pageTitleIndividual("add-business-name.heading"),
          elementTextByID("business-name-hint > p")(formHint),
          elementTextByID("continue-button")(continueButtonText)
        )
      }

      "Individual is authorised" in {
        changeShowTest(false)
      }
      "Agent is authorised" in {
        changeShowTest(true)
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      def changeFSDisabledTest(isAgent: Boolean) = {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        if (isAgent) stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${getShowChangeUrl(isAgent)}")
        val result = IncomeTaxViewChangeFrontend.newGet(isAgent, "/income-sources/add/change-business-name", {
          if (isAgent) clientDetailsWithConfirmation else Map.empty
        })
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(getIncomeSourcesUrl(isAgent))
        )
      }

      "Income Sources FS disabled for individual" in {
        changeFSDisabledTest(false)
      }
      "Income Sources FS disabled for agent" in {
        changeFSDisabledTest(true)
      }
    }
  }

  s"calling POST $changeBusinessNameSubmitUrl & $changeBusinessNameSubmitAgentUrl" should {
    s"303 SEE_OTHER and redirect to $checkBusinessDetailsUrl and $checkBusinessDetailsAgentUrl" when {
      def changeSuccessPostTest(isAgent: Boolean): Unit = {
        enable(IncomeSources)
        if (isAgent) stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("Test Business")
          )
        }

        When(s"I call POST ${getSubmitUrl(isAgent)}")
        val result = IncomeTaxViewChangeFrontend.newPost(isAgent, "/income-sources/add/change-business-name", {
          if (isAgent) clientDetailsWithConfirmation else Map.empty
        })(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(getCheckDetailsUrl(isAgent))
        )
      }

      "Individual is authorised and business name is valid" in {
        changeSuccessPostTest(false)
      }
      "Agent is authorised and business name is valid" in {
        changeSuccessPostTest(true)
      }
    }
    "show error" when {
      def changeFormFillErrorTest(isAgent: Boolean) = {
        enable(IncomeSources)
        if (isAgent) stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("£££")
          )
        }

        val result = IncomeTaxViewChangeFrontend.newPost(isAgent, "/income-sources/add/change-business-name", {
          if (isAgent) clientDetailsWithConfirmation else Map.empty
        })(formData)
        result should have(
          httpStatus(OK),
          elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("add-business-name.form.error.invalidNameFormat"))
        )
      }

      "individual has incorrectly filled form" in {
        changeFormFillErrorTest(false)
      }
      "agent has incorrectly filled form" in {
        changeFormFillErrorTest(true)
      }
    }

  }
}

