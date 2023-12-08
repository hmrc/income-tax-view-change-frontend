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

import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType._
import models.incomeSourceDetails._
import repositories.UIJourneySessionDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe._

@Singleton
class SessionService @Inject()(uiJourneySessionDataRepository: UIJourneySessionDataRepository) {

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

  private def resolveJourneyType[T](implicit tag: TypeTag[T]): JourneyType = {
    typeOf[T] match {
      case x if x == typeOf[AddBusinessNameResponse] =>
        JourneyType(Add, SelfEmployment)
      case x if x == typeOf[AddBusinessTradeResponse] =>
        JourneyType(Add, SelfEmployment)
      case x =>
        throw new Error(s"Unable to resolve journey by type provided: ${x} - ${x == typeOf[AddBusinessNameResponse]}")
    }
  }


  private def mongoObjectToAddResponse[T: TypeTag](obj: UIJourneySessionData): Option[AddJourneyPath] = {
    if (typeOf[T] == typeOf[AddBusinessNameResponse]) {
      obj.addIncomeSourceData
        .flatMap(add => add.businessName)
        .map(x => AddBusinessNameResponse(name = x))
    } else if (typeOf[T] == typeOf[AddBusinessTradeResponse]) {
      obj.addIncomeSourceData
        .flatMap(add => add.businessTrade)
        .map(x => AddBusinessTradeResponse(name = x))
    } else {
      throw new Error(s"Mapping not supported for type:")
    }
  }

  //  private def mongoObjectToManageResponse[T](obj: UIJourneySessionData)
  //                                         (implicit tag: ClassTag[T]): Option[ManageJourneyPath] = ???

  //  private def mongoObjectToCeaseResponse[T](obj: UIJourneySessionData): Option[ManageJourneyPath] = ???

  def getMongoKeyTyped[A]()(implicit hc: HeaderCarrier,
                            ec: ExecutionContext, tag: TypeTag[A]): Future[Either[Throwable, Option[_]]] = {
    val journeyType = resolveJourneyType[A]
    println(s"Here is journey type: ${journeyType}")
    uiJourneySessionDataRepository.get(hc.sessionId.get.value, journeyType.toString) map {
      case Some(data: UIJourneySessionData)  =>
        println(s"Data ${data.addIncomeSourceData}")
        val x = mongoObjectToAddResponse[A](data)
        Right(x)
      //      case Some(data: UIJourneySessionData) if (journeyType.operation == Manage) =>
      //        Right(mongoObjectToManageResponse[A](data))
      //      case Some(data: UIJourneySessionData) if (journeyType.operation == Cease) =>
      //        Right(mongoObjectToCeaseResponse[A](data))
      case _ => // TODO: raise error
        Right(None)
    }
  }

  def setMongoData(uiJourneySessionData: UIJourneySessionData)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    uiJourneySessionDataRepository.set(uiJourneySessionData)
  }

  def setMongoKey(key: String, value: String, journeyType: JourneyType)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Boolean]] = {
    println(s"Data saving ")
    val uiJourneySessionData = UIJourneySessionData(hc.sessionId.get.value, journeyType.toString)
    val jsonAccessorPath = journeyType.operation match {
      case Add => AddIncomeSourceData.getJSONKeyPath(key)
      case Manage => ManageIncomeSourceData.getJSONKeyPath(key)
      case Cease => CeaseIncomeSourceData.getJSONKeyPath(key)
    }
    uiJourneySessionDataRepository.updateData(uiJourneySessionData, jsonAccessorPath, value).map(
      result => result.wasAcknowledged() match {
        case true =>
          println(s"Data saved ...")
          Right(true)
        case false => Left(new Exception("Mongo Save data operation was not acknowledged"))
      }
    )
  }

  def deleteMongoData(journeyType: JourneyType)
                     (implicit hc: HeaderCarrier): Future[Boolean] = {
    uiJourneySessionDataRepository.deleteOne(UIJourneySessionData(hc.sessionId.get.value, journeyType.toString))
  }

  def deleteSession(operation: Operation)(implicit hc: HeaderCarrier): Future[Boolean] = {
    uiJourneySessionDataRepository.deleteJourneySession(hc.sessionId.get.value, operation)
  }
}