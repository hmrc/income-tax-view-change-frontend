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

import connectors.RepaymentConnector
import exceptions.MissingFieldException
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepaymentService @Inject()(val repaymentConnector: RepaymentConnector, implicit val ec: ExecutionContext) {

  def start(nino: String, fullAmount: Option[BigDecimal])
           (implicit headerCarrier: HeaderCarrier): Future[Either[Throwable, String]] = {
    Logger("application").debug(s"[RepaymentService][start]: " +
      s"Repayment journey start with nino: $nino and fullAmount: $fullAmount ")
    fullAmount match {
      case Some(amt)=>
        repaymentConnector.start(nino, math.abs(amt.toDouble)).map {
          case RepaymentJourneyModel(nextUrl) =>
            Right(nextUrl)
          case RepaymentJourneyErrorResponse(status, message) =>
            Logger("application").error(s"[RepaymentService][start]: " +
              s"Repayment journey start error with response code: $status and message: $message")
            Left(new InternalError)
          case _ =>
            Logger("application").error(s" [RepaymentService][start]: " +
              s" Repayment journey view error with response code: unknown and message: unknown")
            Left(new InternalError)
        }.recover { case ex: Exception =>
          Logger("application").error(s"[RepaymentService][start]: " +
            s"Repayment journey start error with exception: $ex")
          Left(ex)
        }
      case None =>
        Logger("application").error(s"[RepaymentService][start] " +
          s"AvailableCredit not found")
        Future.successful(Left(new MissingFieldException("availableCredit")))

    }
  }

  def view(nino: String)
          (implicit headerCarrier: HeaderCarrier): Future[Either[Throwable, String]] = {
    Logger("application").debug(s"Repayment journey view with nino: $nino")
    repaymentConnector.view(nino).map {
      case RepaymentJourneyModel(nextUrl) =>
        Right(nextUrl)
      case RepaymentJourneyErrorResponse(status, message) =>
        Logger("application").error(s" [RepaymentService][start]: " +
          s" Repayment journey view error with response code: $status and message: $message")
        Left(new InternalError)
      case _ =>
        Logger("application").error(s" [RepaymentService][start]: " +
          s" Repayment journey view error with response code: unknown and message: unknown")
        Left(new InternalError)
    }.recover { case ex: Exception =>
      Logger("application").error(s"[RepaymentService][start]: " +
        s"Repayment journey view error with exception: $ex")

      Left(ex)
    }
  }

}
