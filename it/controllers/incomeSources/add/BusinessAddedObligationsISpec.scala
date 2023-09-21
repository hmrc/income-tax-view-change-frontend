package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse
import testConstants.BusinessDetailsIntegrationTestConstants.business1

import java.time.LocalDate

class BusinessAddedObligationsISpec extends ComponentSpecBase {

  val businessAddedObligationsShowUrl: String = controllers.incomeSources.add.routes.BusinessAddedObligationsController.show().url
  val businessReportingMethodUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.show().url

  val businessAddedObligationsSubmitUrl: String = controllers.incomeSources.add.routes.BusinessAddedObligationsController.submit().url
  val addIncomeSourceUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url

  val testDate: String = "2020-11-10"
  val prefix: String = "business-added"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023,1,1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel("123", List(NextUpdateModel(day, day.plusDays(1), day.plusDays(2),"EOPS", None, "EOPS")))))


  s"calling GET $businessAddedObligationsShowUrl" should {
    "render the Business Added page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $businessAddedObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val sessionIncomeSourceId = Map(forms.utils.SessionKeys.incomeSourceId -> testSelfEmploymentId)
        val result = IncomeTaxViewChangeFrontend.getAddBusinessObligations(sessionIncomeSourceId)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI("business-added.sole-trader.head").nonEmpty) {
            messagesAPI("business-added.sole-trader.head") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
          }
          else {
            business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
          }

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $businessAddedObligationsSubmitUrl" should {
    s"redirect to $addIncomeSourceUrl" when {
      "called" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postAddedBusinessObligations()
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/income-sources/add/new-income-sources")
        )
      }
    }
  }
}
