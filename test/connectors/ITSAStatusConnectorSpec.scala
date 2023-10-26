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

import audit.AuditingService
import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import mocks.MockHttp
import org.mockito.Mockito.{mock, when}
import testConstants.BaseTestConstants._
import testConstants.ITSAStatusTestConstants.{badJsonErrorITSAStatusError, errorITSAStatusError}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext

class ITSAStatusConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup extends ITSAStatusConnector {

    val http: HttpClient = mockHttpGet
    val auditingService: AuditingService = mockAuditingService
    val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
    val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    val baseUrl = "http://localhost:9999"

    when(appConfig.itvcProtectedService) thenReturn baseUrl

  }

  "getITSAStatusDetail" should {
    import testConstants.ITSAStatusTestConstants.{badJsonHttpResponse, errorHttpResponse, successHttpResponse, successITSAStatusResponseModel}
    val successResponse = successHttpResponse
    val successResponseBadJson = badJsonHttpResponse
    val badResponse = errorHttpResponse
    val argument = (testNino, "2020", true, true)

    "return a List[ITSAStatusResponseModel] model when successful JSON is received" in new Setup {
      val url = getITSAStatusDetailUrl(argument._1, argument._2)
      setupMockHttpGet(url)(successResponse)
      val result = (getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Right(List(successITSAStatusResponseModel))
    }

    "return ITSAStatusResponseError model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(getITSAStatusDetailUrl(argument._1, argument._2))(successResponseBadJson)
      val result = (getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Left(badJsonErrorITSAStatusError)
    }

    "return ITSAStatusResponseError model in case of failure" in new Setup {
      setupMockHttpGet(getITSAStatusDetailUrl(argument._1, argument._2))(badResponse)
      val result = (getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Left(errorITSAStatusError)

    }
  }
}
