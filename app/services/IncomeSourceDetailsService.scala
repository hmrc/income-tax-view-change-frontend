/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.{MtdItUser, MtdItUserWithNino}
import connectors.IncomeTaxViewChangeConnector
import models.incomeSourceDetails.{IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceDetailsService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                           val cache: AsyncCacheApi, implicit val executionContext: ExecutionContext) {

  val cacheExpiry: Duration = Duration(100, "seconds")
//  val cacheKey = request.headers.get(HeaderNames.xSessionId) + request.nino + "-incomeSources"
  def getCachedIncomeSources(cacheKey: String): Future[Option[IncomeSourceDetailsModel]] = {
    cache.get(cacheKey).map((incomeSources: Option[JsValue]) => {
      incomeSources match {
        case Some(jsonSources) =>
          Json.fromJson[IncomeSourceDetailsModel](jsonSources) match {
            case JsSuccess(sources: IncomeSourceDetailsModel, path: JsPath) =>
              Some(sources)
            case _ => None
          }
        case None => None
      }
    })
  }

  def getIncomeSourceDetails(cacheKey: Option[String] = None)(implicit hc: HeaderCarrier,
                                                    mtdUser: MtdItUserWithNino[_]): Future[IncomeSourceDetailsResponse] = {
    cache.set("test", "teststring")
    if (cacheKey.isDefined) {
      getCachedIncomeSources(cacheKey.get).flatMap {
        case Some(sources: IncomeSourceDetailsModel) =>
          println("getIncomeSourceDetails cache HIT")
          Future.successful(sources)
        case None =>
          println("getIncomeSourceDetails cache MISS")
          incomeTaxViewChangeConnector.getIncomeSources().map(incomeSourcesResponse => {
            cache.set(cacheKey.get, incomeSourcesResponse.toJson, cacheExpiry)
            println("getIncomeSourceDetails set cache key:" + cacheKey.get)
            incomeSourcesResponse
          })
      }
    } else {
      println("getIncomeSourceDetails caching NOT ENABLED")
      incomeTaxViewChangeConnector.getIncomeSources()
//      cache.set(cacheKey.get, incomeSourcesResponse.toJson, cacheExpiry)
    }
  }
}
