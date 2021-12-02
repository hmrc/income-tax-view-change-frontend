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

package controllers.predicates

import auth.{MtdItUser, MtdItUserWithNino}
import config.ItvcErrorHandler
import controllers.BaseController
import forms.utils.SessionKeys
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import play.api.cache._
import play.api.libs.json.{JsError, JsPath, JsResult, JsSuccess, JsValue, Json}
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceDetailsPredicate @Inject()(val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             val itvcErrorHandler: ItvcErrorHandler, cache: AsyncCacheApi)
                                            (implicit val executionContext: ExecutionContext,
                                             mcc: MessagesControllerComponents) extends BaseController with
  ActionRefiner[MtdItUserWithNino, MtdItUser] {

  override def refine[A](request: MtdItUserWithNino[A]): Future[Either[Result, MtdItUser[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val req: MtdItUserWithNino[A] = request

    // check session for source data
    val cacheExpiry: Duration = Duration(100, "seconds")
    val cacheKey = request.nino + "-incomeSources"
    def getCachedIncomeSources(): Future[Option[IncomeSourceDetailsModel]] = {
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

    getCachedIncomeSources().flatMap {
      case Some(sources: IncomeSourceDetailsModel) =>
        Future.successful(Right(MtdItUser(request.mtditid, request.nino, request.userName, sources, request.saUtr, request.credId, request.userType, request.arn)))
      case None => incomeSourceDetailsService.getIncomeSourceDetails() map {
        case sources: IncomeSourceDetailsModel =>
          // store sources in session
          cache.set(cacheKey, sources.toJson, cacheExpiry)
          Right(MtdItUser(request.mtditid, request.nino, request.userName, sources, request.saUtr, request.credId, request.userType, request.arn))
        case _ => Left(itvcErrorHandler.showInternalServerError)
      }
    }
  }
}
