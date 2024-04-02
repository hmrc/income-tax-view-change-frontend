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

import config.FrontendAppConfig
import forms.FeedbackForm
import mocks.MockHttp
import mocks.services.MockSessionService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status.OK
import play.mvc.Http.Status
import testConstants.PaymentAllocationsTestConstants.testValidPaymentAllocationsModelJson
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.Future

class FeedbackConnectorSpec extends TestSupport with MockHttp with MockSessionService{

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val successResponse = HttpResponse(status = Status.OK, json = testValidPaymentAllocationsModelJson, headers = Map.empty)

  val mockAppConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])

  val TestFeedbackConnector: FeedbackConnector = new FeedbackConnector(mockHttpGet, appConfig, mockItvcHeaderCarrierForPartialsConverter) {
    override val config: FrontendAppConfig = mockAppConfig
  }

  "FeedbackConnector" should {
    "return OK response" when {
      "when form is successful" in {

        val formData: FeedbackForm = FeedbackForm(
            Some("Test Business"),
            "Albert Einstein",
            "alberteinstein@gmail.com",
            "MCXSIMMKZC",
            ""
          )

//        val formDataToFormMap: Map[String, Seq[String]] = {
//          Map(
//            "feedback-rating" -> Seq("Test Business"),
//            "feedback-name" -> Seq("Albert Einstein"),
//            "feedback-email" -> Seq("alberteinstein@gmail.com"),
//            "feedback-comments" -> Seq("MCXSIMMKZC"),
//            "csrfToken" -> Seq(""),
//            "referrer" -> Seq("MCXSIMMKZC")
//          )
//        }

//        val testBody = Json.parse(
//          """
//            |{
//            |"nino": "AA123456A",
//            |"mtdbsa": "XIAT0000000000A",
//            |"businesses":[],
//            |"properties":[]
//            |}
//            """".stripMargin
//
//        )
//

//        val feedbackServiceSubmitUrl = s"${
//          mockAppConfig.contactFrontendBaseUrl
//        }/contact-frontend/contact/beta-feedback/submit?" +
//          s"service=ITVC"

        when(mockHttpClient.POSTForm[HttpResponse](any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "valid")))

//        FeedbackConnectorStub.stubPostFeedback(OK)

        val result = TestFeedbackConnector.submit(formData).futureValue
        result shouldBe Right(())
      }
    }
  }

//    "FeedbackConnector" should {
//      "return OK response" when {
//        "when the status is B" in {
//          disableAllSwitches()
//          enable(IncomeSources)
//
//          val result = TestAddressLookupConnector.addressLookupInitializeUrl
//          result shouldBe s"${baseUrl}/api/v2/init"
//        }
//      }
//    }
}
