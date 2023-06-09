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

import auth.MtdItUserWithNino
import connectors.IncomeTaxViewChangeConnector
import exceptions.MissingFieldException
import models.incomeSourceDetails.viewmodels._
import models.incomeSourceDetails.{IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

@Singleton
class IncomeSourceDetailsService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                           val cache: AsyncCacheApi) {
  implicit val ec = ExecutionContext.global
  val cacheExpiry: Duration = Duration(1, "day")

  def getCachedIncomeSources(cacheKey: String): Future[Option[IncomeSourceDetailsModel]] = {
    cache.get(cacheKey).map((incomeSources: Option[JsValue]) => {
      incomeSources match {
        case Some(jsonSources) =>
          Json.fromJson[IncomeSourceDetailsModel](jsonSources) match {
            case JsSuccess(sources: IncomeSourceDetailsModel, _: JsPath) =>
              Some(sources)
            case _ => None
          }
        case None => None
      }
    })
  }

  def getIncomeSourceDetails(cacheKey: Option[String] = None)(implicit hc: HeaderCarrier,
                                                              mtdUser: MtdItUserWithNino[_]): Future[IncomeSourceDetailsResponse] = {
    if (cacheKey.isDefined) {
      getCachedIncomeSources(cacheKey.get).flatMap {
        case Some(sources: IncomeSourceDetailsModel) =>
          Logger("application").info(s"incomeSourceDetails cache HIT with ${cacheKey.get}")
          Future.successful(sources)
        case None =>
          Logger("application").info(s"incomeSourceDetails cache MISS with ${cacheKey.get}")
          incomeTaxViewChangeConnector.getIncomeSources().flatMap {
            case incomeSourceDetailsModel: IncomeSourceDetailsModel =>
              cache.set(cacheKey.get, incomeSourceDetailsModel.sanitise.toJson, cacheExpiry).map(
                _ => incomeSourceDetailsModel
              )
            case error: IncomeSourceDetailsResponse => Future.successful(error)
          }
      }
    } else {
      incomeTaxViewChangeConnector.getIncomeSources()
    }
  }

  def getAddIncomeSourceViewModel(sources: IncomeSourceDetailsModel): Try[AddIncomeSourcesViewModel] = Try {
    val soleTraderBusinesses = sources.businesses.filterNot(_.isCeased)
    val ukProperty = sources.properties.find(_.isUkProperty)
    val foreignProperty = sources.properties.find(_.isForeignProperty)
    val ceasedBusinesses = sources.businesses.filter(_.isCeased)

    AddIncomeSourcesViewModel(
      soleTraderBusinesses = soleTraderBusinesses.map { business =>
        BusinessDetailsViewModel(
          business.tradingName,
          business.tradingStartDate)
      },
      ukProperty = ukProperty.map { property =>
        PropertyDetailsViewModel(property.tradingStartDate)
      },
      foreignProperty = foreignProperty.map { property =>
        PropertyDetailsViewModel(property.tradingStartDate)
      },
      ceasedBusinesses = ceasedBusinesses.map { business =>
        CeasedBusinessDetailsViewModel(
          business.tradingName,
          business.tradingStartDate,
          business.cessation.flatMap(_.date).getOrElse(throw MissingFieldException("Cessation Date"))
        )
      }
    )
  }

  def getViewIncomeSourceViewModel(sources: IncomeSourceDetailsModel): Either[Throwable, ViewIncomeSourcesViewModel] = {

    val maybeSoleTraderBusinesses = sources.businesses.filterNot(_.isCeased)
    val soleTraderBusinessesExists = maybeSoleTraderBusinesses.nonEmpty

    val maybeUkProperty = sources.properties.filterNot(_.isCeased).find(_.isUkProperty)
    val ukPropertyExists = maybeUkProperty.nonEmpty

    val maybeForeignProperty = sources.properties.filterNot(_.isCeased).find(_.isForeignProperty)
    val foreignPropertyExists = maybeForeignProperty.nonEmpty

    val maybeCeasedBusinesses = sources.businesses.filter(_.isCeased)
    val ceasedBusinessExists = maybeCeasedBusinesses.nonEmpty

    Try {
      ViewIncomeSourcesViewModel(
        viewSoleTraderBusinesses = if (soleTraderBusinessesExists) {
          maybeSoleTraderBusinesses.map { business =>
            ViewBusinessDetailsViewModel(
              business.tradingName,
              business.tradingStartDate
            )
          }
        } else Nil,
        viewUkProperty = if (ukPropertyExists) {
          Some(ViewPropertyDetailsViewModel(
            maybeUkProperty.flatMap(_.tradingStartDate)
          ))
        } else None,
        viewForeignProperty = if (foreignPropertyExists) {
          Some(ViewPropertyDetailsViewModel(
            maybeForeignProperty.flatMap(_.tradingStartDate)
          ))
        } else None,
        viewCeasedBusinesses = if (ceasedBusinessExists) {
          maybeCeasedBusinesses.map { business =>
            ViewCeasedBusinessDetailsViewModel(
              tradingName = business.tradingName,
              tradingStartDate = business.tradingStartDate,
              cessationDate = business.cessation.flatMap(_.date).getOrElse(throw MissingFieldException("Cessation Date"))
            )
          }
        } else Nil
      )
    }.toEither
  }


  def getCeaseIncomeSourceViewModel(sources: IncomeSourceDetailsModel): Either[Throwable, CeaseIncomeSourcesViewModel] = {

    val maybeSoleTraderBusinesses = sources.businesses.filterNot(_.isCeased)
    val soleTraderBusinessesExists = maybeSoleTraderBusinesses.nonEmpty

    val maybeUkProperty = sources.properties.filterNot(_.isCeased).find(_.isUkProperty)
    val ukPropertyExists = maybeUkProperty.nonEmpty

    val maybeForeignProperty = sources.properties.filterNot(_.isCeased).find(_.isForeignProperty)
    val foreignPropertyExists = maybeForeignProperty.nonEmpty

    val maybeCeasedBusinesses = sources.businesses.filter(_.isCeased)
    val ceasedBusinessExists = maybeCeasedBusinesses.nonEmpty

    Try {
      CeaseIncomeSourcesViewModel(
        soleTraderBusinesses = if (soleTraderBusinessesExists) {
          maybeSoleTraderBusinesses.map { business =>
            CeaseBusinessDetailsViewModel(
              business.tradingName.getOrElse(throw MissingFieldException("Trading Name")),
              business.tradingStartDate.getOrElse(throw MissingFieldException("Trading Start Date"))
            )
          }
        } else Nil,
        ukProperty = if (ukPropertyExists) {
          Some(CeasePropertyDetailsViewModel(
            maybeUkProperty.flatMap(_.tradingStartDate).getOrElse(throw MissingFieldException("UkProperty: Trading Start Date"))
          ))
        } else None,
        foreignProperty = if (foreignPropertyExists) {
          Some(CeasePropertyDetailsViewModel(
            maybeForeignProperty.flatMap(_.tradingStartDate).getOrElse(throw MissingFieldException("ForeignProperty: Trading Start Date"))
          ))
        } else None,
        ceasedBusinesses = if (ceasedBusinessExists) {
          maybeCeasedBusinesses.map { business =>
            CeaseCeasedBusinessDetailsViewModel(
              tradingName = business.tradingName.getOrElse(throw MissingFieldException("CeasedBusiness: Trading Name")),
              tradingStartDate = business.tradingStartDate.getOrElse(throw MissingFieldException("CeasedBusiness: Trading Start Date")),
              cessationDate = business.cessation.flatMap(_.date).getOrElse(throw MissingFieldException("Cessation Date"))
            )
          }
        } else Nil
      )
    }.toEither
  }
}

