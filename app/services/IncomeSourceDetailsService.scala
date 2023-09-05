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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import exceptions.MissingFieldException
import models.core.AddressModel
import models.incomeSourceDetails.viewmodels._
import models.incomeSourceDetails.{IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class IncomeSourceDetailsService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                           val cache: AsyncCacheApi) {
  implicit val ec = ExecutionContext.global
  val cacheExpiry: Duration = Duration(1, "day")
  val emptyAddress = AddressModel(
    addressLine1 = "",
    addressLine2 = Some(""),
    addressLine3 = Some(""),
    addressLine4 = Some(""),
    postCode = Some(""),
    countryCode = ""
  )

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
    val ukProperty = sources.properties.find(_.isUkProperty).filterNot(_.isCeased)
    val foreignProperty = sources.properties.find(_.isForeignProperty).filterNot(_.isCeased)

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
      ceasedBusinesses = getCeasedBusinesses(sources = sources)
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
              business.incomeSourceId,
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
          getCeasedBusinesses(sources = sources)
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
              business.incomeSourceId,
              business.tradingName,
              business.tradingStartDate
            )
          }
        } else Nil,
        ukProperty = if (ukPropertyExists) {
          Some(CeasePropertyDetailsViewModel(
            maybeUkProperty.flatMap(_.tradingStartDate)
          ))
        } else None,
        foreignProperty = if (foreignPropertyExists) {
          Some(CeasePropertyDetailsViewModel(
            maybeForeignProperty.flatMap(_.tradingStartDate)
          ))
        } else None,
        ceasedBusinesses = if (ceasedBusinessExists) {
          getCeasedBusinesses(sources = sources)
        } else Nil
      )
    }.toEither
  }

  def getCheckCeaseBusinessDetailsViewModel(sources: IncomeSourceDetailsModel, incomeSourceId: String, businessEndDate: String)
  : Either[Throwable, Option[CheckCeaseBusinessDetailsViewModel]] = {

    val soleTraderBusinesses = sources.businesses.filterNot(_.isCeased).find(x => x.incomeSourceId.equals(incomeSourceId))

    Try {
      soleTraderBusinesses.map { business =>
        CheckCeaseBusinessDetailsViewModel(
          business.incomeSourceId,
          business.tradingName,
          business.address,
          LocalDate.parse(businessEndDate)
        )
      }
    }.toEither
  }

  private def getCeasedBusinesses(sources: IncomeSourceDetailsModel): List[CeasedBusinessDetailsViewModel] = {
    val ceasedSEBusinesses = sources.businesses.filter(_.isCeased)
    val ceasedPropertyBusinesses = sources.properties.filter(_.isCeased)

    val ceasedSEBusinessesViewModels: List[CeasedBusinessDetailsViewModel] =
      for {
        business <- ceasedSEBusinesses
        cessationDate <- business.cessation.flatMap(_.date)
      } yield CeasedBusinessDetailsViewModel(
        tradingName = business.tradingName,
        incomeSourceType = SelfEmployment,
        tradingStartDate = business.tradingStartDate,
        cessationDate = cessationDate
      )

    val ceasedPropertyBusinessesViewModels: List[CeasedBusinessDetailsViewModel] = {
      for {
        property <- ceasedPropertyBusinesses
        cessationDate <- property.cessation.flatMap(_.date)
      } yield
        CeasedBusinessDetailsViewModel(
          tradingName = None,
          incomeSourceType = property.incomeSourceType match {
            case Some("02-uk-property") => UkProperty
            case Some("03-foreign-property") => ForeignProperty
            case _ => throw MissingFieldException("Property income source type is missing or not an expected value")
          },
          tradingStartDate = property.tradingStartDate,
          cessationDate = cessationDate
        )
    }
    ceasedSEBusinessesViewModels ++ ceasedPropertyBusinessesViewModels
  }
}

