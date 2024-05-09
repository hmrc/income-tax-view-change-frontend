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

import audit.AuditingService
import audit.models.NextUpdatesAuditing.NextUpdatesAuditModel
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, OptOut}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import models.nextUpdates._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import services.{IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate
import views.html.nextUpdates.{NextUpdates, NextUpdatesOptOut, NoNextUpdates}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesController @Inject()(NoNextUpdatesView: NoNextUpdates,
                                      nextUpdatesView: NextUpdates,
                                      nextUpdatesOptOutView: NextUpdatesOptOut,
                                      incomeSourceDetailsService: IncomeSourceDetailsService,
                                      auditingService: AuditingService,
                                      nextUpdatesService: NextUpdatesService,
                                      itvcErrorHandler: ItvcErrorHandler,
                                      optOutService: OptOutService,
                                      val appConfig: FrontendAppConfig,
                                      val authorisedFunctions: FrontendAuthorisedFunctions,
                                      val auth: AuthenticatorPredicate)
                                     (implicit mcc: MessagesControllerComponents,
                                      implicit val agentItvcErrorHandler: AgentItvcErrorHandler,
                                      val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private def hasAnyIncomeSource(action: => Future[Result])(implicit user: MtdItUser[_], origin: Option[String]): Future[Result] = {
    if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
      action
    } else {
      Future.successful(Ok(NoNextUpdatesView(backUrl = controllers.routes.HomeController.show(origin).url)))
    }
  }

  def getNextUpdates(backUrl: Call, isAgent: Boolean, errorHandler: ShowInternalServerError, origin: Option[String] = None)
                    (implicit user: MtdItUser[_]): Future[Result] =
    hasAnyIncomeSource {
      for {
        nextUpdates <- nextUpdatesService.getNextUpdates().map {
          case obligations: ObligationsModel => obligations
          case _ => ObligationsModel(Nil)
        }
        viewModel = nextUpdatesService.getNextUpdatesViewModel(nextUpdates)
        result <- (nextUpdates.obligations, isEnabled(OptOut)) match {
          case (Nil, _) =>
            Future.successful(errorHandler.showInternalServerError())
          case (_, true) =>
            auditNextUpdates(user, isAgent, origin)
            optOutService.getNextUpdatesQuarterlyReportingContentChecks.flatMap { checks =>
              optOutService.getOneYearOptOutViewModel().map { optOutOneYearViewModel =>
                Ok(nextUpdatesOptOutView(viewModel, optOutOneYearViewModel, checks, backUrl.url, isAgent, origin))
              }
            } recover {
              case ex =>
                Logger("application").error(s"Unexpected future failed error, ${ex.getMessage}")
                errorHandler.showInternalServerError()
            }
          case (_, false) =>
            auditNextUpdates(user, isAgent, origin)
            Future.successful(Ok(nextUpdatesView(viewModel, backUrl.url, isAgent, origin)))
        }
      } yield result
    }(user, origin)


  def show(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      getNextUpdates(
        controllers.routes.HomeController.show(origin),
        isAgent = false,
        itvcErrorHandler,
        origin)
  }

  def showAgent: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
        mtdItUser =>
          getNextUpdates(
            controllers.routes.HomeController.showAgent,
            isAgent = true,
            agentItvcErrorHandler,
            None)(mtdItUser)
      }
  }

  private def auditNextUpdates[A](user: MtdItUser[A], isAgent: Boolean, origin: Option[String])(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    if (isAgent) {
      auditingService.extendedAudit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.showAgent.url))
    } else {
      auditingService.extendedAudit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.show(origin).url))
    }
}