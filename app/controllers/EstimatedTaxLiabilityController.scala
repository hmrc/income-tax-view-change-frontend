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
import models.{LastTaxCalculation, LastTaxCalculationError}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.EstimatedTaxLiabilityService

@Singleton
class EstimatedTaxLiabilityController @Inject()(implicit val config: AppConfig,
                                                val authentication: AuthenticationPredicate,
                                                val estimatedTaxLiabilityService: EstimatedTaxLiabilityService,
                                                implicit val messagesApi: MessagesApi
                              ) extends BaseController {

  //Static values will always be these for MVP
  final val taxYear = "2018"

  val getEstimatedTaxLiability: Action[AnyContent] = authentication.async { implicit request => implicit user =>
    Logger.debug(s"[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Calling Estimated Tax Liability Service with NINO: ${user.nino}")
    estimatedTaxLiabilityService.getLastEstimatedTaxCalculation(user.nino, taxYear) map {
      case success: LastTaxCalculation =>
        Logger.debug(s"[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Success Response: $success")
        Ok(views.html.estimatedTaxLiability(success.calcAmount))
      case failure: LastTaxCalculationError =>
        Logger.warn(s"[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Error Response: Status=${failure.status}, Message=${failure.message}")
        showInternalServerError
    }
  }
}
