package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.SelfEmployment
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino, testSelfEmploymentId}
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, singleBusinessResponse}

import java.time.LocalDate

class BusinessAddedObligationsISpec extends ComponentSpecBase {

  val businessAddedObligationsShowAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent("", SelfEmployment).url
  val businessReportingMethodAgentUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent("").url

  val businessAddedObligationsSubmitAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.agentSubmit().url
  val addIncomeSourceAgentUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url

  val testDate: String = "2020-11-10"
  val prefix: String = "business-added"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023,1,1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel("123", List(NextUpdateModel(day, day.plusDays(1), day.plusDays(2),"EOPS", None, "EOPS")))))


  s"calling GET $businessAddedObligationsShowAgentUrl" should {
    "render the Business Added page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $businessAddedObligationsShowAgentUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val incomeSourceId = testSelfEmploymentId
        val result = IncomeTaxViewChangeFrontend.getAddBusinessObligations(incomeSourceId, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI("business-added.sole-trader.head").nonEmpty) {
          messagesAPI("business-added.sole-trader.head") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
        }
        else {
          business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
        }

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $businessAddedObligationsSubmitAgentUrl" should {
    s"redirect to $addIncomeSourceAgentUrl" when {
      "called" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postAddedBusinessObligations(clientDetailsWithConfirmation)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/add/new-income-sources")
        )
      }
    }
  }
}
