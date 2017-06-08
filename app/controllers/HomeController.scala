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

import com.google.inject.Inject
import config.AppConfig
import controllers.predicates.AuthenticationPredicate
import models.{EstimatedTaxLiability, EstimatedTaxLiabilityError}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.EstimatedTaxLiabilityService

class HomeController @Inject()(implicit val config: AppConfig,
                               val authentication: AuthenticationPredicate,
                               val estimatedTaxLiabilityService: EstimatedTaxLiabilityService,
                               implicit val messagesApi: MessagesApi
                              ) extends BaseController {

  def home(): Action[AnyContent] = authentication.async { implicit request => implicit user =>
    Logger.debug(s"[HomeController][home] Calling Estimated Tax Liability Service with MTDITID: ${user.mtditid}")
    estimatedTaxLiabilityService.getEstimatedTaxLiability(user.mtditid) map {
      case success: EstimatedTaxLiability =>
        Logger.debug(s"[HomeController][home] Success Response: $success")
        Ok(views.html.home(success.total))
      case failure: EstimatedTaxLiabilityError =>
        Logger.debug(s"[HomeController][home] Error Response: Status=${failure.status}, Message=${failure.message}")
        showInternalServerError
    }
  }
}
