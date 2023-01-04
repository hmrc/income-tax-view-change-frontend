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
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, DurationInt, MILLISECONDS}
import scala.concurrent.{Await, ExecutionContext, Future, blocking}


@Singleton
class CalculationPollingService @Inject()(val frontendAppConfig: FrontendAppConfig,
                                          val mongoLockRepository: MongoLockRepository,
                                          val calculationService: CalculationService)
                                         (implicit ec: ExecutionContext) {

  lazy val lockService = LockService(mongoLockRepository, lockId = "calc-poller", ttl = Duration.create(frontendAppConfig.calcPollSchedulerTimeout, MILLISECONDS))
  private lazy val retryableStatusCodes: List[Int] = List(Status.BAD_GATEWAY, Status.NOT_FOUND)

  def initiateCalculationPollingSchedulerWithMongoLock(calcId: String, nino: String, taxYear: Int, mtditid: String)
                                                      (implicit headerCarrier: HeaderCarrier): Future[Any] = {



    val endTimeInMillis: Long = System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerTimeout
    //Acquire Mongo lock and call Calculation service
    //To avoid wait time for first call, calling getCalculationResponse with end time as current time
    lockService.withLock(getCalculationResponse(System.currentTimeMillis(), endTimeInMillis, calcId, nino, taxYear, mtditid)) flatMap {
      case Some(statusCode) =>
        Logger("application").debug(s"[CalculationPollingService] Response received from Calculation service")
        if (!retryableStatusCodes.contains(statusCode)) Future.successful(statusCode)
        else pollCalcInIntervals(calcId, nino, taxYear, mtditid, endTimeInMillis)
      case None =>
        Logger("application").debug(s"[CalculationPollingService] Failed to acquire Mongo lock")
        Future.successful(Status.INTERNAL_SERVER_ERROR)
    }
  }

  //Waits for polling interval time to complete and responds with response code from calculation service
  private def getCalculationResponse(endTimeForEachInterval: Long,
                                     endTimeInMillis: Long,
                                     calcId: String,
                                     nino: String,
                                     taxYear: Int,
                                     mtditid: String
                                    )
                                    (implicit hc: HeaderCarrier): Future[Int] = {

    Logger("application").debug(s"[CalculationPollingService][getCalculationResponse] Starting polling for  calcId: $calcId and nino: $nino")
    while (System.currentTimeMillis() < endTimeForEachInterval) {
      //Waiting until interval time is complete
    }

    calculationService.getLatestCalculation(mtditid, nino, calcId, taxYear).map {
      case _: LiabilityCalculationResponse => Status.OK
      case error: LiabilityCalculationError => {
        if (System.currentTimeMillis() > endTimeInMillis) Status.INTERNAL_SERVER_ERROR
        else error.status
      }
    }
  }

  private def pollCalcInIntervals(calcId: String, nino: String, taxYear: Int, mtditid: String,
                                  endTimeInMillis: Long)
                                 (implicit hc: HeaderCarrier): Future[Int] = {
    blocking {
      while (retryableStatusCodes.contains(
        Await.result(getCalculationResponse(
          System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerInterval,
          endTimeInMillis,
          calcId, nino, taxYear, mtditid
        ), frontendAppConfig.calcPollSchedulerTimeout.millis))) {
        //Polling calc service until non-retryable response code is received
      }

      getCalculationResponse(System.currentTimeMillis(), endTimeInMillis, calcId, nino, taxYear, mtditid)
    }
  }
}
