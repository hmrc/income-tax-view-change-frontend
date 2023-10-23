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
import enums.JourneyType.{Add, Cease, JourneyType, Manage}
import models.incomeSourceDetails.{AddIncomeSourceData, CeaseIncomeSourceData, ManageIncomeSourceData, UIJourneySessionData}
import play.api.mvc.{RequestHeader, Result}
import repositories.UIJourneySessionDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionService @Inject()(uiJourneySessionDataRepository: UIJourneySessionDataRepository) {

  def get(key: String)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {
    Future {
      Right(user.session.get(key))
    }
  }

  def getMongo(journeyType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[UIJourneySessionData]]] = {
    uiJourneySessionDataRepository.get(hc.sessionId.get.value, journeyType) map {
      case Some(data: UIJourneySessionData) =>
        Right(Some(data))
      case None => Right(None)
    }
  }

  def createSession(journeyType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    setMongoData(UIJourneySessionData(hc.sessionId.get.value, journeyType, None))
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
          case Add => getKeyFromObject[String](data.addIncomeSourceData, key)
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

  def set(key: String, value: String, result: Result)
         (implicit ec: ExecutionContext, request: RequestHeader): Future[Either[Throwable, Result]] = {
    Future {
      Right(result.addingToSession(key -> value))
    }
  }

  def setMongoData(uiJourneySessionData: UIJourneySessionData)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    uiJourneySessionDataRepository.set(uiJourneySessionData)
  }

  def setMongoKey(key: String, value: String, journeyType: JourneyType)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Boolean]] = {
    val uiJourneySessionData = UIJourneySessionData(hc.sessionId.get.value, journeyType.toString)
    val jsonAccessorPath = journeyType.operation match {
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

  def setList(result: Result, keyValue: (String, String)*)(implicit ec: ExecutionContext, request: RequestHeader): Future[Either[Throwable, Result]] = {
    Future {
      Right(result.addingToSession(keyValue: _*))
    }
  }

  def set(result: Result, keyValue: (String, String)*)(implicit ec: ExecutionContext, request: RequestHeader): Future[Either[Throwable, Result]] = {
    Future {
      Right(result.addingToSession(keyValue: _*))
    }
  }

  def remove(keys: Seq[String], result: Result)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Result]] = {
    Future {
      val newSession = user.session -- keys
      Right(
        result.withSession(newSession)
      )
    }
  }

  def deleteMongoData(journeyType: JourneyType)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    uiJourneySessionDataRepository.deleteOne(UIJourneySessionData(hc.sessionId.get.value, journeyType.toString))
  }

  def deleteSession(implicit hc: HeaderCarrier): Future[Boolean] = {
    uiJourneySessionDataRepository.deleteOne(hc.sessionId.get.value)
  }
}