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

package controllers.agent

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import forms.utils.SessionKeys
import play.api.Logger
import play.api.mvc._
import services.{CalculationPollingService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationPollingController @Inject()(pollCalculationService: CalculationPollingService,
                                             incomeSourceDetailsService: IncomeSourceDetailsService,
                                             val authorisedFunctions: AuthorisedFunctions)
                                            (implicit val appConfig: FrontendAppConfig,
                                             mcc: MessagesControllerComponents,
                                             val itvcErrorHandler: AgentItvcErrorHandler,
                                             val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching {

  def calculationPoller(taxYear: Int, isFinalCalc: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit agent =>

      lazy val successfulPollRedirect: Call = if (isFinalCalc) {
        controllers.agent.routes.FinalTaxCalculationController.show(taxYear)
      } else {
        controllers.agent.routes.TaxYearOverviewController.show(taxYear)
      }

      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap { user =>
        (request.session.get(SessionKeys.calculationId), user.nino, user.mtditid) match {
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
}
