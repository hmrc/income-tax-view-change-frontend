package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino, testPropertyIncomeId}
import testConstants.IncomeSourceIntegrationTestConstants.ukPropertyOnlyResponse
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel
import testConstants.PropertyDetailsIntegrationTestConstants.ukProperty


class UKPropertyAddedControllerISpec extends ComponentSpecBase {
  val UKPropertyAddedControllerShowUrl: String = controllers.incomeSources.add.routes.UKPropertyAddedController.showAgent(testPropertyIncomeId).url
  val HomeControllerShowUrl: String = controllers.routes.HomeController.showAgent.url
  val pageTitle: String = messagesAPI("htmlTitle.agent", {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}".trim()
  })
  val confirmationPanelContent: String = {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}"
  }

  s"calling GET $UKPropertyAddedControllerShowUrl" should {
    "render the UK Property Added Page" when {
      "UK Property start date is provided" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates return a success response")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

        Then("user is shown UK property added page")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added?id=$testPropertyIncomeId", clientDetailsWithConfirmation)
        result should have(
          httpStatus(OK),
          pageTitleCustom(pageTitle),
          elementTextBySelectorList(".govuk-panel.govuk-panel--confirmation")(confirmationPanelContent)
        )

      }
    }
    "render error page" when {
      "UK property income source is missing trading start date" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))


        Then("user is shown a error page")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added?id=$testPropertyIncomeId", clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleAgent("standardError.heading", isErrorPage = true)
        )

      }
    }
    s"redirect to $HomeControllerShowUrl" when {
      "Income Sources Feature Switch is disabled" in {
        Given("Income Sources FS is disabled")
        disable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        And("API 1525 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))


        Then(s"user is redirected to $HomeControllerShowUrl")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added?id=$testPropertyIncomeId", clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(HomeControllerShowUrl)
        )

      }
    }
  }
}
