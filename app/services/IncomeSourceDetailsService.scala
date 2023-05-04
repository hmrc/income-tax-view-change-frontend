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
import models.incomeSourceDetails.viewmodels.{BusinessDetailsViewModel, CeasedBusinessDetailsViewModel, IncomeSourcesViewModel, PropertyDetailsViewModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class IncomeSourceDetailsService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                           val cache: AsyncCacheApi) {
  implicit val ec = ExecutionContext.global
  val cacheExpiry: Duration = Duration(1, "day")

  def getCachedIncomeSources(cacheKey: String): Future[Option[IncomeSourceDetailsModel]] = {
    println(s"\n[cache.get(cacheKey)]: ${Await.ready(cache.get(cacheKey), Duration.fromNanos(1000000))}\n")
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
//    if (cacheKey.isDefined) {
//      getCachedIncomeSources(cacheKey.get).flatMap {
//        case Some(sources: IncomeSourceDetailsModel) =>
//          Logger("application").info(s"incomeSourceDetails cache HIT with ${cacheKey.get}")
//          Future.successful(sources)
//        case None =>
//          Logger("application").info(s"incomeSourceDetails cache MISS with ${cacheKey.get}")
//          incomeTaxViewChangeConnector.getIncomeSources().flatMap {
//            case incomeSourceDetailsModel: IncomeSourceDetailsModel =>
//              cache.set(cacheKey.get, incomeSourceDetailsModel.sanitise.toJson, cacheExpiry).map(
//                _ => incomeSourceDetailsModel
//              )
//            case error: IncomeSourceDetailsResponse => Future.successful(error)
//          }
//      }
//    } else {
      incomeTaxViewChangeConnector.getIncomeSources()
//    }
  }

  def incomeSourcesAsViewModel(sources: IncomeSourceDetailsModel): IncomeSourcesViewModel = {
    IncomeSourcesViewModel(
      soleTraderBusinesses = sources.businesses.filterNot(_.isCeased).map {
        case maybeSoleTraderBusiness if maybeSoleTraderBusiness.tradingName.nonEmpty
          && maybeSoleTraderBusiness.tradingStartDate.nonEmpty =>
            BusinessDetailsViewModel(maybeSoleTraderBusiness.tradingName,
              maybeSoleTraderBusiness.tradingStartDate)
      },
      ukProperty = sources.property.find(_.isUkProperty).map {
        case maybeUkProperty if maybeUkProperty.tradingStartDate.nonEmpty =>
          PropertyDetailsViewModel(maybeUkProperty.tradingStartDate)
      },
      foreignProperty = sources.property.find(_.isForeignProperty).map {
        case maybeForeignProperty if maybeForeignProperty.tradingStartDate.nonEmpty =>
          PropertyDetailsViewModel(maybeForeignProperty.tradingStartDate)
      },
      ceasedBusinesses = sources.businesses.filter(_.isCeased).map {
        case maybeCeasedBusinesses if maybeCeasedBusinesses.tradingName.nonEmpty
          && maybeCeasedBusinesses.tradingStartDate.nonEmpty
          && maybeCeasedBusinesses.cessation.flatMap(_.date).nonEmpty =>
            CeasedBusinessDetailsViewModel(
              maybeCeasedBusinesses.tradingName,
              maybeCeasedBusinesses.tradingStartDate,
              maybeCeasedBusinesses.cessation.flatMap(_.date)
            )
      }
    )
  }
}
