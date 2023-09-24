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
import enums.IncomeSourceJourney.{AddJourney, JourneyType}
import models.incomeSourceDetails.UIJourneySessionData
import play.api.Logger
import play.api.libs.json.JsResult.Exception
import play.api.mvc.{RequestHeader, Result}
import repositories.UIJourneySessionDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.Right
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.Exception

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

  def createSession(journeyType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    setMongoData(UIJourneySessionData(hc.sessionId.get.value, journeyType, None))
  }

  def getMongoKey(key: String, journeyType: JourneyType)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {
    println(Console.RED + hc.sessionId.get.value + " journey type: " + journeyType + Console.WHITE)
    uiJourneySessionDataRepository.get(hc.sessionId.get.value, journeyType.toString) map {
      case Some(data: UIJourneySessionData) =>
        println(Console.RED + data + Console.WHITE)
//        journeyType.journeyOperation match {
//          case AddJourney => {
//            data.addIncomeSourceData.fold(Right(Some("")))(
//              addIncomeSourceData => {
//                val field = addIncomeSourceData.getClass.getDeclaredField(key)
//                field.setAccessible(true)
//                val value = field.get(addIncomeSourceData).asInstanceOf[Option[String]]
//                println(Console.RED + "got value: " + value + Console.WHITE)
//                Right(value)
//
//              })
//          }
//          case _ => Right(Some("asdf"))
//        }
        if (journeyType.journeyOperation == AddJourney && data.addIncomeSourceData.isDefined) {
          val field = data.addIncomeSourceData.get.getClass.getDeclaredField(key)
          field.setAccessible(true)
          val value = field.get(data.addIncomeSourceData.get).asInstanceOf[Option[String]]
          println(Console.RED + "got value: " + value + Console.WHITE)
          Right(value)
        } else {
          Right(Some(""))
        }

      case None => Right(None)
    }
  }


  def set(key: String, value: String, result: Result)(implicit ec: ExecutionContext, request: RequestHeader): Future[Either[Throwable, Result]] = {
    Future {
      Right(result.addingToSession(key -> value))
    }
  }

  def setMongoData(uiJourneySessionData: UIJourneySessionData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    uiJourneySessionDataRepository.set(uiJourneySessionData)
  }

  def setMongoKey(key: String, value: String, journeyType: JourneyType)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Boolean]] = {
    val uiJourneySessionData = UIJourneySessionData(hc.sessionId.get.value, journeyType.toString, None)
    uiJourneySessionDataRepository.updateData(uiJourneySessionData, "addIncomeSourceData." + key, value).map(
      result => {
        println(Console.RED + result.wasAcknowledged() + Console.WHITE)
        Right(result.wasAcknowledged())
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
}

