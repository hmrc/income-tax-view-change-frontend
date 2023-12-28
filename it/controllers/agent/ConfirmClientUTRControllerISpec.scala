
package controllers.agent

import audit.models.ConfirmClientDetailsAuditModel
import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub
import helpers.servicemocks.AuthStub.titleInternalServer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants._

class ConfirmClientUTRControllerISpec extends ComponentSpecBase with FeatureSwitching {


  s"GET ${controllers.agent.routes.ConfirmClientUTRController.show.url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
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
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }

    s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show.url}" when {
      "the client's name and UTR are not in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR()

        Then("The enter client's utr page is returned to the user")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show.url)
        )
      }
    }

    s"return $OK with the confirm client utr page" in {
      stubAuthorisedAgentUser(authorised = true)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR(clientDetailsWithoutConfirmation)

      Then("The confirm client's utr page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgentLogin("agent.confirmClient.heading")
      )
    }

    s"return $OK with empty black banner on the confirm client utr page" in {
      stubAuthorisedAgentUser(authorised = true)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR(clientDetailsWithoutConfirmation)

      val document: Document = Jsoup.parse(result.toString)
      document.select(".govuk-header__content")
        .select(".hmrc-header__service-name hmrc-header__service-name--linked")
        .text() shouldBe ""

      result should have(
        httpStatus(OK),
        pageTitleAgentLogin("agent.confirmClient.heading")
      )
    }
  }

  s"POST ${controllers.agent.routes.ConfirmClientUTRController.submit.url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postConfirmClientUTR()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
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
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }

    s"redirect ($SEE_OTHER) to the next page" in {
      stubAuthorisedAgentUser(authorised = true)

      val result: WSResponse = IncomeTaxViewChangeFrontend.postConfirmClientUTR(clientDetailsWithoutConfirmation)

      AuditStub.verifyAuditEvent(ConfirmClientDetailsAuditModel(clientName = "Test User", nino = testNino, mtditid = testMtditid, arn = "1", saUtr = testSaUtr, credId = None))

      Then("The user is redirected to the next page")
      result should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.routes.HomeController.showAgent.url)
      )
    }
  }
}
