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

import controllers.agent.sessionUtils.SessionKeys
import helpers.FeedbackConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class FeedbackControllerISpec extends ComponentSpecBase {

  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  "calling GET /report-quarterly/income-and-expenses/view/agents/feedback" should {
    "render the Feedback page" when {
      "Agent is authorised" in {

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET /report-quarterly/income-and-expenses/view/agents/feedback")
        val res: WSResponse = IncomeTaxViewChangeFrontend.getFeedbackPage(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitleAgent("feedback.heading")
        )
      }
    }
  }

  "calling POST to submit feedback form" should {
    "return OK and redirect to thankyou page" when {
      "agent is authorised and all fields filled in" in {

        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        FeedbackConnectorStub.stubPostFeedback(OK)

        val formData: Map[String, Seq[String]] = {
          Map(
            "feedback-rating" -> Seq("Test Business"),
            "feedback-name" -> Seq("Albert Einstein"),
            "feedback-email" -> Seq("alberteinstein@gmail.com"),
            "feedback-comments" -> Seq("MCXSIMMKZC"),
            "csrfToken" -> Seq(""),
            "referrer" -> Seq("MCXSIMMKZC")
          )
        }

        When(s"I call POST /report-quarterly/income-and-expenses/view/agents/feedback")
        val res: WSResponse = IncomeTaxViewChangeFrontend.post("/feedback", clientDetailsWithConfirmation)(formData)
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.feedback.routes.FeedbackController.thankYouAgent().url)
        )

      }
    }
  }

  "calling POST to submit feedback form" should {
    "return an error" when {
      "missing data" in {

        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        FeedbackConnectorStub.stubPostFeedback(OK)

        When("Full name is missing")
        val formData: Map[String, Seq[String]] = {
          Map(
            "feedback-rating" -> Seq("Test Business"),
            "feedback-email" -> Seq("alberteinstein@gmail.com"),
            "feedback-comments" -> Seq("MCXSIMMKZC"),
            "csrfToken" -> Seq(""),
            "referrer" -> Seq("MCXSIMMKZC")
          )
        }

        When(s"I call POST /report-quarterly/income-and-expenses/view/agents/feedback")
        val res: WSResponse = IncomeTaxViewChangeFrontend.post("/feedback", clientDetailsWithConfirmation)(formData)

        res should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("feedback.heading", isInvalidInput = true)
        )
      }
    }
  }

  "calling GET /report-quarterly/income-and-expenses/view/agents/feedback" should {
    "render the Thankyou page for agents" when {
      "form was completed" in {

        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        FeedbackConnectorStub.stubPostFeedback(OK)

        When(s"I call POST /report-quarterly/income-and-expenses/view/agents/thankyou")
        val res: WSResponse = IncomeTaxViewChangeFrontend.getThankyouPage(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitleAgent("feedback.thankYou")
        )
      }
    }
  }

}
