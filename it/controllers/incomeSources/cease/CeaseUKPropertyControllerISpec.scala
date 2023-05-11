package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse

class CeaseUKPropertyControllerISpec extends ComponentSpecBase {
  val showDateUKPropertyCeasedControllerUrl = controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.show().url
  val showCeaseUKPropertyControllerUrl = controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url
  val radioErrorMessage = messagesAPI("incomeSources.ceaseUKProperty.radioError")
  val radioLabelMessage = messagesAPI("incomeSources.ceaseUKProperty.radioLabel")
  val buttonLabel = messagesAPI("base.continue")
  val pageTitleMsgKey = "incomeSources.ceaseUKProperty.heading"

  s"calling GET ${showCeaseUKPropertyControllerUrl}" should {
    "render the Cease UK Property Page" when {
      "Agent is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET ${showCeaseUKPropertyControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCeaseUKProperty
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
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
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(Some("declaration"))
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showDateUKPropertyCeasedControllerUrl)
        )
      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(None)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-uk-property-declaration-error")(messagesAPI("base.error-prefix") + " " + radioErrorMessage)
        )
      }
    }
  }

}
