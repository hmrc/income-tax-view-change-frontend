/*
 * Copyright 2025 HM Revenue & Customs
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

import mocks.connectors.MockPenaltyDetailsConnector
import models.penalties.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsSuccessResponse}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import testConstants.PenaltiesTestConstants.getPenaltyDetails
import testUtils.TestSupport

class PenaltyDetailsServiceSpec extends TestSupport with MockPenaltyDetailsConnector {

  val mtditid = "123456"
  object TestPenaltyDetailsService extends PenaltyDetailsService(mockGetPenaltyDetailsConnector, appConfig)

  "PenaltyDetailsService" should {

    "return a GetPenaltyDetails successful response model" when {
      "the response from the connector is successful" in {
        setupMockGetPenaltyDetailsConnector(mtditid)(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails)))

        val result = TestPenaltyDetailsService.getPenaltyDetails(mtditid).futureValue
        result shouldBe Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))
      }
    }

    "return a GetPenaltyDetails failure response" when {
      "unable to retrieve the penalty details model from the connector" in {
        setupMockGetPenaltyDetailsConnector(mtditid)(Left(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)))

        val result = TestPenaltyDetailsService.getPenaltyDetails(mtditid).futureValue
        result shouldBe Left(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
