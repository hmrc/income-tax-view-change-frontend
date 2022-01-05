/*
 * Copyright 2022 HM Revenue & Customs
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

package mocks.services

import testConstants.BaseTestConstants._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import repositories.MongoLockRepository
import services.CalculationPollingService
import testUtils.UnitSpec
import utils.PollCalculationLockKeeper

import scala.concurrent.Future

trait MockCalculationPollingService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val testCalcId: String = "1234567890"

  val mockCalculationPollingService: CalculationPollingService = mock[CalculationPollingService]


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCalculationPollingService)
  }

  def setupMockInitiateCalculationPolling(calcId: String, nino: String)(response: Int): Unit =
    when(mockCalculationPollingService
      .initiateCalculationPollingSchedulerWithMongoLock(
        ArgumentMatchers.eq(calcId),
        ArgumentMatchers.eq(nino)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def mockCalculationPollingSuccess(): Unit = setupMockInitiateCalculationPolling(testCalcId, testNino)(Status.OK)
  def mockCalculationPollingRetryableError(): Unit = setupMockInitiateCalculationPolling(testCalcId, testNino)(Status.NOT_FOUND)
  def mockCalculationPollingNonRetryableError(): Unit = setupMockInitiateCalculationPolling(testCalcId, testNino)(Status.INTERNAL_SERVER_ERROR)
}
