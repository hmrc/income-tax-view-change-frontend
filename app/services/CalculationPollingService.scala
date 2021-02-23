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

import config.FrontendAppConfig
import models.calculation._
import org.joda.time.Duration
import play.api.Logger
import play.api.http.Status
import repositories.MongoLockRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.PollCalculationLockKeeper

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, blocking}


@Singleton
class CalculationPollingService @Inject()(val frontendAppConfig: FrontendAppConfig,
                                          val mongoLockRepository: MongoLockRepository,
                                          val calculationService: CalculationService)
                                         (implicit ec: ExecutionContext) {

  private lazy val retryableStatusCodes: List[Int] = List(Status.BAD_GATEWAY, Status.NOT_FOUND)

  def initiateCalculationPollingSchedulerWithMongoLock(calcId: String, nino: String)
                                                      (implicit headerCarrier: HeaderCarrier): Future[Int] = {


    lazy val lockKeeper: PollCalculationLockKeeper = new PollCalculationLockKeeper {
      override val lockId = calcId
      override val forceLockReleaseAfter: Duration = Duration.millis(frontendAppConfig.calcPollSchedulerTimeout)
      override lazy val repo = mongoLockRepository.repo
    }

    val endTimeInMillis: Long = System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerTimeout

    //Create MongoLock and call Calculation service
    lockKeeper.tryLock().flatMap {
      isLocked =>
        if(isLocked) {
          //to avoid wait time for first call, calling getCalculationResponse with end time as current time
          getCalculationResponse(System.currentTimeMillis(), endTimeInMillis, calcId, nino).flatMap {
            case statusCode if !retryableStatusCodes.contains(statusCode) => {
              lockKeeper.releaseLock
              Future.successful(statusCode)
            }
            case _ => pollCalcInIntervals(calcId, nino, lockKeeper, endTimeInMillis)
          }
        } else {
          Future.successful(Status.INTERNAL_SERVER_ERROR)
        }
    }
  }

  //Waits for polling interval time to complete and responds with response code from calculation service.
  private def getCalculationResponse(endTimeForEachInterval: Long,
                                     endTimeInMillis: Long,
                                     calcId: String, nino: String)
                                    (implicit hc: HeaderCarrier): Future[Int] = {

    Logger.debug(s"[CalculationPollingService][getCalculationResponse] Starting polling for  calcId: $calcId and nino: $nino")
    while(System.currentTimeMillis() < endTimeForEachInterval) {
      //Waiting until interval time is complete
    }

    calculationService.getLatestCalculation(nino, Right(calcId)).map {
      case _: Calculation => Status.OK
      case error: CalculationErrorModel => {
        if(System.currentTimeMillis() > endTimeInMillis) Status.INTERNAL_SERVER_ERROR
        else error.code
      }
    }
  }

  private def pollCalcInIntervals(calcId: String, nino: String,
                                  lockKeeper: PollCalculationLockKeeper,
                                  endTimeInMillis: Long)
                                 (implicit hc: HeaderCarrier): Future[Int] = {
    blocking {
      while (retryableStatusCodes.contains(
        Await.result(getCalculationResponse(
          System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerInterval,
          endTimeInMillis,
          calcId, nino
        ), frontendAppConfig.calcPollSchedulerTimeout.millis))) {
        //polling calc service until non retryable response code is received
      }

      lockKeeper.releaseLock
      getCalculationResponse(System.currentTimeMillis(), endTimeInMillis, calcId, nino)
    }
  }
}
