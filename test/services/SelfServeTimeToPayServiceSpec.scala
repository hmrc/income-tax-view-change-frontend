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

import connectors.SelfServeTimeToPayConnector
import exceptions.SelfServeTimeToPayJourneyException
import mocks.MockHttp
import models.core.{SelfServeTimeToPayJourneyErrorResponse, SelfServeTimeToPayJourneyResponseModel}
import org.mockito.Mockito.{mock, when}
import testUtils.TestSupport
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.INTERNAL_SERVER_ERROR
import testConstants.BaseTestConstants.{expectedJourneyId, testSetUpPaymentPlanUrl}

import scala.concurrent.Future

class SelfServeTimeToPayServiceSpec extends TestSupport with MockHttp {

  val selfServeTimeToPayConnector: SelfServeTimeToPayConnector = mock(classOf[SelfServeTimeToPayConnector])
  val selfServeTimeToPayService = new SelfServeTimeToPayService(selfServeTimeToPayConnector)

  "SelfServeTimeToPayService" should {
    "startSelfServeTimeToPayJourney" when {
      "return Right when connector returns successful response" in {

        when(selfServeTimeToPayConnector.startSelfServeTimeToPayJourney(any())(any()))
          .thenReturn(Future {
            SelfServeTimeToPayJourneyResponseModel(expectedJourneyId, testSetUpPaymentPlanUrl)
          })
        selfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any())
          .futureValue shouldBe Right(testSetUpPaymentPlanUrl)
      }
    }
    "return a Left" when {
      "startSelfServeTimeToPayJourney failed to start" in {

        when(selfServeTimeToPayConnector.startSelfServeTimeToPayJourney(any())(any()))
          .thenReturn(Future {
            SelfServeTimeToPayJourneyErrorResponse(INTERNAL_SERVER_ERROR, "Error message")
          })
        selfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any())
          .futureValue shouldBe Left(SelfServeTimeToPayJourneyException(INTERNAL_SERVER_ERROR, "Error message"))
      }
    }
    "return a Left" when {
      "startSelfServeTimeToPayJourney future failed" in {

        when(selfServeTimeToPayConnector.startSelfServeTimeToPayJourney(any())(any()))
          .thenReturn(Future.failed(new Exception("Error message")))
        selfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any())
          .futureValue.toString shouldBe Left(new Exception("Error message")).toString
      }
    }
  }
}
