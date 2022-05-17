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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.CalculationPollingService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationPollingController @Inject()(authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             checkSessionTimeout: SessionTimeoutPredicate,
                                             retrieveNino: NinoPredicate,
                                             pollCalculationService: CalculationPollingService,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                            (implicit val appConfig: FrontendAppConfig,
                                             implicit override val mcc: MessagesControllerComponents,
                                             val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def handleRequest(origin: Option[String] = None,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean,
                    successfulPollRedirect: Call,
                    calculationId: Option[String])
                   (implicit user: MtdItUserWithNino[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    (calculationId, user.nino, user.mtditid) match {
      case (Some(calculationId), nino, mtditid) =>
        Logger("application").info(s"[CalculationPollingController][calculationPoller] Polling started for $calculationId")
        pollCalculationService.initiateCalculationPollingSchedulerWithMongoLock(calculationId, nino, mtditid) flatMap {
          case OK =>
            Logger("application").info(s"[CalculationPollingController][calculationPoller] Received OK response for calcId: $calculationId")
            Future.successful(Redirect(successfulPollRedirect))
          case _ =>
            Logger("application").info(s"[CalculationPollingController][calculationPoller] No calculation found for calcId: $calculationId")
            Future.successful(itvcErrorHandler.showInternalServerError())
        } recover {
          case ex: Exception =>
            Logger("application").error(s"[CalculationPollingController][calculationPoller] Polling failed with exception: ${ex.getMessage}")
            itvcErrorHandler.showInternalServerError()
        }

      case _ =>
        Logger("application").error(s"[CalculationPollingController][calculationPoller] calculationId and nino not found in session")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  def calculationPoller(taxYear: Int, isFinalCalc: Boolean, origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino).async {
      implicit user =>

        lazy val successfulPollRedirect: Call = if (isFinalCalc) {
          controllers.routes.FinalTaxCalculationController.show(taxYear, origin)
        } else {
          controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin)
        }

        handleRequest(
          origin = origin,
          itcvErrorHandler = itvcErrorHandler,
          taxYear = taxYear,
          isAgent = false,
          successfulPollRedirect = successfulPollRedirect,
          calculationId = user.session.get(SessionKeys.calculationId)
        )
    }

  def calculationPollerAgent(taxYear: Int, isFinalCalc: Boolean, origin: Option[String] = None): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit agent =>

        lazy val successfulPollRedirect: Call = if (isFinalCalc) {
          controllers.routes.FinalTaxCalculationController.showAgent(taxYear)
        } else {
          controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear)
        }

        handleRequest(
          origin = origin,
          itcvErrorHandler = itvcErrorHandlerAgent,
          taxYear = taxYear,
          isAgent = true,
          successfulPollRedirect = successfulPollRedirect,
          calculationId = request.session.get(SessionKeys.calculationId)
        )(getMtdItUserWithNino()(agent, request, implicitly), implicitly, implicitly, implicitly)

    }
  }

}
