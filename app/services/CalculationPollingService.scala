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

import actors.CalculationPoolingActor
import actors.CalculationPoolingActor.{GetCalculationRequest, GetCalculationResponse, OriginalParams, WaitForMeBlank}
import config.FrontendAppConfig
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
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
                                          val calculationService: CalculationService,
                                          system: ActorSystem)
                                         (implicit ec: ExecutionContext) {

  private lazy val calculationPoolingActor = system.actorOf( CalculationPoolingActor.props(calculationService),
    "CalculationPoolingActor-actor")

  lazy val lockService: LockService = LockService(
    mongoLockRepository, lockId = "calc-poller",
    ttl = Duration.create(frontendAppConfig.calcPollSchedulerTimeout, MILLISECONDS))

  private lazy val retryableStatusCodes: List[Int] = List(Status.BAD_GATEWAY, Status.NOT_FOUND)

  def initiateCalculationPollingSchedulerWithMongoLock(calcId: String, nino: String, taxYear: Int, mtditid: String)
                                                      (implicit headerCarrier: HeaderCarrier): Future[Any] = {


    val endTimeInMillis: Long = System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerTimeout
    //Acquire Mongo lock and call Calculation service
    //To avoid wait time for first call, calling getCalculationResponse with end time as current time
    lockService.withLock {
      getCalculationResponse(System.currentTimeMillis(), endTimeInMillis, calcId, nino, taxYear, mtditid)
    } flatMap {
      case Some(statusCode) =>
        Logger("application").debug("[CalculationPollingService] Response received from Calculation service")
        if (!retryableStatusCodes.contains(statusCode)) Future.successful(statusCode)
        else pollCalcInIntervals(calcId, nino, taxYear, mtditid, endTimeInMillis)
      case None =>
        Logger("application").debug("[CalculationPollingService] Failed to acquire Mongo lock")
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

    Logger("application").debug(s"[CalculationPollingService][getCalculationResponse] " +
      s"Starting polling for  calcId: $calcId and nino: $nino")

    implicit val timeout : Timeout = 1.second
    for {
      _ <- ask(calculationPoolingActor, WaitForMeBlank).mapTo[Unit]
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
    // TODO: main timeout???
    implicit val timeout : Timeout = 25.second
    val originalParams = OriginalParams(
      endTimeForEachInterval = System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerInterval,
      endTimeInMillis = endTimeInMillis,
      calcId = calcId,
      nino = nino,
      taxYear = taxYear,
      mtditid = mtditid,
      hc = hc)
    (calculationPoolingActor ? GetCalculationRequest(originalParams))
      .mapTo[GetCalculationResponse]
      .map(response => response.responseCode)
  }
}
