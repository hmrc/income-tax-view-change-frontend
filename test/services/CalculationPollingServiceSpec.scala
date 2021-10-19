/*
 * Copyright 2021 HM Revenue & Customs
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

import testConstants.BaseTestConstants._
import testConstants.CalcBreakdownTestConstants.{calculationDataErrorModel, calculationDataSuccessModel}
import config.FrontendAppConfig
import mocks.services.{MockCalculationService, MockPollCalculationLockKeeper}
import models.calculation.CalculationErrorModel
import play.api.http.Status
import testUtils.TestSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class CalculationPollingServiceSpec extends TestSupport with MockCalculationService with MockPollCalculationLockKeeper {

  def fakeServicesConfig(interval : Int, timeout: Int): ServicesConfig = new ServicesConfig(conf) {
    override def getInt(key: String): Int = key match {
      case "calculation-polling.interval" => interval
      case "calculation-polling.timeout" => timeout
    }
  }

  val frontendAppConfig = new FrontendAppConfig(fakeServicesConfig(250, 1500), conf)

  object TestCalculationPollingService extends CalculationPollingService(
    frontendAppConfig,
    mockMongoLockRepository,
    mockCalculationService
  )

  "The CalculationPollingService.initiateCalculationPollingSchedulerWithMongoLock method" when {
    "when MongoLock is acquired and success response is received from calculation service" should {
      "return a success response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testNino, Right(testCalcId))(calculationDataSuccessModel)

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino).futureValue shouldBe Status.OK
      }
    }

    "when MongoLock is acquired and non-retryable response is received from calculation service" should {
      "return a non-retryable(500) response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testNino, Right(testCalcId))(calculationDataErrorModel)

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(502) is received from calculation service for all retries" should {
      "return a retryable(502) response back" in {

        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testNino, Right(testCalcId))(CalculationErrorModel(Status.BAD_GATEWAY, "bad gateway"))

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(404) is received from calculation service for all retries" should {
      "return a retryable(404) response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testNino, Right(testCalcId))(CalculationErrorModel(Status.NOT_FOUND, "Not found"))

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(404) is received initially from calculation service and 200 after few seconds" should {
      "return a retryable(404) response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testNino, Right(testCalcId))(CalculationErrorModel(Status.NOT_FOUND, "Not found"))

        val result = TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino)

        Thread.sleep(1000)
        setupMockGetLatestCalculation(testNino, Right(testCalcId))(calculationDataSuccessModel)

        result.futureValue shouldBe Status.OK
      }
    }

    "when MongoLock is acquired and retryable response(502) is received initially from calculation service and 504 after few seconds" should {
      "return a retryable(404) response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testNino, Right(testCalcId))(CalculationErrorModel(Status.BAD_GATEWAY, "Bad gateway found"))

        val result = TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino)

        Thread.sleep(1000)
        setupMockGetLatestCalculation(testNino, Right(testCalcId))(CalculationErrorModel(Status.GATEWAY_TIMEOUT, "Gateway timeout"))

        result.futureValue shouldBe Status.GATEWAY_TIMEOUT
      }
    }

    "when MongoLock acquired failed" should {
      "return a 500 found response back" in {
        mockLockRepositoryIsLockedFalse()

        val result = TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino)

        result.futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
