package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys.{addBusinessAccountingMethod, addBusinessAccountingPeriodEndDate, addBusinessAddressLine1, addBusinessPostalCode, businessName, businessStartDate, businessTrade}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.addIncomeSource.AddIncomeSourceResponse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndUkProperty, noPropertyOrBusinessResponse}

class ForeignPropertyCheckDetailsControllerISpec extends ComponentSpecBase{

  val foreignPropertyCheckDetailsShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.show().url
  val foreignPropertyAccountingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show().url

  val foreignPropertyCheckDetailsSubmitUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submit().url
  val foreignPropertyReportingMethodShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show("123").url

  s"calling GET $foreignPropertyCheckDetailsShowUrl" should {
    "render the FP check details page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val response = List(AddIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        When(s"I call $foreignPropertyCheckDetailsShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-check-details")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.foreign-property-check-details.title")
        )
      }
    }
  }

}
