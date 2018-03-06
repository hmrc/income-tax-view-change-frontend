/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import audit.AuditingService
import auth.{MtdItUser, MtdItUserOptionNino, MtdItUserWithNino}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.{Crystallised, Estimate}
import models.IncomeSourcesModel
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.CalculationService

import scala.concurrent.Future

class EstimatesController @Inject()(implicit val config: FrontendAppConfig,
                                implicit val messagesApi: MessagesApi,
                                val checkSessionTimeout: SessionTimeoutPredicate,
                                val authenticate: AuthenticationPredicate,
                                val retrieveNino: NinoPredicate,
                                val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                val calculationService: CalculationService,
                                val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                val itvcErrorHandler: ItvcErrorHandler,
                                val auditingService: AuditingService
                               ) extends BaseController {

  val viewEstimateCalculations: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user => if(config.features.estimatesEnabled()) renderView else fRedirectToHome
  }

  private[EstimatesController] def renderView[A](implicit user: MtdItUser[A]): Future[Result] = {
    implicit val sources: IncomeSourcesModel = user.incomeSources

    calculationService.getAllLatestCalculations(user.nino, sources.orderedTaxYears).map(estimatesResponse => {
      if (estimatesResponse.exists(_.isErrored)) {
        Logger.debug(s"[EstimatesController][viewEstimateCalculations] Retrieved at least one Errored Last Tax Calc. Response: $estimatesResponse")
        itvcErrorHandler.showInternalServerError
      } else if (estimatesResponse.count(!_.matchesStatus(Crystallised)) == 1) {
        Redirect(controllers.routes.CalculationController.showCalculationForYear(estimatesResponse.filter(!_.matchesStatus(Crystallised)).head.taxYear))
      } else Ok(views.html.estimates(estimatesResponse.filter(!_.matchesStatus(Crystallised)), sources.earliestTaxYear.get))
    })
  }

}
