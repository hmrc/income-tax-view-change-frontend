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
import mocks.MockHttp
import org.mockito.Mockito.when
import testConstants.BaseTestConstants._
import testConstants.ITSAStatusTestConstants.{badJsonErrorITSAStatusError, errorITSAStatusError}
import testUtils.TestSupport

class ITSAStatusConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  trait Setup {
    val connector = new ITSAStatusConnector(mockHttpGet, appConfig)
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
      val url = connector.getITSAStatusDetailUrl(argument._1, argument._2)
      setupMockHttpGet(url)(successResponse)
      val result = (connector.getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Right(List(successITSAStatusResponseModel))
    }

    "return ITSAStatusResponseError model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpGet(connector.getITSAStatusDetailUrl(argument._1, argument._2))(successResponseBadJson)
      val result = (connector.getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Left(badJsonErrorITSAStatusError)
    }

    "return ITSAStatusResponseError model in case of failure" in new Setup {
      setupMockHttpGet(connector.getITSAStatusDetailUrl(argument._1, argument._2))(badResponse)
      val result = (connector.getITSAStatusDetail _).tupled(argument)
      result.futureValue shouldBe Left(errorITSAStatusError)

    }
  }
}
