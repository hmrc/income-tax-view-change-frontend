/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.agent

import audit.models.ConfirmClientDetailsAuditModel
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub, MTDAgentAuthStub}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class ConfirmClientUTRControllerISpec extends ControllerISpecHelper {

  val path = "/agents/confirm-client-details"

  s"GET ${controllers.agent.routes.ConfirmClientUTRController.show.url}" when {
    s"a user is a primary agent (session data isSupportingAgent = false)" that {
      val additionalCookies = getAgentClientDetailsForCookie(false, false)
      "is authenticated, with a valid agent and client delegated enrolment" should {
        "render the confirm client utr page with an empty black banner" in {
          MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, false)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          val result = buildMTDClient(path, additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            pageTitleAgentLogin("agent.confirmClient.heading")
          )

          val document: Document = Jsoup.parse(result.toString)
          document.select(".govuk-header__content")
            .select(".hmrc-header__service-name hmrc-header__service-name--linked")
            .text() shouldBe ""
        }
      }

      testAuthFailuresForMTDAgent(path, false, false)
    }

    s"a user is a supporting agent (session data isSupportingAgent = true)" that {
      val additionalCookies = getAgentClientDetailsForCookie(true, false)
      "is authenticated, with a valid agent and client delegated enrolment" should {
        "render the confirm client utr page with an empty black banner" in {
          MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          val result = buildGETMTDClient(path, additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            pageTitleAgentLogin("agent.confirmClient.heading")
          )

          val document: Document = Jsoup.parse(result.toString)
          document.select(".govuk-header__content")
            .select(".hmrc-header__service-name hmrc-header__service-name--linked")
            .text() shouldBe ""
        }
      }

      testAuthFailuresForMTDAgent(path, true, false)
    }

    testNoClientDataFailure(path)
  }

  s"POST ${controllers.agent.routes.ConfirmClientUTRController.submit.url}" when {
    s"a user is a primary agent (session data isSupportingAgent = false)" that {
      val additionalCookies = getAgentClientDetailsForCookie(false, false)
      "is authenticated, with a valid agent and client delegated enrolment" should {
        s"redirect ($SEE_OTHER) to the agent home page" in {
          MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.showAgent.url)
          )
          AuditStub.verifyAuditEvent(ConfirmClientDetailsAuditModel(clientName = "Test User", nino = testNino, mtditid = testMtditid, arn = "1", saUtr = testSaUtr, credId = None))

        }
      }

      testAuthFailuresForMTDAgent(path, true, false, optBody = Some(Map.empty))

    }

    s"a user is a supporting agent (session data isSupportingAgent = true)" that {
      val additionalCookies = getAgentClientDetailsForCookie(false, false)
      "is authenticated, with a valid agent and client delegated enrolment" should {
        s"redirect ($SEE_OTHER) to the agent home page" in {
          MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, false)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.showAgent.url)
          )
          AuditStub.verifyAuditEvent(ConfirmClientDetailsAuditModel(clientName = "Test User", nino = testNino, mtditid = testMtditid, arn = "1", saUtr = testSaUtr, credId = None))

        }
      }

      testAuthFailuresForMTDAgent(path, false, false, optBody = Some(Map.empty))

    }

    testNoClientDataFailure(path, optBody = Some(Map.empty))
  }
}
