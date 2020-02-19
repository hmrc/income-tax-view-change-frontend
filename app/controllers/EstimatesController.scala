/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.AuditingService
import auth.MtdItUser
import config.featureswitch.{Estimates, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.CalculationService

import scala.concurrent.{ExecutionContext, Future}

class EstimatesController @Inject()(implicit val appConfig: FrontendAppConfig,
                                    implicit val messagesApi: MessagesApi,
                                    implicit val ec: ExecutionContext,
                                    val checkSessionTimeout: SessionTimeoutPredicate,
                                    val authenticate: AuthenticationPredicate,
                                    val retrieveNino: NinoPredicate,
                                    val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                    val calculationService: CalculationService,
                                    val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                    val itvcErrorHandler: ItvcErrorHandler,
                                    val auditingService: AuditingService
                                   ) extends BaseController with FeatureSwitching{

  val viewEstimateCalculations: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user => if (isEnabled(Estimates)) renderView else fRedirectToHome
  }

  private[EstimatesController] def renderView[A](implicit user: MtdItUser[A]): Future[Result] = {
    calculationService.getAllLatestCalculations(user.nino, user.incomeSources.orderedTaxYears).map {
      case estimatesResponse if estimatesResponse.exists(_.isError) =>
        itvcErrorHandler.showInternalServerError
      case estimatesResponse if estimatesResponse.count(_.notCrystallised) == 1 =>
        Redirect(controllers.routes.CalculationController.renderCalculationPage(estimatesResponse.filter(_.notCrystallised).head.year))
          .addingToSession("singleEstimate" -> "true")
      case estimatesResponse => Ok(views.html.estimates(estimatesResponse.filter(_.notCrystallised)))
        .addingToSession("singleEstimate" -> "false")
    }
  }

}
