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
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import models.admin.{OptInOptOutContentUpdateR17, OptOutFs}
import models.obligations._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.NextUpdatesService
import services.optout.OptOutService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import viewUtils.NextUpdatesViewUtils
import views.html.nextUpdates.{NextUpdates, NextUpdatesOptOut, NoNextUpdates}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesController @Inject()(
                                       noNextUpdatesView: NoNextUpdates,
                                       nextUpdatesView: NextUpdates,
                                       nextUpdatesOptOutView: NextUpdatesOptOut,
                                       auditingService: AuditingService,
                                       nextUpdatesService: NextUpdatesService,
                                       itvcErrorHandler: ItvcErrorHandler,
                                       optOutService: OptOutService,
                                       nextUpdatesViewUtils: NextUpdatesViewUtils,
                                       val appConfig: FrontendAppConfig,
                                       val authActions: AuthActions
                                     )
                                     (
                                       implicit mcc: MessagesControllerComponents,
                                       val agentItvcErrorHandler: AgentItvcErrorHandler,
                                       val ec: ExecutionContext
                                     )
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  private def hasAnyIncomeSource(action: => Future[Result])(implicit user: MtdItUser[_], origin: Option[String]): Future[Result] = {

    if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
      action
    } else {
      Future.successful(Ok(noNextUpdatesView(backUrl = controllers.routes.HomeController.show(origin).url)))
    }
  }

  def getNextUpdates(backUrl: Call, isAgent: Boolean, errorHandler: ShowInternalServerError, origin: Option[String] = None)
                    (implicit user: MtdItUser[_]): Future[Result] = {

    hasAnyIncomeSource {
      for {
        nextUpdates <- nextUpdatesService.getOpenObligations().map {
          case obligations: ObligationsModel => obligations
          case _ => ObligationsModel(Nil)
        }
        isR17ContentEnabled = isEnabled(OptInOptOutContentUpdateR17)
        isOptOutEnabled = isEnabled(OptOutFs)

        result <- nextUpdates.obligations match {
          case Nil =>
            Future.successful(errorHandler.showInternalServerError())
          case _ =>
            auditNextUpdates(user, isAgent, origin)

            val optOutSetup = {
              for {
                (checks, optOutOneYearViewModel, optOutProposition) <- optOutService.nextUpdatesPageChecksAndProposition()
                viewModel = nextUpdatesService.getNextUpdatesViewModel(nextUpdates, isR17ContentEnabled)
              } yield {
                val whatTheUserCanDoContent = if (isOptOutEnabled) nextUpdatesViewUtils.whatTheUserCanDo(optOutOneYearViewModel, isAgent) else None

                Ok(
                nextUpdatesOptOutView(
                  viewModel = viewModel,
                  checks = checks,
                  optOutProposition = optOutProposition,
                  backUrl = backUrl.url,
                  isAgent = isAgent,
                  isSupportingAgent = user.isSupportingAgent,
                  origin = origin,
                  whatTheUserCanDo = whatTheUserCanDoContent,
                  optInOptOutContentR17Enabled = isR17ContentEnabled
                )
              )
              }
            }.recoverWith {
              case ex =>
                val viewModel = nextUpdatesService.getNextUpdatesViewModel(nextUpdates, false)

                Logger("application").error(s"Failed to retrieve quarterly reporting content checks: ${ex.getMessage}")
                Future.successful(Ok(nextUpdatesView(viewModel, backUrl.url, isAgent, user.isSupportingAgent, origin))) // Render view even on failure
            }
            optOutSetup
        }
      } yield result
    }(user, origin)
  }


  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual.async { implicit user =>
    getNextUpdates(
      backUrl = controllers.routes.HomeController.show(origin),
      isAgent = false,
      errorHandler = itvcErrorHandler,
      origin = origin
    )
  }

  def showAgent: Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      getNextUpdates(
        backUrl = controllers.routes.HomeController.showAgent(),
        isAgent = true,
        errorHandler = agentItvcErrorHandler,
        origin = None
      )
  }

  private def auditNextUpdates[A](user: MtdItUser[A], isAgent: Boolean, origin: Option[String])(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    if (isAgent) {
      auditingService.extendedAudit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.showAgent().url))
    } else {
      auditingService.extendedAudit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.show(origin).url))
    }
}