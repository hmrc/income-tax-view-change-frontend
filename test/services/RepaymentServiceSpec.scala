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

import connectors.RepaymentConnector
import mocks.MockHttp
import mocks.connectors.MockBusinessDetailsConnector
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE}
import testUtils.TestSupport

import scala.concurrent.Future

class RepaymentServiceSpec extends TestSupport
  with MockHttp with MockBusinessDetailsConnector {

  val nino = "AA010101Q"
  val fullAmount = BigDecimal("303.00")

  val repaymentConnector: RepaymentConnector = mock(classOf[RepaymentConnector])

  object UnderTestService extends RepaymentService(repaymentConnector, ec)

  "RepaymentService" when {
    "core scenarios" when {
      "action start " should {
        // A: happy path: fullAmount exists and > 0
        "return success - Next url" in {
          val expected = "http://nextUrl/withSessionId"
          when(repaymentConnector.start(any(), any())(any()))
            .thenReturn(Future {
              RepaymentJourneyModel(expected)
            })

          val actualFutureResult = UnderTestService
            .start(nino, fullAmount = Some(fullAmount))
          actualFutureResult.futureValue should be(Right(expected))
        }
        // B: ~: fullAmount not exists or None
        "return failure" in {
          val result = Future.successful(RepaymentJourneyErrorResponse(INTERNAL_SERVER_ERROR, "Server error details"))
          when(repaymentConnector.start(any(), any())(any()))
            .thenReturn(result)
          val actualFutureResult = UnderTestService
            .start(nino, fullAmount = Some(fullAmount))
          actualFutureResult.futureValue match {
            case Right(_) => fail("Failure expected")
            case Left(ex) if ex.isInstanceOf[InternalError] =>
              succeed
            case _ => fail("Unexpected error")
          }
        }
      }

      "action view " should {
        "return success" in {
          val expected = "http://NextUrl/WithSessionId"
          when(repaymentConnector.view(any())(any()))
            .thenReturn(Future.successful {
              RepaymentJourneyModel(expected)
            })
          val actualFutureResult = UnderTestService.view(nino)
          actualFutureResult.futureValue should be(Right(expected))
        }
        "return failure" in {
          when(repaymentConnector.view(any())(any()))
            .thenReturn(Future.successful {
              RepaymentJourneyErrorResponse(SERVICE_UNAVAILABLE, "Service not available")
            })
          val actualFutureResult = UnderTestService.view(nino)
          actualFutureResult.futureValue match {
            case Right(_) => fail("Failure expected")
            case Left(ex) if ex.isInstanceOf[InternalError] =>
              succeed
            case _ => fail("Unexpected error")
          }
        }
      }

    }
  }
}