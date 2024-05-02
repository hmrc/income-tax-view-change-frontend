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

package services

import models.paymentOnAccount.{PoAAmendmentData, PoASessionData}
import repositories.PoAAmendmentDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.adjustPoa.checkAnswers.SelectYourReason

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentOnAccountSessionService @Inject()(poAAmmendmentDataRepository: PoAAmendmentDataRepository) {

  def createSession(implicit hc: HeaderCarrier): Future[Boolean] = {
    setMongoData(None)
  }

  def getMongo(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[PoAAmendmentData]]] = {
    poAAmmendmentDataRepository.get(hc.sessionId.get.value) map {
      case Some(data: PoASessionData) =>
        Right(data.poaAmendmentData)
      case None => Right(None)
    }
  }

  def setMongoData(poAAmmendmentData: Option[PoAAmendmentData])(implicit hc: HeaderCarrier): Future[Boolean] = {
    poAAmmendmentDataRepository.set(PoASessionData(hc.sessionId.get.value, poAAmmendmentData))
  }

  def setAdjustmentReason(poaAdjustmentReason: SelectYourReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Unit]] = {
    poAAmmendmentDataRepository.get(hc.sessionId.get.value).flatMap {
      case Some(data: PoASessionData) =>
        val newData: PoAAmendmentData = data.poaAmendmentData match {
          case Some(value) =>
            value.copy(poaAdjustmentReason = Some(poaAdjustmentReason))
          case None =>
            PoAAmendmentData(poaAdjustmentReason = Some(poaAdjustmentReason))
        }
        setMongoData(Some(newData))
          .flatMap(v => {
            if (v)
              Future.successful(Right(()))
            else {
              Future.successful(Left(new Error("Unable to save records")))
            }
          })
      case None =>
        Future.successful(Left(new Error("No active mongo session found")))
    }
  }

  def setNewPoAAmount(newPoAAmount: BigDecimal)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Unit]] = {
    poAAmmendmentDataRepository.get(hc.sessionId.get.value).flatMap {
      case Some(data: PoASessionData) =>
        val newData: PoAAmendmentData = data.poaAmendmentData match {
          case Some(value) =>
            value.copy(newPoAAmount = Some(newPoAAmount))
          case None =>
            PoAAmendmentData(newPoAAmount = Some(newPoAAmount))
        }
        setMongoData(Some(newData))
          .flatMap(v => {
            if (v)
              Future.successful(Right(()))
            else {
              Future.successful(Left(new Error("Unable to save records")))
            }
          })
      case None =>
        Future.successful(Left(new Error("No active mongo session found")))
    }
  }

}
