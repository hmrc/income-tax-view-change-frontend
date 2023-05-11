package controllers.agent.incomeSources.cease


import config.featureswitch.{FeatureSwitching, IncomeSources}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuthStub, IncomeTaxViewChangeStub}
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponse

class CeaseUKPropertyControllerISpec extends ComponentSpecBase with FeatureSwitching {
  val showDateUKPropertyCeasedControllerUrl = controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.showAgent().url
  val showCeaseUKPropertyControllerUrl = controllers.incomeSources.cease.routes.CeaseUKPropertyController.showAgent().url
  val radioErrorMessage = messagesAPI("incomeSources.ceaseUKProperty.radioError")
  val radioLabelMessage = messagesAPI("incomeSources.ceaseUKProperty.radioLabel")
  val buttonLabel = messagesAPI("base.continue")
  val pageTitleMsgKey = "incomeSources.ceaseUKProperty.heading"

  s"calling GET ${showCeaseUKPropertyControllerUrl}" should {
    "render the Cease UK Property Page" when {
      "user is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = businessAndPropertyResponse
        )
        When(s"I call GET ${showCeaseUKPropertyControllerUrl}")
        val res: WSResponse = IncomeTaxViewChangeFrontend.getCeaseUKProperty(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.ceaseUKProperty.heading"),
          elementTextBySelector("label")(radioLabelMessage),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }

  s"calling POST ${controllers.incomeSources.cease.routes.CeaseUKPropertyController.submit.url}" should {
    "redirect to showDateUKPropertyCeasedControllerUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        AuthStub.stubAuthorisedAgent()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(Some("declaration"), clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showDateUKPropertyCeasedControllerUrl)
        )
      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        AuthStub.stubAuthorisedAgent()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(None, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-uk-property-declaration-error")(messagesAPI("base.error-prefix") + " " + radioErrorMessage)
        )
      }
    }
  }


}
