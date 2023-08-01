package controllers.agent.incomeSources.manage

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSelfEmploymentId}
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessAndPropertyResponse, businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel

class ManageObligationsControllerISpec extends ComponentSpecBase{

  val annual = "annual"
  val quarterly = "quarterly"
  val taxYear = "2023-2024"

  val manageSEObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showAgentSelfEmployment(annual, taxYear, testMtditid).url
  val manageUKObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showAgentUKProperty(annual, taxYear).url
  val manageFPObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showAgentForeignProperty(annual, taxYear).url

  val manageConfirmShowUrl: String = controllers.incomeSources.manage.routes.ManageConfirmController.showAgent().url

  val manageObligationsSubmitUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.agentSubmit().url
  val manageIncomeSourcesShowUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceController.showAgent().url

  val prefix: String = "incomeSources.add.manageObligations"
  val reusedPrefix: String = "business-added"

  val continueButtonText: String = messagesAPI(s"$reusedPrefix.income-sources-button")

  s"calling GET $manageSEObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $manageSEObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getManageSEObligations(annual, taxYear, testSelfEmploymentId, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          business1.tradingName.getOrElse("") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $manageUKObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $manageUKObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getManageUKObligations(annual, taxYear, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + messagesAPI(s"$prefix.uk-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          messagesAPI(s"$prefix.uk-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $manageFPObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $manageFPObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getManageFPObligations(quarterly, taxYear, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + messagesAPI(s"$prefix.foreign-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.quarterly") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          messagesAPI(s"$prefix.foreign-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.quarterly") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $manageObligationsSubmitUrl" should {
    s"redirect to $manageIncomeSourcesShowUrl" when {
      "called" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        val resultSE = IncomeTaxViewChangeFrontend.postManageObligations("business", clientDetailsWithConfirmation)
        resultSE should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/manage/view-and-manage-income-sources")
        )
        val resultUK = IncomeTaxViewChangeFrontend.postManageObligations("uk-property", clientDetailsWithConfirmation)
        resultUK should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/manage/view-and-manage-income-sources")
        )
        val resultFP = IncomeTaxViewChangeFrontend.postManageObligations("foreign-property", clientDetailsWithConfirmation)
        resultFP should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/manage/view-and-manage-income-sources")
        )
      }
    }
  }

}
