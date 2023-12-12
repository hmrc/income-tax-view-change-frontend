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

import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import enums.JourneyType._
import models.incomeSourceDetails.{AddJourneyPath, _}
import repositories.UIJourneySessionDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
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

  private def resolveJourneyType[T](implicit tag: TypeTag[T],
                                    incomeSourceType: IncomeSourceType): JourneyType = {
    if (typeOf[T] <:< typeOf[AddJourneyPath]) {
      JourneyType(Add, incomeSourceType)
    } else if (typeOf[T] <:< typeOf[ManageJourneyPath]) {
      JourneyType(Manage, incomeSourceType)
    } else if (typeOf[T] <:< typeOf[CeasedJourneyPath]) {
      JourneyType(Cease, incomeSourceType)
    } else {
      throw new Error(s"Unable to detect journey type: ${tag.tpe}")
    }
  }

  private def mongoObjectToAddResponse[T: TypeTag](obj: UIJourneySessionData): Option[AddJourneyPath] = {
    typeOf[T] match {
      case x if x == typeOf[AddBusinessNameResponse] =>
        obj.addIncomeSourceData
          .flatMap(add => add.businessName)
          .map(x => AddBusinessNameResponse(name = x))
      case x if x == typeOf[AddBusinessTradeResponse] =>
        obj.addIncomeSourceData
          .flatMap(add => add.businessTrade)
          .map(x => AddBusinessTradeResponse(name = x))
      case x if x == typeOf[AddDateStartedResponse] =>
        obj.addIncomeSourceData
          .flatMap(add => add.dateStarted)
          .map(x => AddDateStartedResponse(date = x))
      case x if x == AddDateStartedAndAccMethodResponse =>
        obj.addIncomeSourceData
          .map(add => (add.dateStarted, add.incomeSourcesAccountingMethod))
          .map(pr => AddDateStartedAndAccMethodResponse(date = pr._1, accMethod = pr._2))
      case _ =>
        throw new Error(s"Mapping not supported for type:")
    }
  }

  private def mongoObjectToManageResponse[T](obj: UIJourneySessionData)
                                            (implicit tag: TypeTag[T]): Option[ManageJourneyPath] = ???

  private def mongoObjectToCeaseResponse[T](obj: UIJourneySessionData)
                                           (implicit tag: TypeTag[T]): Option[ManageJourneyPath] = ???

  def getMongoKeyTyped[A]()(implicit hc: HeaderCarrier, incomeSourceType: IncomeSourceType,
                            ec: ExecutionContext, tag: TypeTag[A]): Future[Either[Error, Option[A]]] = {
    val journeyType = resolveJourneyType[A]
    uiJourneySessionDataRepository.get(hc.sessionId.get.value, journeyType.toString) map {
      case Some(data: UIJourneySessionData) if (typeOf[A] <:< typeOf[AddJourneyPath]) =>
        Right(mongoObjectToAddResponse[A](data).asInstanceOf[Option[A]])
      case Some(data: UIJourneySessionData) if (typeOf[A] <:< typeOf[ManageJourneyPath]) =>
        Right(mongoObjectToManageResponse[A](data).asInstanceOf[Option[A]])
      case Some(data: UIJourneySessionData) if (journeyType.operation == Cease) =>
        Right(mongoObjectToCeaseResponse[A](data).asInstanceOf[Option[A]])
      case _ =>
        Left(new Error(s"Type is not supported => ${tag.tpe}"))
    }
  }

  def setMongoData(uiJourneySessionData: UIJourneySessionData)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    uiJourneySessionDataRepository.set(uiJourneySessionData)
  }

  private def updateMongoAddData[T](request: T,
                                    data: UIJourneySessionData)
                                   (implicit tag: TypeTag[T]): Either[Error, UIJourneySessionData] = {
    typeOf[T] match {
      case x if x == typeOf[AddBusinessNameResponse] =>
        val obj: AddBusinessNameResponse = request.asInstanceOf[AddBusinessNameResponse]
        Right(data.copy(
            addIncomeSourceData = data.addIncomeSourceData match {
              case Some(ad) => Some(ad.copy(businessName = Some(obj.name)))
              case None => Some(AddIncomeSourceData(businessName = Some(obj.name)))
            }
          )
        )

      case x if x == typeOf[AddBusinessTradeResponse] =>
        val obj: AddBusinessTradeResponse = request.asInstanceOf[AddBusinessTradeResponse]
        Right(data.copy(
          addIncomeSourceData = data.addIncomeSourceData match {
            case Some(ad) => Some(ad.copy(businessTrade = Some(obj.name)))
            case None => Some(AddIncomeSourceData(businessTrade = Some(obj.name)))
          }
        )
        )
      case _ =>
        Left(new Error(s"Mapping not supported for type:"))
    }
  }


  def setMongoKeyTyped[T](request: T)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext, tag: TypeTag[T],
                          incomeSourceType: IncomeSourceType): Future[Boolean] = {
    val journeyType = resolveJourneyType[T]
    val res = uiJourneySessionDataRepository.get(hc.sessionId.get.value, journeyType.toString) map {
      case Some(data: UIJourneySessionData) if (typeOf[T] <:< typeOf[AddJourneyPath]) =>
        val updateData = updateMongoAddData[T](request, data).toOption.get
        setMongoData(updateData)
      // TODO: extend for the others journeys
      case _ =>
        throw new Error(s"Not supported type: ${tag.tpe}")
    }
    res.flatten
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
        case true =>
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