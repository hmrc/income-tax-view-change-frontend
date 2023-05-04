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
import models.incomeSourceDetails.viewmodels.{AddIncomeSourcesViewModel, BusinessDetailsViewModel, CeasedBusinessDetailsViewModel, PropertyDetailsViewModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

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

  def incomeSourcesAsViewModel(sources: IncomeSourceDetailsModel): AddIncomeSourcesViewModel = {

    val soleTraderBusinessesExists = !sources.businesses.forall(_.isCeased)

    val ukPropertyExists = sources.property.exists(_.isUkProperty)

    val foreignPropertyExists = sources.property.exists(_.isForeignProperty)

    val ceasedBusinessExists = sources.businesses.exists(_.isCeased)

    AddIncomeSourcesViewModel(
      soleTraderBusinesses = if (soleTraderBusinessesExists) {
        val maybeSoleTraderBusinesses = sources.businesses.filterNot(_.isCeased)
        maybeSoleTraderBusinesses.map { business =>
          BusinessDetailsViewModel(
            business.tradingName.getOrElse(throw MissingFieldException("Trading Name")),
            business.tradingStartDate.getOrElse(throw MissingFieldException("Trading Start Date"))
          )
        }
      } else Nil,
      ukProperty = if (ukPropertyExists) {
        val maybeUkProperty = sources.property.find(_.isUkProperty)
        Some(PropertyDetailsViewModel(
          maybeUkProperty.flatMap(_.tradingStartDate).getOrElse(throw MissingFieldException("Trading Start Date"))
        ))
      } else None,
      foreignProperty = if (foreignPropertyExists) {
        val maybeForeignProperty = sources.property.find(_.isForeignProperty)
        Some(PropertyDetailsViewModel(
          maybeForeignProperty.flatMap(_.tradingStartDate).getOrElse(throw MissingFieldException("Trading Start Date"))
        ))
      } else None,
      ceasedBusinesses = if (ceasedBusinessExists) {
        sources.businesses.filter(_.isCeased).map { maybeCeasedBusiness =>
          CeasedBusinessDetailsViewModel(
            tradingName = maybeCeasedBusiness.tradingName.getOrElse(throw MissingFieldException("Trading Name")),
            tradingStartDate = maybeCeasedBusiness.tradingStartDate.getOrElse(throw MissingFieldException("Trading Start Date")),
            cessationDate = maybeCeasedBusiness.cessation.flatMap(_.date).getOrElse(throw MissingFieldException("Cessation Date"))
          )
        }
      } else Nil
    )
  }
}




      //    for {
      //      business <- sources.businesses
      //      property <- sources.property
      //    } yield {
      //      AddIncomeSourcesViewModel(
      //        soleTraderBusinesses = if(business.isCeased) {
      //          business.tradingName.getOrElse()
      //        }
      //
      //
      //
      //        , ukProperty = ???, foreignProperty = ???, ceasedBusinesses = ???
      //      )
      //    }
//
//        maybeSoleTraderBusinesses.map {
//        case maybeSoleTraderBusiness if maybeSoleTraderBusiness.tradingName.nonEmpty
//          && maybeSoleTraderBusiness.tradingStartDate.nonEmpty =>
//            BusinessDetailsViewModel(maybeSoleTraderBusiness.tradingName,
//              maybeSoleTraderBusiness.tradingStartDate)
//        case _ => BusinessDetailsViewModel(None, None)
//      },
//      ukProperty = sources.property.find(_.isUkProperty).map {
//        case maybeUkProperty if maybeUkProperty.tradingStartDate.nonEmpty =>
//          PropertyDetailsViewModel(maybeUkProperty.tradingStartDate)
//        case _ => PropertyDetailsViewModel(None)
//      },
//      foreignProperty = sources.property.find(_.isForeignProperty).map {
//        case maybeForeignProperty if maybeForeignProperty.tradingStartDate.nonEmpty =>
//          PropertyDetailsViewModel(maybeForeignProperty.tradingStartDate)
//        case _ => PropertyDetailsViewModel(None)
//      },
//      ceasedBusinesses = sources.businesses.filter(_.isCeased).map {
//        case maybeCeasedBusiness if maybeCeasedBusiness.tradingName.nonEmpty
//          && maybeCeasedBusiness.tradingStartDate.nonEmpty
//          && maybeCeasedBusiness.cessation.flatMap(_.date).nonEmpty =>
//            CeasedBusinessDetailsViewModel(
//              maybeCeasedBusiness.tradingName,
//              maybeCeasedBusiness.tradingStartDate,
//              maybeCeasedBusiness.cessation.flatMap(_.date)
//            )
//        case _ => CeasedBusinessDetailsViewModel(None, None, None)
//      }
//    )
//  }
//}
