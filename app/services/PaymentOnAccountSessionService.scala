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

import config.FrontendAppConfig
import models.paymentOnAccount.PoAAmmendmentData
import repositories.PoAAmmendmentDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentOnAccountSessionService @Inject()(
                                                poAAmmendmentDataRepository: PoAAmmendmentDataRepository) {

  def createSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    setMongoData(PoAAmmendmentData(hc.sessionId.get.value))
  }

  def getMongo(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[PoAAmmendmentData]]] = {
    poAAmmendmentDataRepository.get(hc.sessionId.get.value) map {
      case Some(data: PoAAmmendmentData) =>
        Right(Some(data))
      case None => Right(None)
    }
  }

  def getMongoKey(key: String)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {
    poAAmmendmentDataRepository.get(hc.sessionId.get.value) map {
      case Some(data: PoAAmmendmentData) =>
        val field = data.getClass.getDeclaredField(key)
        field.setAccessible(true)
        try {
          Right(field.get(data).asInstanceOf[Option[String]])
        } catch {
          case err: ClassCastException => Left(err)
        }
      case None => Right(None)
    }
  }

  def setMongoData(poAAmmendmentData: PoAAmmendmentData): Future[Boolean] = {
    poAAmmendmentDataRepository.set(poAAmmendmentData)
  }

}
