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

package services

import config.FrontendAppConfig
import mocks.services.{MockCalculationService, MockPollCalculationLockKeeper}
import models.liabilitycalculation._
import play.api.http.Status
import testConstants.BaseTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class CalculationPollingServiceSpec extends TestSupport with MockCalculationService with MockPollCalculationLockKeeper {

  val liabilityCalculationSuccessResponse: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = None
    )),
    messages = None,
    metadata = Metadata("2019-02-15T09:35:15.094Z", Some(false)),
    calculation = None)

  val liabilityCalculationNotFoundResponse: LiabilityCalculationError = LiabilityCalculationError(Status.NOT_FOUND, "not found")
  val liabilityCalculationErrorResponse: LiabilityCalculationError = LiabilityCalculationError(Status.INTERNAL_SERVER_ERROR, "Internal server error")

  def fakeServicesConfig(interval: Int, timeout: Int): ServicesConfig = new ServicesConfig(conf) {
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
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId)(liabilityCalculationSuccessResponse)

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testMtditid).futureValue shouldBe Status.OK
      }
    }

    "when MongoLock is acquired and non-retryable response is received from calculation service" should {
      "return a non-retryable(500) response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId)(liabilityCalculationErrorResponse)

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testMtditid).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(502) is received from calculation service for all retries" should {
      "return a retryable(502) response back" in {

        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId)(LiabilityCalculationError(Status.BAD_GATEWAY, "bad gateway"))

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testMtditid).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(404) is received from calculation service for all retries" should {
      "return a retryable(404) response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId)(liabilityCalculationNotFoundResponse)

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testMtditid).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(404) is received initially from calculation service and 200 after few seconds" should {
      "return a retryable(404) response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId)(liabilityCalculationNotFoundResponse)

        val result = TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testMtditid)

        Thread.sleep(1000)
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId)(liabilityCalculationSuccessResponse)

        result.futureValue shouldBe Status.OK
      }
    }

    "when MongoLock is acquired and retryable response(502) is received initially from calculation service and 504 after few seconds" should {
      "return a retryable(404) response back" in {
        mockLockRepositoryIsLockedTrue()
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId)(LiabilityCalculationError(Status.BAD_GATEWAY, "Bad gateway found"))

        val result = TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testMtditid)

        Thread.sleep(1000)
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId)(LiabilityCalculationError(Status.GATEWAY_TIMEOUT, "Gateway timeout"))

        result.futureValue shouldBe Status.GATEWAY_TIMEOUT
      }
    }

    "when MongoLock acquired failed" should {
      "return a 500 found response back" in {
        mockLockRepositoryIsLockedFalse()

        val result = TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testMtditid)

        result.futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
