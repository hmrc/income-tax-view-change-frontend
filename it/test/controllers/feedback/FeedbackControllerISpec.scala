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

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDPrimaryAgent}
import helpers.FeedbackConnectorStub
import helpers.servicemocks.{MTDAgentAuthStub, MTDIndividualAuthStub}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse

class FeedbackControllerISpec extends ControllerISpecHelper {

  def getPath(isAgent: Boolean, isThankyou: Boolean = false): String = {
    val pathStart = if (isAgent) "/agents" else ""
    val pathEnd = if(isThankyou) "/thankyou" else "/feedback"
    pathStart + pathEnd
  }

  List(false, true).foreach { isAgent =>
    val feedbackPath = getPath(isAgent)
    val thankyouPath = getPath(isAgent, true)
    val (authStub, role) = if(isAgent) (MTDAgentAuthStub, "Agent") else (MTDIndividualAuthStub, "Individual")

    s"calling GET $feedbackPath" should {
      "render the Feedback page" when {
        s"User is an authorised $role" in {
          authStub.stubAuthorisedWhenNoChecks()
          val result = buildGETMTDClient(feedbackPath, Map.empty).futureValue

          result should have(
            httpStatus(OK),
            pageTitle(if(isAgent) MTDPrimaryAgent else MTDIndividual, "feedback.heading")
          )
        }
      }
    }

    s"POST $feedbackPath" should {
      "redirect to thankyou page" when {
        s"user is an authorised $role and all fields filled in" in {
          authStub.stubAuthorisedWhenNoChecks()
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

          val res = buildPOSTMTDPostClient(feedbackPath, Map.empty, formData).futureValue

          val expectedRedirectLocation = if (isAgent) {
            controllers.feedback.routes.FeedbackController.thankYouAgent().url
          } else {
            controllers.feedback.routes.FeedbackController.thankYou().url
          }

          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(expectedRedirectLocation)
          )
        }
      }

      "return an error" when {

        s"user is an authorised $role and missing form data" in {
          authStub.stubAuthorisedWhenNoChecks()

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

          val res = buildPOSTMTDPostClient(feedbackPath, Map.empty, formData).futureValue

          res should have(
            httpStatus(BAD_REQUEST),
            pageTitle(if(isAgent) MTDPrimaryAgent else MTDIndividual, "feedback.heading", isInvalidInput = true)
          )
        }
      }
    }

    s"GET $thankyouPath" should {
      "render the Thankyou page" when {
        s"user is an authorised $role" in {
          authStub.stubAuthorisedWhenNoChecks()
          FeedbackConnectorStub.stubPostThankyou(OK)

          val res: WSResponse = buildGETMTDClient(thankyouPath, Map.empty).futureValue

          res should have(
            httpStatus(OK),
            pageTitle(if(isAgent) MTDPrimaryAgent else MTDIndividual, "feedback.thankYou")
          )
        }
      }
    }
  }
}

