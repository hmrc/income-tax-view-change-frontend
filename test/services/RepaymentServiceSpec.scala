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
import exceptions.{RepaymentStartJourneyAmountIsNoneException, RepaymentStartJourneyException, RepaymentViewJourneyException}
import mocks.connectors.MockBusinessDetailsConnector
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE}
import testUtils.TestSupport

import scala.concurrent.{ExecutionException, Future}

class RepaymentServiceSpec extends TestSupport
   with MockBusinessDetailsConnector {

  val nino = "AA010101Q"
  val fullAmount: BigDecimal = BigDecimal("303.00")

  val repaymentConnector: RepaymentConnector = mock(classOf[RepaymentConnector])

  object UnderTestService extends RepaymentService(repaymentConnector, ec)

  "RepaymentService" when {
    "core scenarios" when {
      "action start " should {
        "return success when amount > 0" in {
          val expected = "http://nextUrl/withSessionId"
          when(repaymentConnector.start(any(), any())(any()))
            .thenReturn(Future {
              RepaymentJourneyModel(expected)
            })

          val actualFutureResult = UnderTestService
            .start(nino, fullAmount = Some(fullAmount))
          actualFutureResult.futureValue should be(Right(expected))
        }
        "return failure when amount is None" in {
          val expected = "http://nextUrl/withSessionId"
          when(repaymentConnector.start(any(), any())(any()))
            .thenReturn(Future {
              RepaymentJourneyModel(expected)
            })
          val actualFutureResult = UnderTestService
            .start(nino, fullAmount = None)
          actualFutureResult.futureValue should be(Left(RepaymentStartJourneyAmountIsNoneException))
        }
        "return standard service failure" in {
          val result = Future.successful(RepaymentJourneyErrorResponse(INTERNAL_SERVER_ERROR, "Server error details"))
          when(repaymentConnector.start(any(), any())(any()))
            .thenReturn(result)
          val actualFutureResult = UnderTestService
            .start(nino, fullAmount = Some(fullAmount))
          actualFutureResult.futureValue match {
            case Right(_) =>
              fail("Failure expected")
            case Left(RepaymentStartJourneyException(INTERNAL_SERVER_ERROR,"Server error details")) =>
              succeed
            case _ =>
              fail("Unexpected error")
          }
        }

        "return non standard exception" in {
          val result = Future.failed(new Error("Operation failed"))
          when(repaymentConnector.start(any(), any())(any()))
            .thenReturn(result)
          val actualFutureResult = UnderTestService
            .start(nino, fullAmount = Some(fullAmount))
          actualFutureResult.futureValue match {
            case Right(_) =>
              fail("Failure expected")
            case Left(ex) if ex.isInstanceOf[ExecutionException] =>
              succeed
            case ex =>
              fail(s"Unexpected error: $ex")
          }
        }

        "handle critical error" in {
          when(repaymentConnector.start(any(), any())(any()))
            .thenThrow(new Error("BigBoom"))
          intercept[Error] {
            UnderTestService
              .start(nino, fullAmount = Some(fullAmount))
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
            case Left(RepaymentViewJourneyException(SERVICE_UNAVAILABLE, "Service not available")) =>
              succeed
            case _ => fail("Unexpected error")
          }
        }
        "return not standard failure" in {
          when(repaymentConnector.view(any())(any()))
            .thenReturn(Future.failed { new Error("Some Error")})
          val actualFutureResult = UnderTestService.view(nino)
          actualFutureResult.futureValue match {
            case Right(_) => fail("Failure expected")
            case Left(ex) if ex.isInstanceOf[ExecutionException] =>
              succeed
            case _ => fail("Unexpected error")
          }
        }
        "handle critical error" in {
          when(repaymentConnector.view(any())(any()))
            .thenThrow(new Error("Some internal error"))
          intercept[Error] {
            UnderTestService.view(nino)
          }
        }
      }

    }
  }
}