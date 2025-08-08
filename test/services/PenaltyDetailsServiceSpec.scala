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
import models.itsaStatus.ITSAStatus
import models.penalties.{GetPenaltyDetails, Totalisations}
import models.penalties.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsSuccessResponse}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import testConstants.BaseTestConstants
import testConstants.PenaltiesTestConstants.getPenaltyDetails
import testUtils.TestSupport

class PenaltyDetailsServiceSpec extends TestSupport with MockPenaltyDetailsConnector {

  val nino = "AA123456A"
  object TestPenaltyDetailsService extends PenaltyDetailsService(mockGetPenaltyDetailsConnector, appConfig)

  "PenaltyDetailsService" when {

    "calling GetPenaltyDetails" should {

      "return a GetPenaltyDetails successful response model" when {
        "the response from the connector is successful" in {
          setupMockGetPenaltyDetailsConnector(nino)(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails)))

          val result = TestPenaltyDetailsService.getPenaltyDetails(nino).futureValue
          result shouldBe Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))
        }
      }

      "return a GetPenaltyDetails failure response" when {
        "unable to retrieve the penalty details model from the connector" in {
          setupMockGetPenaltyDetailsConnector(nino)(Left(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)))

          val result = TestPenaltyDetailsService.getPenaltyDetails(nino).futureValue
          result shouldBe Left(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))
        }
      }

    }

    "calling getPenaltySubmissionFrequency" should {

      "return a response of Quarterly" when {

        "ITSA status is mandated" in {
          val result = TestPenaltyDetailsService.getPenaltySubmissionFrequency(ITSAStatus.Mandated)
          result shouldBe "Quarterly"
        }

        "ITSA status is voluntary" in {
          val result = TestPenaltyDetailsService.getPenaltySubmissionFrequency(ITSAStatus.Voluntary)
          result shouldBe "Quarterly"
        }
      }

      "return a response of Annual" when {
        "ITSA status is annual" in {
          val result = TestPenaltyDetailsService.getPenaltySubmissionFrequency(ITSAStatus.Annual)
          result shouldBe "Annual"
        }
      }

      "return a response of Not Applicable" when {
        val notApplicableStatuses = List(ITSAStatus.NoStatus, ITSAStatus.Dormant, ITSAStatus.Exempt, ITSAStatus.DigitallyExempt)

        notApplicableStatuses.foreach{ status =>
          s"ITSA status is ${status.toString}" in {
            val result = TestPenaltyDetailsService.getPenaltySubmissionFrequency(status)
            result shouldBe "Non Penalty Applicable Status"
          }
        }
      }
    }

    "calling getPenaltiesCount" should {

      "return a valid count of 0" when {

        "the penalties feature is disabled" in {
          setupMockGetPenaltyDetailsConnector(BaseTestConstants.testNino)(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails)))

          val result = TestPenaltyDetailsService.getPenaltiesCount(penaltiesCallEnabled = false).futureValue
          result shouldBe 0
        }

        "the response is successful but with no totalisations object" in {
          setupMockGetPenaltyDetailsConnector(BaseTestConstants.testNino)(Right(GetPenaltyDetailsSuccessResponse(GetPenaltyDetails(None, None, None, None))))

          val result = TestPenaltyDetailsService.getPenaltiesCount(penaltiesCallEnabled = false).futureValue
          result shouldBe 0
        }

        "the response is successful with a zero points total returned" in {
          setupMockGetPenaltyDetailsConnector(BaseTestConstants.testNino)(Right(GetPenaltyDetailsSuccessResponse(
            GetPenaltyDetails(Some(Totalisations(Some(0), None, None, None)), None, None, None))))

          val result = TestPenaltyDetailsService.getPenaltiesCount(penaltiesCallEnabled = false).futureValue
          result shouldBe 0
        }
      }

      "return a valid count greater than 0" when {

        "the response is successful with a non-zero points total returned" in {
          setupMockGetPenaltyDetailsConnector(BaseTestConstants.testNino)(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails)))

          val result = TestPenaltyDetailsService.getPenaltiesCount(penaltiesCallEnabled = true).futureValue
          result shouldBe 2
        }
      }

      "return an error" when {

        "the response is a generic FailureResponse" in {
          setupMockGetPenaltyDetailsConnector(BaseTestConstants.testNino)(Left(GetPenaltyDetailsFailureResponse(500)))

          val exception = intercept[Exception] {
            TestPenaltyDetailsService.getPenaltiesCount(penaltiesCallEnabled = true).futureValue
          }
          exception.getMessage shouldBe "The future returned an exception of type: java.lang.Exception, with message: Get penalty details call failed with status of : 500."
        }

        "the response is a malformed FailureResponse" in {
          setupMockGetPenaltyDetailsConnector(BaseTestConstants.testNino)(Left(GetPenaltyDetailsMalformed))

          val exception = intercept[Exception] {
            TestPenaltyDetailsService.getPenaltiesCount(penaltiesCallEnabled = true).futureValue
          }
          exception.getMessage shouldBe "The future returned an exception of type: java.lang.Exception, with message: Get penalty details call failed with a malformed response body."
        }
      }
    }
  }
}
