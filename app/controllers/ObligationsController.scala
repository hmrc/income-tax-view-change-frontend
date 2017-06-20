/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import controllers.predicates.AuthenticationPredicate
import models.{ObligationsResponseModel, ObligationsErrorModel, ObligationsModel}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.ObligationsService

import scala.concurrent.Future

@Singleton
class ObligationsController @Inject()(implicit val config: AppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      val authentication: AuthenticationPredicate,
                                      val obligationsService: ObligationsService
                                     ) extends BaseController {

  val getObligations: Action[AnyContent] = authentication.async { implicit request =>
    implicit user =>

      (for{
        business <- obligationsService.getObligations(user.nino)
        property <- obligationsService.getPropertyObligations(user.nino)
      } yield (business,property)).map {
        case error: (ObligationsErrorModel, ObligationsErrorModel) =>
          Logger.debug("[ObligationsController][getObligations] No Obligations retrieved. Rendering ISE")
          showInternalServerError
        case business: (ObligationsModel, ObligationsErrorModel) =>
          Logger.debug("[ObligationsController][getObligations] Business Obligations retrieved. Serving HTML page")
          Ok(views.html.obligations(business._1))
        case property: (ObligationsErrorModel, ObligationsModel) =>
          Logger.debug("[ObligationsController][getObligations] Property Obligations retrieved. Serving HTML page")
          Ok(views.html.obligations(property._2))
        case _ =>
          Logger.debug("[ObligationsController][getObligations] Business & Property Obligations retrieved. Serving HTML page")
          showInternalServerError
      }
  }
}

