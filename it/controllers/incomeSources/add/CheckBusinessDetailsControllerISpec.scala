package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys.{addBusinessAccountingMethod, addBusinessAddressLine1, addBusinessPostCode, businessName, businessStartDate, businessTrade}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class CheckBusinessDetailsControllerISpec extends ComponentSpecBase{

  val checkBusinessDetailsShowUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url

  val dateCookie: Map[String, String] = Map(businessName -> "Test Business",
                                            businessStartDate -> "2022-01-01",
                                            businessTrade -> "Plumbing",
                                            addBusinessAddressLine1 -> "Test Road",
                                            addBusinessPostCode -> "B32 1PQ",
                                            addBusinessAccountingMethod -> "Quarterly")
  val businessName: String = "Test Business"
  val continueButtonText: String = messagesAPI("check-business-details.confirm-button")


  s"calling GET $checkBusinessDetailsShowUrl" should {
    "render the Check Business details page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $checkBusinessDetailsShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/business-check-details", dateCookie)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("check-business-details.heading"),
          elementTextByID("business-name-value")(businessName),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
  }
 }
