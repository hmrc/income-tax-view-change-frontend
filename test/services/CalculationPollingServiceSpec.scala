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

import config.FrontendAppConfig
import mocks.services.{LockServiceDidNotAcquireMongoLock, MockCalculationService}
import models.liabilitycalculation._
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status
import testConstants.BaseTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
@deprecated("Being moved to submission team", "MISUV-8977")
class CalculationPollingServiceSpec extends TestSupport with MockCalculationService {
  val liabilityCalculationSuccessResponse: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(
      taxRegime = "UK", class2VoluntaryContributions = None
    )),
    messages = None,
    metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), Some(false), Some("customerRequest")),
    calculation = None)

  val liabilityCalculationNoContentResponse: LiabilityCalculationError = LiabilityCalculationError(Status.NO_CONTENT, "no content")
  val liabilityCalculationErrorResponse: LiabilityCalculationError = LiabilityCalculationError(Status.INTERNAL_SERVER_ERROR, "Internal server error")
  val testCalcId: String = "1234567890"

  def fakeServicesConfig(interval: Int, timeout: Int): ServicesConfig = new ServicesConfig(conf) {
    override def getInt(key: String): Int = key match {
      case "calculation-polling.interval" => interval
      case "calculation-polling.timeout" => timeout
      case "calculation-polling.attempts" => 10
      case "calculation-polling.delayBetweenAttemptInMilliseconds" => 500
    }
  }

  val frontendAppConfig: FrontendAppConfig = new FrontendAppConfig(fakeServicesConfig(250, 1500), conf)
  val mockMongoLockRepository: MongoLockRepository = app.injector.instanceOf[MongoLockRepository]
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))


  object TestCalculationPollingService extends CalculationPollingService(
    frontendAppConfig,
    mockMongoLockRepository,
    mockCalculationService,
    actorSystem
  )

  "The CalculationPollingService.initiateCalculationPollingSchedulerWithMongoLock method" when {
    "when MongoLock is acquired and success response is received from calculation service" should {
      "return a success response back" in {
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId, testTaxYear)(liabilityCalculationSuccessResponse)

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testTaxYear, testMtditid).futureValue shouldBe Status.OK
      }
    }

    "when MongoLock is acquired and non-retryable response is received from calculation service" should {
      "return a non-retryable(500) response back" in {
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId, testTaxYear)(liabilityCalculationErrorResponse)

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testTaxYear, testMtditid).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(502) is received from calculation service for all retries" should {
      "return a retryable(502) response back" in {

        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId, testTaxYear)(LiabilityCalculationError(Status.BAD_GATEWAY, "bad gateway"))

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testTaxYear, testMtditid).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(204) is received from calculation service for all retries" should {
      "return a retryable(404) response back" in {
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId, testTaxYear)(liabilityCalculationNoContentResponse)

        TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testTaxYear, testMtditid).futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "when MongoLock is acquired and retryable response(204) is received initially from calculation service and 200 after few seconds" should {
      "return a retryable(404) response back" in {
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId, testTaxYear)(liabilityCalculationNoContentResponse)

        val result = TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testTaxYear, testMtditid)

        Thread.sleep(1000)
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId, testTaxYear)(liabilityCalculationSuccessResponse)

        result.futureValue shouldBe Status.OK
      }
    }

    "when MongoLock is acquired and retryable response(502) is received initially from calculation service and 504 after few seconds" should {
      "return a retryable(404) response back" in {
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId, testTaxYear)(LiabilityCalculationError(Status.BAD_GATEWAY, "Bad gateway found"))

        val result = TestCalculationPollingService
          .initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testTaxYear, testMtditid)

        Thread.sleep(1000)
        setupMockGetLatestCalculation(testMtditid, testNino, testCalcId, testTaxYear)(LiabilityCalculationError(Status.GATEWAY_TIMEOUT, "Gateway timeout"))

        result.futureValue shouldBe Status.GATEWAY_TIMEOUT
      }
    }

    "when MongoLock acquired failed" should {
      "return a 500 found response back" in {
        object TestCalculationPollingServiceWithFailedMongoLock extends CalculationPollingService(
          frontendAppConfig,
          mockMongoLockRepository,
          mockCalculationService,
          actorSystem
        ) {
          override lazy val lockService: LockService =
            new LockServiceDidNotAcquireMongoLock
        }
        val result = TestCalculationPollingServiceWithFailedMongoLock.initiateCalculationPollingSchedulerWithMongoLock(testCalcId, testNino, testTaxYear, testMtditid)
        result.futureValue shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
