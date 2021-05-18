
package controllers.agent

import config.featureswitch.{AgentViewer, FeatureSwitching}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import play.api.http.Status._
import play.api.libs.ws.WSResponse

class ConfirmClientUTRControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  val clientDetails = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
		SessionKeys.clientMTDID -> "XAIT000000000000"
  )

  s"GET ${controllers.agent.routes.ConfirmClientUTRController.show().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }

    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
    }

    s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show().url}" when {
      "the client's name and UTR are not in session" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR()

        Then("The enter client's utr page is returned to the user")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show().url)
        )
      }
    }

    s"return $NOT_FOUND" when {
      "the agent viewer feature switch is disabled" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR(clientDetails)

        Then(s"A not found page is returned to the user")
        result should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
        )
      }
    }

    s"return $OK with the confirm client utr page" when {
      "the agent viewer feature switch is enabled" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR(clientDetails)

        Then("The confirm client's utr page is returned to the user")
        result should have(
          httpStatus(OK),
          pageTitle("Confirm your client’s details - Your client’s Income Tax details - GOV.UK")
        )
      }
    }
  }

  s"POST ${controllers.agent.routes.ConfirmClientUTRController.submit().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postConfirmClientUTR()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }

    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postConfirmClientUTR()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
    }

    s"return $NOT_FOUND" when {
      "the agent viewer feature switch is disabled" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postConfirmClientUTR(clientDetails)

        Then(s"A not found page is returned to the user")
        result should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
        )
      }
    }

    s"redirect ($SEE_OTHER) to the next page" when {
      "the agent viewer feature switch is enabled" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postConfirmClientUTR(clientDetails)

        Then("The user is redirected to the next page")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.HomeController.show().url)
        )
      }
    }

  }
}
