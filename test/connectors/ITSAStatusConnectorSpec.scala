/*
 * Copyright 2023 HM Revenue & Customs
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

/*
 * Copyright 2023 HM Revenue & Customs
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

import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import mocks.MockHttpV2
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.Configuration
import play.api.http.Status.INTERNAL_SERVER_ERROR
import testConstants.BaseTestConstants._
import testConstants.ITSAStatusTestConstants.{badJsonErrorITSAStatusError, notFoundHttpResponse}
import testUtils.TestSupport
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class ITSAStatusConnectorSpec extends TestSupport with MockHttpV2 with MockAuditingService {

  trait Setup {
    val baseUrl = "http://localhost:9999"
    def getAppConfig: FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val incomeTaxObligationsService: String = "http://localhost:9999"
      }

    val connector = new ITSAStatusConnector(mockHttpClientV2, getAppConfig)

    def transformMock(): OngoingStubbing[RequestBuilder] = {
      when(mockRequestBuilder
        .transform(ArgumentMatchers.any()))
        .thenReturn(mockRequestBuilder)
    }
  }

  "getITSAStatusDetail" should {
    import testConstants.ITSAStatusTestConstants.{badJsonHttpResponse, successHttpResponse, successITSAStatusResponseModel}
    val successResponse = successHttpResponse
    val successResponseBadJson = badJsonHttpResponse
    val notFoundResponse = notFoundHttpResponse

    val argument = (testNino, "2020", true, true)

    "return a List[ITSAStatusResponseModel] model when successful JSON is received" in new Setup {
      val url: String = connector.getITSAStatusDetailUrl(argument._1, argument._2, argument._3, argument._4)
      setupMockHttpV2Get(url)(successResponse)

      transformMock()

      val result: Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] =
        (connector.getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Right(List(successITSAStatusResponseModel))
    }

    "return a Empty List[] model when NOT_FOUND is received" in new Setup {
      val url: String = connector.getITSAStatusDetailUrl(argument._1, argument._2, argument._3, argument._4)
      setupMockHttpV2Get(url)(notFoundResponse)

      transformMock()

      val result: Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] =
        (connector.getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Right(List())
    }

    "return ITSAStatusResponseError model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpV2Get(connector.getITSAStatusDetailUrl(argument._1, argument._2, argument._3, argument._4))(successResponseBadJson)

      transformMock()

      val result: Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] =
        (connector.getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Left(badJsonErrorITSAStatusError)
    }

    "return ITSAStatusResponseError model in case of failure" in new Setup {
      setupMockFailedHttpV2Get(
        connector.getITSAStatusDetailUrl(argument._1, argument._2, argument._3, argument._4)
      )

      transformMock()

      val result: Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] =
        (connector.getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "unknown error"))
    }
  }
}
