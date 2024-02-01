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
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, DurationInt, MILLISECONDS}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CalculationPollingService @Inject()(val frontendAppConfig: FrontendAppConfig,
                                          val mongoLockRepository: MongoLockRepository,
                                          val calculationService: CalculationService,
                                          system: ActorSystem)
                                         (implicit ec: ExecutionContext) {

  lazy val lockService: LockService = LockService(
    mongoLockRepository, lockId = "calc-poller",
    ttl = Duration.create(frontendAppConfig.calcPollSchedulerTimeout, MILLISECONDS))

  private lazy val retryableStatusCodes: List[Int] = List(Status.BAD_GATEWAY, Status.NOT_FOUND, NO_CONTENT)

  def initiateCalculationPollingSchedulerWithMongoLock(calcId: String, nino: String, taxYear: Int, mtditid: String)
                                                      (implicit headerCarrier: HeaderCarrier): Future[Any] = {
    val endTimeInMillis: Long = System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerTimeout
    //Acquire Mongo lock and call Calculation service
    //To avoid wait time for first call, calling getCalculationResponse with end time as current time
    lockService.withLock {
      getCalculationResponse(System.currentTimeMillis(), endTimeInMillis, calcId, nino, taxYear, mtditid)
    } flatMap {
      case Some(statusCode) =>
        Logger("application").info(s"[CalculationPollingService] - ${Thread.currentThread().getId}")
        Logger("application").info(s"[CalculationPollingService] Response received from Calculation service: $statusCode")
        if (!retryableStatusCodes.contains(statusCode)) Future.successful(statusCode)
        else pollCalcInIntervals(calcId, nino, taxYear, mtditid, endTimeInMillis)
      case None =>
        Logger("application").info(s"[CalculationPollingService] - ${Thread.currentThread().getId}")
        Logger("application").info("[CalculationPollingService] Failed to acquire Mongo lock")
        Future.successful(Status.INTERNAL_SERVER_ERROR)
    }
  }

  private def getCalculationResponse(endTimeForEachInterval: Long,
                                     endTimeInMillis: Long,
                                     calcId: String,
                                     nino: String,
                                     taxYear: Int,
                                     mtditid: String
                                    )
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    for {
      result <-
        calculationService.getLatestCalculation(mtditid, nino, calcId, taxYear).map {
          case _: LiabilityCalculationResponse => Status.OK
          case error: LiabilityCalculationError =>
            if (System.currentTimeMillis() > endTimeInMillis) Status.INTERNAL_SERVER_ERROR
            else error.status
        }
    } yield result
  }

  private def pollCalcInIntervals(calcId: String,
                                  nino: String,
                                  taxYear: Int,
                                  mtditid: String,
                                  endTimeInMillis: Long)
                                 (implicit hc: HeaderCarrier): Future[Int] = {
    Logger("application").info(s"[CalculationPollingService][pollCalcInIntervals] - A ${Thread.currentThread().getId}")
    implicit val scheduler: Scheduler = system.scheduler

    // http://localhost:9081/report-quarterly/income-and-expenses/view/calculation/2023/submitted
    def futureToAttempt(): Future[Int] = {
      Logger("application").info(s"[CalculationPollingService][futureToAttempt] - B - ${Thread.currentThread().getId}")
      for {
        statusCode <- getCalculationResponse(System.currentTimeMillis(), endTimeInMillis, calcId, nino, taxYear, mtditid)
        res <- {
          if (!retryableStatusCodes.contains(statusCode))
            Future.successful {
              statusCode
            }
          else
            Future.failed {
              new RuntimeException(s"$statusCode")
            }
        }
      } yield res
    }

    // TODO: move setting into config ???
    // V1: fixed delay between calls
    //retry(() => futureToAttempt(), attempts = 10, 800.milliseconds)
    // V2: with backOff
    retry(() => futureToAttempt(), attempts = 10, minBackoff = 1.second,  maxBackoff = 10.seconds, randomFactor = 0.5)
  }

}
