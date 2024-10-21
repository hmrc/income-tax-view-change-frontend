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

package services

import controllers.agent.sessionUtils
import mocks.connectors.MockSessionDataConnector
import testOnly.models.SessionDataGetResponse.{SessionDataGetSuccess, SessionGetResponse}
import testUtils.TestSupport

class SessionDataServiceSpec extends TestSupport with MockSessionDataConnector {

  object TestSessionDataService extends SessionDataService(mockSessionDataConnector)

  "SessionDataService.getSessionData(useCookie = true)" should {
    "return a SessionGetSuccessResponse" when {
      "the cookie contains the mtdId, utr and nino" in {
        val response: SessionGetResponse = Right(SessionDataGetSuccess(mtditid = "one", nino = "two", utr = "three", sessionId = "not required"))
        val request = fakeRequestWithActiveSession.withSession(
          sessionUtils.SessionKeys.clientUTR -> "three",
          sessionUtils.SessionKeys.clientMTDID -> "one",
          sessionUtils.SessionKeys.clientNino -> "two"
        )

        TestSessionDataService.getSessionData(true)(request, headerCarrier).futureValue shouldBe response
      }
    }

    "return a SessionGetSuccessResponse" when {
      "the cookie does not contain mtditid" in {
        val response: SessionGetResponse = Left(throw new Exception("Cookie does not contain agent data"))
        val request = fakeRequestWithActiveSession.withSession(
          sessionUtils.SessionKeys.clientUTR -> "three",
          sessionUtils.SessionKeys.clientNino -> "two"
        )

        TestSessionDataService.getSessionData(true)(request, headerCarrier).futureValue shouldBe response
      }

      "the cookie does not contain utr" in {
        val response: SessionGetResponse = Left(throw new Exception("Cookie does not contain agent data"))
        val request = fakeRequestWithActiveSession.withSession(
          sessionUtils.SessionKeys.clientMTDID -> "one",
          sessionUtils.SessionKeys.clientNino -> "two"
        )

        TestSessionDataService.getSessionData(true)(request, headerCarrier).futureValue shouldBe response

      }

      "the cookie does not contain nino" in {
        val response: SessionGetResponse = Left(throw new Exception("Cookie does not contain agent data"))
        val request = fakeRequestWithActiveSession.withSession(
          sessionUtils.SessionKeys.clientMTDID -> "one",
          sessionUtils.SessionKeys.clientUTR -> "two"
        )

        TestSessionDataService.getSessionData(true)(request, headerCarrier).futureValue shouldBe response
      }

      "the cookie does not contain session data" in {
        val response: SessionGetResponse = Left(throw new Exception("Cookie does not contain agent data"))
        val request = fakeRequestWithActiveSession

        TestSessionDataService.getSessionData(true)(request, headerCarrier).futureValue shouldBe response
      }
    }
  }

  "SessionDataService.getSessionData(useCookie = false)" when {
    val request = fakeRequestWithActiveSession
    "the connector returns a success response" should {
      "return the same success response" in {
        val response: SessionGetResponse = Right(SessionDataGetSuccess(mtditid = "one", nino = "two", utr = "three", sessionId = "four"))
        setupMockGetSessionData(response)

        TestSessionDataService.getSessionData()(request, headerCarrier).futureValue shouldBe response
      }
    }
    "the connector returns an error response" should {
      "return the same error response" in {
        val response: SessionGetResponse = Left(new Exception("TEST ERROR!"))
        setupMockGetSessionData(response)

        TestSessionDataService.getSessionData()(request, headerCarrier).futureValue shouldBe response
      }
    }
  }

}
