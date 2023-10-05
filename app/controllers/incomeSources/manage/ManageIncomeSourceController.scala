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

package controllers.incomeSources.manage

import audit.AuditingService
import audit.models.ManageIncomeSourcesAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.ManageIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                             val checkSessionTimeout: SessionTimeoutPredicate,
                                             val authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             val retrieveNino: NinoPredicate,
                                             val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                             implicit val itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             auditingService: AuditingService,
                                             val retrieveBtaNavBar: NavBarPredicate)
                                            (implicit val ec: ExecutionContext,
                                             implicit val sessionService: SessionService,
                                             implicit override val mcc: MessagesControllerComponents,
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def show(isAgent: Boolean): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>
    handleRequest(
      sources = user.incomeSources,
      isAgent = isAgent,
      backUrl = {
        if (isAgent) controllers.routes.HomeController.show()
        else controllers.routes.HomeController.showAgent
      }.url
    )
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = {

    withIncomeSourcesFS {
      incomeSourceDetailsService.getViewIncomeSourceViewModel(sources) match {
        case Right(viewModel) =>
          withIncomeSourcesRemovedFromSession {
            auditingService
              .extendedAudit(
                ManageIncomeSourcesAuditModel(
                  ukProperty = viewModel.viewUkProperty,
                  foreignProperty = viewModel.viewForeignProperty,
                  ceasedBusinesses = viewModel.viewCeasedBusinesses,
                  soleTraderBusinesses = viewModel.viewSoleTraderBusinesses
                )
              )
            Ok(manageIncomeSources(
              sources = viewModel,
              isAgent = isAgent,
              backUrl = backUrl
            ))
          } recover {
            case ex: Exception =>
              Logger("application").error(
                s"[ManageIncomeSourceController][handleRequest] - Session Error: ${ex.getMessage}")
              showInternalServerError(isAgent)
          }
        case Left(ex) =>
          Logger("application").error(
            s"[ManageIncomeSourceController][handleRequest] - Error: ${ex.getMessage}")
          Future(showInternalServerError(isAgent))
      }
    }
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }
}