/*
 * Copyright 2024 HM Revenue & Customs
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

package actors

import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import org.apache.pekko.actor.{Actor, ActorRef, Props}
import org.apache.pekko.event.Logging
import org.apache.pekko.util.Timeout
import play.api.Logger
import play.api.http.Status
import services.CalculationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object CalculationPoolingActor {
  def props(calculationService: CalculationService)
           (implicit ec: ExecutionContext): Props = Props(new CalculationPoolingActor(calculationService))

  // TODO: check if these are required at all ?
  case object WaitForMeBlank
  case object WaitComplete

  case class OriginalParams(endTimeForEachInterval: Long,
                            endTimeInMillis: Long,
                            calcId: String,
                            nino: String,
                            taxYear: Int,
                            mtditid: String, hc: HeaderCarrier)

  case class GetCalculationRequest(originalParams: OriginalParams, originalSender: Option[ActorRef] = None)

  case class GetCalculationResponse(responseCode: Int, originalParams: OriginalParams, originalSender: ActorRef)
}

import org.apache.pekko.pattern.{ ask, pipe }

class CalculationPoolingActor(val calculationService: CalculationService)
                             (implicit ec: ExecutionContext) extends Actor {
  import CalculationPoolingActor._

  private var counter : Int = 1

  private lazy val retryableStatusCodes: List[Int] = List(Status.BAD_GATEWAY, Status.NOT_FOUND)
  private val logger = Logging(context.system, this)

  def receive: Receive = {
    case WaitForMeBlank =>
      // TODO: specify original wait internal
      logger.info(s"[CalculationPoolingActor][WaitForMeBlank]")
      context.system.scheduler.scheduleOnce(500.milliseconds, self, WaitComplete)
    case WaitComplete =>
      self ! ()

    case GetCalculationRequest(origin, originalSender) =>
        // TODO: set up timeout
        logger.info(s"[CalculationPoolingActor][GetCalculationRequest] - call counter => $counter")
        counter += 1
        val futureResult = {
          getCalculationResponse(origin.endTimeForEachInterval,
            origin.endTimeInMillis,
            origin.calcId,
            origin.nino,
            origin.taxYear,
            origin.mtditid)(origin.hc)
        }.recover { _ =>
            Status.BAD_GATEWAY
          }
          .map(code => GetCalculationResponse(responseCode = code,
            originalParams = origin, originalSender.getOrElse(sender())) )
      pipe(futureResult) to self

    case GetCalculationResponse(responseCode, origin, originalSender) if retryableStatusCodes.contains(responseCode) =>
      logger.info(s"[CalculationPoolingActor][GetCalculationRequest] - re-try")
      implicit val timeout : Timeout = 1.second
      pipe({
        self ? GetCalculationRequest(origin)
      }.recover{ _ => // attempt to intercept Future timeout
        GetCalculationResponse(responseCode = Status.BAD_GATEWAY, originalParams = origin, originalSender= originalSender)
      }) to self

    case GetCalculationResponse(responseCode, originalParams, originalSender) =>
      logger.info(s"[CalculationPoolingActor][GetCalculationRequest] - job done")
      originalSender ! GetCalculationResponse(responseCode = responseCode, originalParams = originalParams, originalSender = originalSender)
  }

  private def getCalculationResponse(endTimeForEachInterval: Long,
                                     endTimeInMillis: Long,
                                     calcId: String,
                                     nino: String,
                                     taxYear: Int,
                                     mtditid: String
                                    )
                                    (implicit hc: HeaderCarrier): Future[Int] = {

    Logger("application").debug(s"[CalculationPollingActor][getCalculationResponse] " +
      s"Starting polling for  calcId: $calcId and nino: $nino")
    for {
      result <- calculationService.getLatestCalculation(mtditid, nino, calcId, taxYear).map {
        case _: LiabilityCalculationResponse => Status.OK
        case error: LiabilityCalculationError =>
          if (System.currentTimeMillis() > endTimeInMillis) Status.INTERNAL_SERVER_ERROR
          else error.status
      }
    } yield result
  }

}
