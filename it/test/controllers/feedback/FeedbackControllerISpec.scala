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

package controllers.feedback

import auth.MtdItUser
import forms.FeedbackForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, FeedbackConnectorStub}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{noPropertyOrBusinessResponse, paymentHistoryBusinessAndPropertyResponse}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class FeedbackControllerISpec extends ComponentSpecBase {

  val testForm: FeedbackForm = FeedbackForm(
    Some("Good"), "Albert Einstein", "testuser@gmail.com", "test", "d5f739ae-8615-478d-a393-7fa4b31090e9"
  )

  val testRefererRoute: String = "/test/referer/route"

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  "calling GET /report-quarterly/income-and-expenses/view/feedback" should {
    "render the Feedback page" when {
      "User is authorised" in {

        isAuthorisedUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET /report-quarterly/income-and-expenses/view/feedback")
        val res: WSResponse = IncomeTaxViewChangeFrontendManageBusinesses.getFeedbackPage

        res should have(
          httpStatus(OK),
          pageTitleIndividual("feedback.heading")
        )
      }
    }
  }

  "Navigating to /report-quarterly/income-and-expenses/view/agents/thankyou" when {

    "Agent" when {

      "all fields filled in" in {
      //enable()
      }
    }
  }

  "calling POST to submit feedback form" should {
    "OK and redirect to thankyou page" when {
      "user is authorised and all fields filled in" in {

        isAuthorisedUser(authorised = true)
        stubUserDetails()
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

        When(s"I call POST /report-quarterly/income-and-expenses/view/thankyou")
        val res: WSResponse = IncomeTaxViewChangeFrontendManageBusinesses.post("/feedback")(formData)
        res should have(
          httpStatus(303),
          //pageTitleIndividual("feedback.thankYou") TODO: use appropriate title here
        )

      }
    }
  }

}

