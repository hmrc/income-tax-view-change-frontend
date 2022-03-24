/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.MtdItUserWithNino
import config.{FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import controllers.predicates.{AuthenticationPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationPollingService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationPollingController @Inject()(authenticate: AuthenticationPredicate,
                                             checkSessionTimeout: SessionTimeoutPredicate,
                                             retrieveNino: NinoPredicate,
                                             pollCalculationService: CalculationPollingService,
                                             val itvcErrorHandler: ItvcErrorHandler)
                                            (implicit val appConfig: FrontendAppConfig,
                                             mcc: MessagesControllerComponents,
                                             val executionContext: ExecutionContext)
  extends BaseController with I18nSupport with FeatureSwitching {

  val action: ActionBuilder[MtdItUserWithNino, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino

  def calculationPoller(taxYear: Int, isFinalCalc: Boolean, origin: Option[String]): Action[AnyContent] = action.async {
    implicit user =>

      lazy val successfulPollRedirect: Call = if (isFinalCalc) {
        controllers.routes.FinalTaxCalculationController.show(taxYear, origin)
      } else {
        controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear, origin)
      }

      (user.session.get(SessionKeys.calculationId), user.nino, user.mtditid) match {
        case (Some(calculationId), nino, mtditid) => {
          Logger("application").info(s"[CalculationPollingController][calculationPoller] Polling started for $calculationId")
          pollCalculationService.initiateCalculationPollingSchedulerWithMongoLock(calculationId, nino, mtditid) flatMap {
            case OK =>
              Logger("application").info(s"[CalculationPollingController][calculationPoller] Received OK response for calcId: $calculationId")
              Future.successful(Redirect(successfulPollRedirect))
            case _ =>
              Logger("application").info(s"[CalculationPollingController][calculationPoller] No calculation found for calcId: $calculationId")
              Future.successful(itvcErrorHandler.showInternalServerError())
          } recover {
            case ex: Exception => {
              Logger("application").error(s"[CalculationPollingController][calculationPoller] Polling failed with exception: ${ex.getMessage}")
              itvcErrorHandler.showInternalServerError()
            }
          }
        }
        case _ =>
          Logger("application").error(s"[CalculationPollingController][calculationPoller] calculationId and nino not found in session")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }
}
