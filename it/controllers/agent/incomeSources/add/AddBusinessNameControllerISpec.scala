package controllers.agent.incomeSources.add


import config.featureswitch.IncomeSources
import forms.utils.SessionKeys.businessName
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, clientDetailsWithoutConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, noPropertyOrBusinessResponse}


class AddBusinessNameControllerISpec extends ComponentSpecBase {

  val addBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.showAgent().url
  val addBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitAgent().url
  val changeBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.changeBusinessNameAgent().url
  val changeBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitChangeAgent().url
  val addBusinessStartDateUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent.url
  val checkBusinessDetailsUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url
  val addIncomeSourceUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
  val incomeSourcesUrl: String = controllers.routes.HomeController.showAgent.url

  val prefix: String = "add-business-name"
  val htmlTitle = messagesAPI("htmlTitle.agent")
  val dateCookie: Map[String, String] = Map(businessName -> "Test Business")
  val formHint: String = messagesAPI("add-business-name.p1") + " " +
    messagesAPI("add-business-name.p2")
  val continueButtonText: String = messagesAPI("base.continue")


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
          elementTextByID("addBusinessName-hint > p")(formHint),
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
            "addBusinessName" -> Seq("Test Business")
          )
        }

        When(s"I call POST ${addBusinessNameSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-name", clientDetailsWithConfirmation)(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateUrl)
        )
      }
    }
    "show error when form is filled incorrectly" in {
      stubAuthorisedAgentUser(true)

      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          "addBusinessName" -> Seq("£££")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-name", clientDetailsWithConfirmation)(formData)
      result should have(
        httpStatus(OK),
        elementTextByID("addBusinessName-error")(messagesAPI("base.error-prefix") + " " +
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
          elementTextByID("addBusinessName-hint > p")(formHint),
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
            "addBusinessName" -> Seq("Test Business")
          )
        }

        When(s"I call POST ${changeBusinessNameSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-name", clientDetailsWithConfirmation)(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkBusinessDetailsUrl)
        )
      }
    }
    "show error when form is filled incorrectly" in {
      stubAuthorisedAgentUser(true)
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          "addBusinessName" -> Seq("£££")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-name", clientDetailsWithConfirmation)(formData)
      result should have(
        httpStatus(OK),
        elementTextByID("addBusinessName-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-name.form.error.invalidNameFormat"))
      )
    }
  }
}

