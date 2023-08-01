package controllers.incomeSources.manage

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel

class ManageObligationsControllerISpec extends ComponentSpecBase{

  val annual = "annual"
  val quarterly = "quarterly"
  val taxYear = "2023-2024"
  val incomeSourceId = "XAIS00000000001"

  val manageSEObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showSelfEmployment(annual, taxYear, incomeSourceId).url
  val manageUKObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showUKProperty(annual, taxYear).url
  val manageFPObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showForeignProperty(annual, taxYear).url

  val manageConfirmShowUrl: String = controllers.incomeSources.manage.routes.ManageConfirmController.show().url

  val manageObligationsSubmitUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.submit().url
  val manageIncomeSourcesShowUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show().url

  s"calling GET $manageSEObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $manageSEObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val incomeSourceId = testSelfEmploymentId
        val result = IncomeTaxViewChangeFrontend.getAddBusinessObligations(incomeSourceId)
        verifyIncomeSourceDetailsCall(testMtditid)

        //val expectedText: String = ???
      }
    }
  }
}
