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
import actors.CalculationPoolingActor.{GetCalculationRequestAwait, GetCalculationResponse, OriginalParams}
import config.FrontendAppConfig
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import play.api.Logger
import play.api.http.Status
import services.helpers.CalculationServiceHelper
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
                                         (implicit ec: ExecutionContext) extends CalculationServiceHelper {

  private lazy val calculationPoolingActor = system.actorOf(CalculationPoolingActor.props(calculationService),
    "CalculationPoolingActor-actor")

  lazy val lockService: LockService = LockService(
    mongoLockRepository, lockId = "calc-poller",
    ttl = Duration.create(frontendAppConfig.calcPollSchedulerTimeout, MILLISECONDS))

  private lazy val retryableStatusCodes: List[Int] = List(Status.BAD_GATEWAY, Status.NOT_FOUND) //, NO_CONTENT)

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

  private def pollCalcInIntervals(calcId: String,
                                  nino: String,
                                  taxYear: Int,
                                  mtditid: String,
                                  endTimeInMillis: Long)
                                 (implicit hc: HeaderCarrier): Future[Int] = {
    implicit val timeout: Timeout = 5.second
    val params = OriginalParams(
      endTimeForEachInterval = System.currentTimeMillis() + frontendAppConfig.calcPollSchedulerInterval,
      endTimeInMillis = endTimeInMillis,
      calcId = calcId,
      nino = nino,
      taxYear = taxYear,
      mtditid = mtditid,
      hc = hc)
    (calculationPoolingActor ? GetCalculationRequestAwait(params))
      .recover { // TODO: intercept only timeouts?
        _ => GetCalculationResponse(Status.BAD_GATEWAY, originalParams = params, calculationPoolingActor)
      }
      .mapTo[GetCalculationResponse]
      .map(response => response.responseCode)
  }

}
