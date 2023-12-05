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

import auth.MtdItUser
import enums.JourneyType.{Add, Cease, JourneyType, Manage, Operation}
import models.incomeSourceDetails.{AddIncomeSourceData, CeaseIncomeSourceData, ManageIncomeSourceData, SensitiveUIJourneySessionData, UIJourneySessionData}
import play.api.mvc.{RequestHeader, Result}
import repositories.{UIJourneySensitiveSessionDataRepository, UIJourneySessionDataRepository}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class SessionService @Inject()(
                                encryptionService: EncryptionService,
                                uiJourneySessionDataRepository: UIJourneySessionDataRepository,
                                uIJourneySensitiveSessionDataRepository: UIJourneySensitiveSessionDataRepository
                              ) {

  def getMongo(journeyType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[UIJourneySessionData]]] = {
    uiJourneySessionDataRepository.get(hc.sessionId.get.value, journeyType) map {
      case Some(data: UIJourneySessionData) =>
        Right(Some(data))
      case None => Right(None)
    }
  }

  def getMongoSensitive(journeyType: JourneyType)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[UIJourneySessionData]]] = {
    uIJourneySensitiveSessionDataRepository.get(hc.sessionId.get.value, journeyType.toString) map {
      case Some(data: SensitiveUIJourneySessionData) =>
        Right(Some(data.decrypted))
      case None => Right(None)
    }
  }

  def createSession(journeyType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    setMongoData(UIJourneySessionData(hc.sessionId.get.value, journeyType, None))
  }

  def createSessionSensitive(journeyType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    setMongoDataSensitive(UIJourneySessionData(hc.sessionId.get.value, journeyType, None))
  }

  private def getKeyFromObject[A](objectOpt: Option[Any], key: String): Either[Throwable, Option[A]] = {
    objectOpt match {
      case Some(obj) =>
        val field = obj.getClass.getDeclaredField(key)
        field.setAccessible(true)
        try {
          Right(field.get(obj).asInstanceOf[Option[A]])
        } catch {
          case err: ClassCastException => Left(err)
        }
      case None => Right(None)
    }
  }

  def getMongoKey(key: String, journeyType: JourneyType)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {

    uiJourneySessionDataRepository.get(hc.sessionId.get.value, journeyType.toString) map {
      case Some(data: UIJourneySessionData) =>
        journeyType.operation match {
          case Add => getKeyFromObject[String](data.addIncomeSourceData, key).map(_.map(encryptionService.decryptSessionValue)) // DECRYPT VALUE
          case Manage => getKeyFromObject[String](data.manageIncomeSourceData, key)
          case Cease => getKeyFromObject[String](data.ceaseIncomeSourceData, key)
        }
      case None => Right(None)
    }
  }

  def getMongoKeyTyped[A](key: String, journeyType: JourneyType)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[A]]] = {
    uiJourneySessionDataRepository.get(hc.sessionId.get.value, journeyType.toString) map {
      case Some(data: UIJourneySessionData) =>
        journeyType.operation match {
          case Add => getKeyFromObject[A](data.addIncomeSourceData, key)
          case Manage => getKeyFromObject[A](data.manageIncomeSourceData, key)
          case Cease => getKeyFromObject[A](data.ceaseIncomeSourceData, key)
        }
      case None => Right(None)
    }
  }

  def setMongoData(uiJourneySessionData: UIJourneySessionData)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    uiJourneySessionDataRepository.set(uiJourneySessionData)
  }

  def setMongoDataSensitive(uiJourneySessionData: UIJourneySessionData)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    uIJourneySensitiveSessionDataRepository.set(uiJourneySessionData.encrypted)
  }

  def setMongoKey(key: String, value: String, journeyType: JourneyType)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Boolean]] = {
    val uiJourneySessionData = UIJourneySessionData(hc.sessionId.get.value, journeyType.toString)
    val jsonAccessorPath = journeyType.operation match {
      case Add => AddIncomeSourceData.getJSONKeyPath(key)
      case Manage => ManageIncomeSourceData.getJSONKeyPath(key)
      case Cease => CeaseIncomeSourceData.getJSONKeyPath(key)
    }
    uiJourneySessionDataRepository.updateData(
      uiJourneySessionData,
      jsonAccessorPath,
      encryptionService.encryptSessionValue(value) // ENCRYPT VALUE
    ).map(
      _.wasAcknowledged() match {
        case true => Right(true)
        case false => Left(new Exception("Mongo Save data operation was not acknowledged"))
      }
    )
  }

  def deleteMongoData(journeyType: JourneyType)
                     (implicit hc: HeaderCarrier): Future[Boolean] = {
    uiJourneySessionDataRepository.deleteOne(UIJourneySessionData(hc.sessionId.get.value, journeyType.toString))
  }

  def deleteMongoDataSensitive(journeyType: JourneyType)
                              (implicit hc: HeaderCarrier): Future[Boolean] = {
    uIJourneySensitiveSessionDataRepository.deleteOne(UIJourneySessionData(hc.sessionId.get.value, journeyType.toString).encrypted)
  }

  def deleteSession(operation: Operation)(implicit hc: HeaderCarrier): Future[Boolean] = {
    uiJourneySessionDataRepository.deleteJourneySession(hc.sessionId.get.value, operation)
  }

  def deleteSessionSensitive(operation: Operation)(implicit hc: HeaderCarrier): Future[Boolean] = {
    uIJourneySensitiveSessionDataRepository.deleteJourneySession(hc.sessionId.get.value, operation)
  }
}