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

package connectors

import forms.FeedbackForm
import mocks.MockHttpV2
import mocks.services.MockSessionService
import org.mockito.Mockito._
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials


class FeedbackConnectorSpec extends TestSupport with MockHttpV2 with MockSessionService{

  val formData: FeedbackForm = FeedbackForm(
    Some("Random Rating"),
    "Albert Einstein",
    "alberteinstein@gmail.com",
    "Comments",
    ""
  )

  val feedbackFormData: JsValue = Json.obj(
    "feedbackRating"-> "Random Rating",
    "feedbackName"-> "Albert Einstein",
    "feedbackEmail"-> "alberteinstein@gmail.com",
    "feedbackComments"-> "Comments",
    "feedbackCsrfToken" -> "9394",
    "feedbackReferrer" -> "346446"
  )

  val successResponse: HttpResponse = HttpResponse(status = Status.OK, json = feedbackFormData, headers = Map.empty)
  val badResponse: HttpResponse = HttpResponse(status = Status.BAD_REQUEST, body = "RESPONSE status: BAD_REQUEST")

  val TestFeedbackConnector: FeedbackConnector = new FeedbackConnector(mockHttpClientV2, appConfig, mockItvcHeaderCarrierForPartialsConverter)

  "FeedbackConnector" should {
    "return OK response" when {
      "when form is successful" in {

        setupMockHttpV2Post(TestFeedbackConnector.feedbackServiceSubmitUrl.toString)(successResponse)

        when(mockItvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest)
          .thenReturn(HeaderCarrierForPartials(headerCarrier))
        when(mockItvcHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier)
          .thenReturn(headerCarrier)

        val result = TestFeedbackConnector.submit(formData).futureValue
        result shouldBe Right(())
      }
    }
  }

    "FeedbackConnector" should {
      "return a BAD_REQUEST response" when {
        "when form unsuccessfully completed" in {

          setupMockHttpV2Post(TestFeedbackConnector.feedbackServiceSubmitUrl.toString)(badResponse)

          when(mockItvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest)
            .thenReturn(HeaderCarrierForPartials(headerCarrier))
          when(mockItvcHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier)
            .thenReturn(headerCarrier)

          val result = TestFeedbackConnector.submit(formData).futureValue
          result shouldBe Left(BAD_REQUEST)
        }
      }
    }
}
