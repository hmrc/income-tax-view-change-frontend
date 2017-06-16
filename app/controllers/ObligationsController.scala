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
import models.{ObligationsErrorModel, ObligationsModel}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.ObligationsService

@Singleton
class ObligationsController @Inject()(implicit val config: AppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      val authentication: AuthenticationPredicate,
                                      val obligationsService: ObligationsService
                                     ) extends BaseController {

  val getObligations: Action[AnyContent] = authentication.async { implicit request =>
    implicit user =>
      Logger.debug(s"[ObligationsController][getObligations] Calling Obligations Service for user with NINO: ${user.nino}")
      obligationsService.getObligations(user.nino).map {
        case obligations: ObligationsModel =>
          Logger.debug("[ObligationsController][getObligations] Obligations retrieved.  Serving HTML page")
          Ok(views.html.obligations(obligations))
        case error: ObligationsErrorModel =>
          Logger.warn("[ObligationsController][getObligations] No obligations retrieved. Rendering ISE")
          showInternalServerError
      }
  }
}
