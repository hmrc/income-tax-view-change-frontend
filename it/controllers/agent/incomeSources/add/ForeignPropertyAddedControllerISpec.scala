package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testPropertyIncomeId}
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyOnlyResponse}

import java.time.LocalDate

class ForeignPropertyAddedControllerISpec extends ComponentSpecBase {

  val foreignPropertyObligationsShowAgentUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAddedController.showAgent("").url
  val foreignPropertyReportingMethodAgentUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent("").url

  val foreignPropertyAddedObligationsSubmitAgentUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAddedController.submitAgent().url
  val addIncomeSourceAgentUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url

  val testDate: String = "2020-11-10"
  val prefix: String = "business-added"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023,1,1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel("123", List(NextUpdateModel(day, day.plusDays(1), day.plusDays(2),"EOPS", None, "EOPS")))))


  s"calling GET $foreignPropertyObligationsShowAgentUrl" should {
    "render the Foreign Property Added obligations page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $foreignPropertyObligationsShowAgentUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val incomeSourceId = testPropertyIncomeId
        val result = IncomeTaxViewChangeFrontend.getForeignPropertyAddedObligations(incomeSourceId, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("business-added.foreign-property.h1") + " " + messagesAPI("business-added.foreign-property.base")

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $foreignPropertyAddedObligationsSubmitAgentUrl" should {
    s"redirect to $addIncomeSourceAgentUrl" when {
      "called" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postForeignPropertyAddedObligations(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/add/new-income-sources")
        )
      }
    }
  }
}
