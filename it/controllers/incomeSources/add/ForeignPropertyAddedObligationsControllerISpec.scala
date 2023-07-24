package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testPropertyIncomeId}
import testConstants.IncomeSourceIntegrationTestConstants.foreignPropertyOnlyResponse

import java.time.LocalDate

class ForeignPropertyAddedObligationsControllerISpec extends ComponentSpecBase {

  val foreignPropertyAddedObligationsShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAddedObligationsController.show("").url
  val foreignPropertyReportingMethodShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show("").url

  val foreignPropertyAddedObligationsSubmitUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAddedObligationsController.submit().url
  val addIncomeSourceShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url

  val testDate: String = "2020-11-10"
  val prefix: String = "business-added"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023,1,1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel("123", List(NextUpdateModel(day, day.plusDays(1), day.plusDays(2),"EOPS", None, "EOPS")))))


  s"calling GET $foreignPropertyAddedObligationsShowUrl" should {
    "render the Foreign Property Added obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $foreignPropertyAddedObligationsShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val incomeSourceId = testPropertyIncomeId
        val result = IncomeTaxViewChangeFrontend.getForeignPropertyAddedObligations(incomeSourceId)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("business-added.foreign-property-h1") + " " + messagesAPI("business-added.h2")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $foreignPropertyAddedObligationsSubmitUrl" should {
    s"redirect to $addIncomeSourceShowUrl" when {
      "called" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postForeignPropertyAddedObligations()
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/income-sources/add/new-income-sources")
        )
      }
    }
  }
}
