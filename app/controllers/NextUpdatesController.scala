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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.nextUpdates._
import models.optOut.OptOutMessageResponse
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{IncomeSourceDetailsService, NextUpdatesService, OptOutService_MISUV_7542}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate
import views.html.{NextUpdates, NextUpdatesOptOut, NoNextUpdates}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesController @Inject()(NoNextUpdatesView: NoNextUpdates,
                                      nextUpdatesView: NextUpdates,
                                      nextUpdatesOptOutView: NextUpdatesOptOut,
                                      incomeSourceDetailsService: IncomeSourceDetailsService,
                                      auditingService: AuditingService,
                                      nextUpdatesService: NextUpdatesService,
                                      optOutService: OptOutService_MISUV_7542,
                                      itvcErrorHandler: ItvcErrorHandler,
                                      val appConfig: FrontendAppConfig,
                                      val authorisedFunctions: FrontendAuthorisedFunctions,
                                      val auth: AuthenticatorPredicate)
                                     (implicit mcc: MessagesControllerComponents,
                                      implicit val agentItvcErrorHandler: AgentItvcErrorHandler,
                                      val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def getNextUpdates(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
        for {
          nextUpdates <- nextUpdatesService.getNextUpdates().map {
            case obligations: ObligationsModel => obligations
            case _ => ObligationsModel(Nil)
          }
          optOutMessage <- optOutService.displayOptOutMessage()
        } yield {
          if (nextUpdates.obligations.nonEmpty) {
            Ok(view(nextUpdates, optOutMessage, backUrl = controllers.routes.HomeController.show(origin).url, isAgent = false, origin = origin)(user))
          } else {
            itvcErrorHandler.showInternalServerError()
          }
        }
      } else {
        Future.successful(Ok(NoNextUpdatesView(backUrl = controllers.routes.HomeController.show(origin).url)))
      }
  }

  val getNextUpdatesAgent: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
        mtdItUser =>
          optOutService.displayOptOutMessage().flatMap {
            optOutMessage =>
              nextUpdatesService.getNextUpdates()(implicitly, mtdItUser).map {
                case nextUpdates: ObligationsModel if nextUpdates.obligations.nonEmpty =>
                  Ok(view(nextUpdates, optOutMessage, controllers.routes.HomeController.showAgent.url, isAgent = true)(mtdItUser))
                case _ => agentItvcErrorHandler.showInternalServerError()
              }
          }
      }
  }

  private def view(obligationsModel: ObligationsModel, optOutMessage: OptOutMessageResponse, backUrl: String, isAgent: Boolean, origin: Option[String] = None)
                  (implicit user: MtdItUser[_]): Html = {
    auditNextUpdates(user, isAgent, origin)
    val viewModel = nextUpdatesService.getNextUpdatesViewModel(obligationsModel)
    if (isEnabled(OptOut)) {
      nextUpdatesOptOutView(currentObligations = viewModel, optOutMessage, backUrl = backUrl, isAgent = isAgent, origin = origin)
    } else {
      nextUpdatesView(currentObligations = viewModel, backUrl = backUrl, isAgent = isAgent, origin = origin)
    }
  }

  private def auditNextUpdates[A](user: MtdItUser[A], isAgent: Boolean, origin: Option[String])(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    if (isAgent) {
      auditingService.extendedAudit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.getNextUpdatesAgent.url))
    } else {
      auditingService.extendedAudit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.getNextUpdates(origin).url))
    }
}