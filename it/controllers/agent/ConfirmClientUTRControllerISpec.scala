
package controllers.agent

import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.titleInternalServer
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.clientDetailsWithoutConfirmation
import testConstants.messages.AgentMessages.confirmClientDetails

class ConfirmClientUTRControllerISpec extends ComponentSpecBase with FeatureSwitching {


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
          pageTitleAgent(titleInternalServer)
        )
      }
    }

    s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show().url}" when {
      "the client's name and UTR are not in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR()

        Then("The enter client's utr page is returned to the user")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show().url)
        )
      }
    }

    s"return $OK with the confirm client utr page" in {
      stubAuthorisedAgentUser(authorised = true)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR(clientDetailsWithoutConfirmation)

      Then("The confirm client's utr page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent(confirmClientDetails)
      )
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
          pageTitleAgent(titleInternalServer)
        )
      }
    }

    s"redirect ($SEE_OTHER) to the next page" in {
      stubAuthorisedAgentUser(authorised = true)

      val result: WSResponse = IncomeTaxViewChangeFrontend.postConfirmClientUTR(clientDetailsWithoutConfirmation)

      Then("The user is redirected to the next page")
      result should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.routes.HomeController.showAgent().url)
      )
    }
  }
}
