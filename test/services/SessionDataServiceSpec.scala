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

package services

import mocks.connectors.MockSessionDataConnector
import testOnly.models.SessionDataGetResponse.{SessionDataGetSuccess, SessionGetResponse}
import testUtils.TestSupport

class SessionDataServiceSpec extends TestSupport with MockSessionDataConnector {

  object TestSessionDataService extends SessionDataService(mockSessionDataConnector)

  "SessionDataService.getSessionData" when {
    "the connector returns a success response" should {
      "return the same success response" in {
        val response: SessionGetResponse = Right(SessionDataGetSuccess(mtditid = "one", nino = "two", utr = "three", sessionId = "four"))
        setupMockGetSessionData(response)

        TestSessionDataService.getSessionData().futureValue shouldBe response
      }
    }
    "the connector returns an error response" should {
      "return the same error response" in {
        val response: SessionGetResponse = Left(new Exception("TEST ERROR!"))
        setupMockGetSessionData(response)

        TestSessionDataService.getSessionData().futureValue shouldBe response
      }
    }
  }

}
