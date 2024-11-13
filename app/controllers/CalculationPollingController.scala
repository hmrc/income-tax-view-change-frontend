/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.utils.SessionKeys
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationPollingService, SessionDataService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.AuthenticatorPredicate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationPollingController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                             pollCalculationService: CalculationPollingService,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             val auth: AuthenticatorPredicate)
                                            (implicit val appConfig: FrontendAppConfig,
                                             implicit override val mcc: MessagesControllerComponents,
                                             implicit val ec: ExecutionContext,
                                             val languageUtils: LanguageUtils,
                                             val sessionDataService: SessionDataService)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def handleRequest(origin: Option[String] = None,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean,
                    successfulPollRedirect: Call,
                    calculationId: Option[String])
                   (implicit user: MtdItUserWithNino[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    (calculationId, user.nino, user.mtditid) match {
      case (Some(calculationId), nino, mtditid) =>
        Logger("application").info(s"Polling started for $calculationId")
        pollCalculationService.initiateCalculationPollingSchedulerWithMongoLock(calculationId, nino, taxYear, mtditid) flatMap {
          case OK =>
            Logger("application").info(s"Received OK response for calcId: $calculationId")
            Future.successful(Redirect(successfulPollRedirect))
          case _ =>
            Logger("application").info(s"No calculation found for calcId: $calculationId")
            Future.successful(itvcErrorHandler.showInternalServerError())
        } recover {
          case ex: Exception =>
            Logger("application").error(s"Polling failed with exception: ${ex.getMessage} - ${ex.getCause}")
            itvcErrorHandler.showInternalServerError()
        }

      case _ =>
        Logger("application").error("calculationId and nino not found in session")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  def calculationPoller(taxYear: Int, isFinalCalc: Boolean, origin: Option[String] = None): Action[AnyContent] =
    auth.authenticatedActionWithNino {
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
    auth.authenticatedActionWithNinoAgent { implicit response =>
      getMtdItUserWithNino()(response.agent, response.request) flatMap { userWithNino =>

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
          calculationId = response.request.session.get(SessionKeys.calculationId)
        )(userWithNino, response.hc, implicitly)

      }
    }
  }
}
