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
import enums.JourneyType._
import models.UIJourneySessionData
import models.incomeSourceDetails.{AddIncomeSourceData, CeaseIncomeSourceData, ManageIncomeSourceData}
import repositories.{SensitiveUIJourneySessionDataRepository, UIJourneySessionDataRepository}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionService @Inject()(
                                uiJourneySessionDataRepository: UIJourneySessionDataRepository,
                                sensitiveUIJourneySessionDataRepository: SensitiveUIJourneySessionDataRepository,
                                config: FrontendAppConfig
                              ) {

  def getMongo(journeyType: JourneyType)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[UIJourneySessionData]]] = {
    hc.sessionId.map(_.value) match {
      case Some(sessionId) if config.encryptionIsEnabled =>
        sensitiveUIJourneySessionDataRepository.get(sessionId, journeyType).map(data => Right(data))
      case Some(sessionId) =>
        uiJourneySessionDataRepository.get(sessionId, journeyType).map(data => Right(data))
      case _ =>
        Future.successful(Left(new Exception("Missing sessionId in HeaderCarrier")))
    }
  }

  def createSession(journeyType: JourneyType)(implicit hc: HeaderCarrier): Future[Boolean] = {
    setMongoData(UIJourneySessionData(hc.sessionId.get.value, journeyType.toString, None))
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
      case None =>
        Right(None)
    }
  }

  def getMongoKey(key: String, incomeSources: IncomeSourceJourneyType)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {
    uiJourneySessionDataRepository.get(hc.sessionId.get.value, incomeSources) map {
      case Some(data: UIJourneySessionData) =>
        incomeSources.operation match {
          case Add => getKeyFromObject[String](data.addIncomeSourceData, key)
          case Manage => getKeyFromObject[String](data.manageIncomeSourceData, key)
          case Cease => getKeyFromObject[String](data.ceaseIncomeSourceData, key)
        }
      case None =>
        Right(None)
    }
  }

  def getMongoKeyTyped[A](key: String, incomeSources: IncomeSourceJourneyType)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[A]]] = {
    uiJourneySessionDataRepository.get(hc.sessionId.get.value, incomeSources) map {
      case Some(data: UIJourneySessionData) =>
        incomeSources.operation match {
          case Add => getKeyFromObject[A](data.addIncomeSourceData, key)
          case Manage => getKeyFromObject[A](data.manageIncomeSourceData, key)
          case Cease => getKeyFromObject[A](data.ceaseIncomeSourceData, key)

        }
      case None => Right(None)
    }
  }

  def setMongoData(uiJourneySessionData: UIJourneySessionData): Future[Boolean] = {
    if (config.encryptionIsEnabled)
      sensitiveUIJourneySessionDataRepository.set(uiJourneySessionData)
    else
      uiJourneySessionDataRepository.set(uiJourneySessionData)
  }

  def setMultipleMongoData(keyAndValue: Map[String, String], incomeSources: IncomeSourceJourneyType)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Boolean]] = {
    val uiJourneySessionData = UIJourneySessionData(hc.sessionId.get.value, incomeSources.toString)
    val keyValueToUpdate = keyAndValue.map { case (key, value) =>
      incomeSources.operation match {
        case Add => (AddIncomeSourceData.getJSONKeyPath(key), value)
        case Manage => (ManageIncomeSourceData.getJSONKeyPath(key), value)
        case Cease => (CeaseIncomeSourceData.getJSONKeyPath(key), value)
      }
    }
    uiJourneySessionDataRepository.updateMultipleData(uiJourneySessionData, keyValueToUpdate)
      .map(
        result => result.wasAcknowledged() match {
          case true => Right(true)
          case false => Left(new Exception("Mongo Save data operation was not acknowledged"))
        })
  }

  def setMongoKey(key: String, value: String, incomeSources: IncomeSourceJourneyType)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Boolean]] = {
    val uiJourneySessionData = UIJourneySessionData(hc.sessionId.get.value, incomeSources.toString)
    val jsonAccessorPath = incomeSources.operation match {
      case Add => AddIncomeSourceData.getJSONKeyPath(key)
      case Manage => ManageIncomeSourceData.getJSONKeyPath(key)
      case Cease => CeaseIncomeSourceData.getJSONKeyPath(key)
    }
    uiJourneySessionDataRepository.updateData(uiJourneySessionData, jsonAccessorPath, value).map(
      result => result.wasAcknowledged() match {
        case true => Right(true)
        case false => Left(new Exception("Mongo Save data operation was not acknowledged"))
      }
    )
  }

  def deleteMongoData(journeyType: JourneyType)
                     (implicit hc: HeaderCarrier): Future[Boolean] = {
    if (config.encryptionIsEnabled)
      sensitiveUIJourneySessionDataRepository.deleteOne(UIJourneySessionData(hc.sessionId.get.value, journeyType.toString))
    else
      uiJourneySessionDataRepository.deleteOne(UIJourneySessionData(hc.sessionId.get.value, journeyType.toString))
  }

  def deleteSession(operation: Operation)(implicit hc: HeaderCarrier): Future[Boolean] = {
    if (config.encryptionIsEnabled)
      sensitiveUIJourneySessionDataRepository.deleteJourneySession(hc.sessionId.get.value, operation)
    else
      uiJourneySessionDataRepository.deleteJourneySession(hc.sessionId.get.value, operation)
  }

  def clearSession(sessionId: String)(implicit ec: ExecutionContext): Future[Unit] = {
    uiJourneySessionDataRepository.clearSession(sessionId).flatMap {
      case true => Future.successful(())
      case false => Future.failed(new Exception("failed to clear session"))
    }
  }
}
