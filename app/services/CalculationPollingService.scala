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
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.pattern.retry
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, DurationInt, MILLISECONDS}
import scala.concurrent.{ExecutionContext, Future}

@deprecated("Being moved to submission team", "MISUV-8977")
@Singleton
class CalculationPollingService @Inject()(val frontendAppConfig: FrontendAppConfig,
                                          val mongoLockRepository: MongoLockRepository,
                                          val calculationService: CalculationService,
                                          system: ActorSystem)
                                         (implicit ec: ExecutionContext) {

  private implicit val scheduler: Scheduler = system.scheduler

  lazy val lockService: LockService = LockService(
    mongoLockRepository, lockId = "calc-poller",
    ttl = Duration.create(frontendAppConfig.calcPollSchedulerTimeout, MILLISECONDS))

  private lazy val retryableStatusCodes: List[Int] = List(Status.BAD_GATEWAY, Status.NO_CONTENT)

  def initiateCalculationPollingSchedulerWithMongoLock(calcId: String, nino: String, taxYear: Int, mtditid: String)
                                                      (implicit headerCarrier: HeaderCarrier): Future[Any] = {
    val endTimeInMillis: Long = System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerTimeout
    //Acquire Mongo lock and call Calculation service
    //To avoid wait time for first call, calling getCalculationResponse with end time as current time
    lockService.withLock {
      getCalculationResponse(endTimeInMillis, calcId, nino, taxYear, mtditid)
    } flatMap {
      case Some(statusCode) =>
        Logger("application").debug(s"[CalculationPollingService] - Response received from Calculation service: $statusCode")
        if (!retryableStatusCodes.contains(statusCode)) Future.successful(statusCode)
        else {
          // V0: Original version
          //pollCalcInIntervals(calcId, nino, taxYear, mtditid, endTimeInMillis)

          // Ref: https://pekko.apache.org/docs/pekko/current/futures.html
          // V1: apply retry with a fixed delay between calls
          retry(() =>
            attemptToPollCalc(calcId, nino, taxYear, mtditid, endTimeInMillis),
            attempts = frontendAppConfig.calcPollNumberOfAttempts,
            delay = frontendAppConfig.calcPollDelayBetweenAttempts.millisecond)

          // V2: apply retry with with backOff strategy
          //retry(() => futureToAttempt(), attempts = 10, minBackoff = 1.second,  maxBackoff = 10.seconds, randomFactor = 0.5)
        }
      case None =>
        Logger("application").info(s"[CalculationPollingService] - Failed to acquire Mongo lock")
        Future.successful(Status.INTERNAL_SERVER_ERROR)
    }
  }

  private def getCalculationResponse(endTimeInMillis: Long,
                                     calcId: String,
                                     nino: String,
                                     taxYear: Int,
                                     mtditid: String
                                    )
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    Logger("application").debug(s"Starting polling for calcId: $calcId and nino: $nino")
    for {
      result <-
        calculationService.getLatestCalculation(mtditid, nino, calcId, taxYear).map {
          case _: LiabilityCalculationResponse => Status.OK
          case error: LiabilityCalculationError =>
            // TODO: remove next line in the future, atm this cause tests to fail
            if (System.currentTimeMillis() > endTimeInMillis) Status.INTERNAL_SERVER_ERROR
            else error.status
        }
    } yield result
  }

  private def attemptToPollCalc(calcId: String,
                                  nino: String,
                                  taxYear: Int,
                                  mtditid: String,
                                  endTimeInMillis: Long)
                                 (implicit hc: HeaderCarrier): Future[Int] = {
    for {
      statusCode <- getCalculationResponse(endTimeInMillis, calcId, nino, taxYear, mtditid)
      resultFuture <- {
        if (!retryableStatusCodes.contains(statusCode))
          Future.successful {
            statusCode
          }
        else // fail future in order to trigger retry
          Future.failed {
            new RuntimeException(s"Fail to evaluate calc response: $statusCode")
          }
      }
    } yield resultFuture
  }

}
