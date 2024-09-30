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

import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, FeedbackConnectorStub}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class FeedbackControllerISpec extends ComponentSpecBase {

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

  "calling POST to submit feedback form" should {
    "return OK and redirect to thankyou page" when {
      "user is authorised and all fields filled in" in {

        isAuthorisedUser(authorised = true)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        FeedbackConnectorStub.stubPostFeedback(OK)


        val formData: Map[String, Seq[String]] = {
          Map(
            "feedback-rating" -> Seq("Test Business"),
            "feedback-name" -> Seq("Albert Einstein"),
            "feedback-email" -> Seq("alberteinstein@gmail.com"),
            "feedback-comments" -> Seq("MCXSIMMKZC"),
            "csrfToken" -> Seq("mdkdmskd"),
            "referrer" -> Seq("MCXSIMMKZC")
          )
        }

        When(s"I call POST /report-quarterly/income-and-expenses/view/feedback")
        val res: WSResponse = IncomeTaxViewChangeFrontendManageBusinesses.post("/feedback")(formData)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.feedback.routes.FeedbackController.thankYou().url)
        )

      }
    }
  }

  "calling POST to submit feedback form" should {
    "return an error" when {
      "missing data" in {

        isAuthorisedUser(authorised = true)
        stubUserDetails()
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

        When(s"I call POST /report-quarterly/income-and-expenses/view/feedback")
        val res: WSResponse = IncomeTaxViewChangeFrontendManageBusinesses.post("/feedback")(formData)

        res should have(
          httpStatus(BAD_REQUEST),
          pageTitleIndividual("feedback.heading", isInvalidInput = true)
        )
      }
    }
  }

  "calling GET /report-quarterly/income-and-expenses/view/feedback" should {
    "render the Thankyou page" when {
      "form was completed" in {

        isAuthorisedUser(authorised = true)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        FeedbackConnectorStub.stubPostThankyou(OK)

        When(s"I call POST /report-quarterly/income-and-expenses/view/thankyou")
        val res: WSResponse = IncomeTaxViewChangeFrontendManageBusinesses.getThankyouPage

        res should have(
          httpStatus(OK),
          pageTitleIndividual("feedback.thankYou")
        )
      }
    }
  }

}

