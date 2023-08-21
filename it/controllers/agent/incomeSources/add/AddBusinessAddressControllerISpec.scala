package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddBusinessAddressControllerISpec extends ComponentSpecBase {

  val changeBusinessAddressShowAgentUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = true).url
  val businessAddressShowAgentUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = false).url

  s"calling GET $businessAddressShowAgentUrl" should {
    "render the add business address page" when {
      "Agent is authorised" in {
        Given("I wiremock stub a successful Income Source Details response")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $businessAddressShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessAddress

        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }

  s"calling GET $changeBusinessAddressShowAgentUrl" should {
    "render the change business address page" when {
      "Agent is authorised" in {
        Given("I wiremock stub a successful Income Source Details response")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $changeBusinessAddressShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getAddChangeBusinessAddress

        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }
}
