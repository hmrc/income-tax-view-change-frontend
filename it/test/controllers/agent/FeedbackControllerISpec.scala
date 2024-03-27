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

import auth.MtdItUser
import controllers.agent.utils.SessionKeys
import forms.FeedbackForm
import helpers.FeedbackConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{noPropertyOrBusinessResponse, paymentHistoryBusinessAndPropertyResponse}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class FeedbackControllerISpec extends ComponentSpecBase {

  val testForm: FeedbackForm = FeedbackForm(
    Some("Good"), "Albert Einstein", "testuser@gmail.com", "test", "d5f739ae-8615-478d-a393-7fa4b31090e9"
  )

  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val testRefererRoute: String = "/test/referer/route"

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  "calling GET /report-quarterly/income-and-expenses/view/agents/feedback" should {
    "render the Feedback page" when {
      "Agent is authorised" in {

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(200, noPropertyOrBusinessResponse)

        When(s"I call GET /report-quarterly/income-and-expenses/view/agents/feedback")
        val res: WSResponse = IncomeTaxViewChangeFrontend.getFeedbackPage(clientDetailsWithConfirmation)

        res should have(
          httpStatus(200),
          pageTitleAgent("feedback.heading")
        )
      }
    }
  }

  "calling POST to submit feedback form" should {
    "return OK and redirect to thankyou page" when {
      "agent is authorised and all fields filled in" in {

        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(200, noPropertyOrBusinessResponse)
        FeedbackConnectorStub.stubPostFeedback(200)

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
        println("MMMMMMMM" + res.status)
        res should have(
          httpStatus(303),
          redirectURI(controllers.feedback.routes.FeedbackController.thankYouAgent.url)
        )

      }
    }
  }

  "calling POST to submit feedback form" should {
    "return an error" when {
      "missing data" in {

        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(200, noPropertyOrBusinessResponse)
        FeedbackConnectorStub.stubPostFeedback(200)

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
          httpStatus(400),
          pageTitleAgent("feedback.heading", isInvalidInput = true)
        )
      }
    }
  }

  "calling GET /report-quarterly/income-and-expenses/view/agents/feedback" should {
    "render the Thankyou page for agents" when {
      "form was completed" in {

        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(200, noPropertyOrBusinessResponse)
        FeedbackConnectorStub.stubPostFeedback(200)

        When(s"I call POST /report-quarterly/income-and-expenses/view/agents/thankyou")
        val res: WSResponse = IncomeTaxViewChangeFrontend.getThankyouPage(clientDetailsWithConfirmation)

        res should have(
          httpStatus(200),
          pageTitleAgent("feedback.thankYou")
        )
      }
    }
  }

}
