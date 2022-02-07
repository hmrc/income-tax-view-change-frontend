
package controllers.agent

import config.featureswitch.FeatureSwitching
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.{titleInternalServer, titleThereIsAProblem}
import play.api.http.Status._
import play.api.libs.ws.WSResponse

class UTRErrorControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val clientUTR = Map(
    SessionKeys.clientUTR -> "1234567890"
  )

  s"GET ${controllers.agent.routes.UTRErrorController.show().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getUTRError()

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

        val result: WSResponse = IncomeTaxViewChangeFrontend.getUTRError()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleInternalServer)
        )
      }
    }

    s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show().url}" when {
      "the client's UTR is not in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getUTRError()

        Then("The enter client's utr page is returned to the user")
        result should have (
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show().url)
        )
      }
    }

    s"return $OK with the UTR Error page" when {
      "the client's UTR is in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getUTRError(clientUTR)

        Then("The UTR Error page is returned to the user")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleThereIsAProblem)
        )
      }
    }
  }

  s"POST ${controllers.agent.routes.UTRErrorController.submit().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postUTRError

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

        val result: WSResponse = IncomeTaxViewChangeFrontend.postUTRError

        Then("Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleInternalServer)
        )
      }
    }

    s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show().url}" in {
			stubAuthorisedAgentUser(authorised = true)

			val result: WSResponse = IncomeTaxViewChangeFrontend.postUTRError

			Then(s"The user is redirected to ${controllers.agent.routes.EnterClientsUTRController.show().url}")
			result should have(
				httpStatus(SEE_OTHER),
				redirectURI(controllers.agent.routes.EnterClientsUTRController.show().url)
			)
    }
  }

}
