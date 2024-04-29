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

import cats.data.EitherT
import models.paymentOnAccount.{PoAAmendmentData, PoASessionData}
import repositories.PoAAmendmentDataRepository
import uk.gov.hmrc.http.HeaderCarrier

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


  def setAdjustmentReason(poaAdjustmentReason: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Unit]] = {
    poAAmmendmentDataRepository.get(hc.sessionId.get.value).flatMap {
      case Some(data: PoASessionData) =>
        data.poaAmendmentData match {
          case Some(value) =>
            setMongoData(Some(value.copy(poaAdjustmentReason = Some(poaAdjustmentReason))))
              .flatMap(v =>
              {
                if (v)
                  Future.successful(Right(()))
                else {
                  Future.successful( Left(new Error("Unable to save records")) )
                }
              })
          case None =>
            Future.successful( Left(new Error("Record not found in mongo: 1")) )
        }
      case None =>
        Future.successful( Left(new Error("Record not found in mongo: 2")) )
    }
  }

  def setAdjustmentReason2(poaAdjustmentReason2: String)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Unit]] = {
    {
      for {
        res <- EitherT(poAAmmendmentDataRepository.get2(hc.sessionId.get.value))
        res2 <- {
          val e: PoASessionData = setPoaAdjustmentField(poaAdjustmentReason2, res)
          EitherT(poAAmmendmentDataRepository.set2(e))
        }
      } yield res2
    }
  }.value

  private def setPoaAdjustmentField(poaAdjustmentReason2: String, res: PoASessionData) = {
    val e = PoASessionData(
      sessionId = res.sessionId,
      poaAmendmentData =
        res.poaAmendmentData.map(r =>
          r.copy(poaAdjustmentReason = Some(poaAdjustmentReason2))
        )
    )
    e
  }
}
