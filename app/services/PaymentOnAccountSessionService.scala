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

import models.claimToAdjustPoa.{PoaAmendmentData, PoaSessionData, SelectYourReason}
import repositories.PoaAmendmentDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentOnAccountSessionService @Inject() (poaAmendmentDataRepository: PoaAmendmentDataRepository) {

  def createSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Unit]] = {
    setMongoData(Some(PoaAmendmentData())).flatMap { res =>
      if (res)
        Future.successful(Right(()))
      else
        Future.successful(Left(new Error("Unable to create mongo session")))
    }
  }

  def getMongo(
      implicit hc: HeaderCarrier,
      ec:          ExecutionContext
    ): Future[Either[Throwable, Option[PoaAmendmentData]]] = {
    poaAmendmentDataRepository.get(hc.sessionId.get.value) map {
      case Some(data: PoaSessionData) =>
        Right(data.poaAmendmentData)
      case None => Right(None)
    }
  }

  def setMongoData(poaAmendmentData: Option[PoaAmendmentData])(implicit hc: HeaderCarrier): Future[Boolean] = {
    poaAmendmentDataRepository.set(PoaSessionData(hc.sessionId.get.value, poaAmendmentData))
  }

  def setAdjustmentReason(
      poaAdjustmentReason: SelectYourReason
    )(
      implicit hc: HeaderCarrier,
      ec:          ExecutionContext
    ): Future[Either[Throwable, Unit]] = {
    poaAmendmentDataRepository.get(hc.sessionId.get.value).flatMap {
      case Some(data: PoaSessionData) =>
        val newData: PoaAmendmentData = data.poaAmendmentData match {
          case Some(value) =>
            value.copy(poaAdjustmentReason = Some(poaAdjustmentReason))
          case None =>
            PoaAmendmentData(poaAdjustmentReason = Some(poaAdjustmentReason))
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

  def setNewPoaAmount(
      newPoaAmount: BigDecimal
    )(
      implicit hc: HeaderCarrier,
      ec:          ExecutionContext
    ): Future[Either[Throwable, Unit]] = {
    poaAmendmentDataRepository.get(hc.sessionId.get.value).flatMap {
      case Some(data: PoaSessionData) =>
        val newData: PoaAmendmentData = data.poaAmendmentData match {
          case Some(value) =>
            value.copy(newPoaAmount = Some(newPoaAmount))
          case None =>
            PoaAmendmentData(newPoaAmount = Some(newPoaAmount))
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

  def setCompletedJourney(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Unit]] = {
    poaAmendmentDataRepository.get(hc.sessionId.get.value).flatMap {
      case Some(data: PoaSessionData) =>
        val newData: PoaAmendmentData = data.poaAmendmentData match {
          case Some(value) =>
            value.copy(journeyCompleted = true)
          case None =>
            PoaAmendmentData(journeyCompleted = true)
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
