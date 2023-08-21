package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys.businessName
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class AddBusinessNameControllerISpec extends ComponentSpecBase {

  val addBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.show().url
  val addBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submit().url
  val changeBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.changeBusinessName().url
  val changeBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitChange().url
  val addBusinessStartDateUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceKey = "SE", isAgent = false, isChange = false).url
  val checkBusinessDetailsUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  val addIncomeSourceUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  val incomeSourcesUrl: String = controllers.routes.HomeController.show().url


  val prefix: String = "add-business-name"
  val htmlTitle = messagesAPI("htmlTitle")
  val dateCookie: Map[String, String] = Map(businessName -> "Test Business")
  val formHint: String = messagesAPI("add-business-name.p1") + " " +
    messagesAPI("add-business-name.p2")
  val continueButtonText: String = messagesAPI("base.continue")


  s"calling GET $addBusinessNameShowUrl" should {
    "render the Add Business Name page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addBusinessNameShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessName

        result should have(
          httpStatus(OK),
          pageTitleIndividual("add-business-name.heading"),
          elementTextByID("addBusinessName-hint > p")(formHint),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${addBusinessNameShowUrl}")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessName

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
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            "addBusinessName" -> Seq("Test Business")
          )
        }

        When(s"I call POST ${addBusinessNameSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-name")(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateUrl)
        )
      }
    }
    "show error when form is filled incorrectly" in {
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          "addBusinessName" -> Seq("£££")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-name")(formData)
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
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $changeBusinessNameShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/change-business-name")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("add-business-name.heading"),
          elementTextByID("addBusinessName-hint > p")(formHint),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${changeBusinessNameShowUrl}")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/change-business-name")
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
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            "addBusinessName" -> Seq("Test Business")
          )
        }

        When(s"I call POST ${changeBusinessNameSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-name")(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkBusinessDetailsUrl)
        )
      }
    }
    "show error when form is filled incorrectly" in {
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          "addBusinessName" -> Seq("£££")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-name")(formData)
      result should have(
        httpStatus(OK),
        elementTextByID("addBusinessName-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-name.form.error.invalidNameFormat"))
      )
    }
  }
}

