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

package controllers.incomeSources.cease

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{IncomeSourceDetailsService, UpdateIncomeSourceService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.CheckCeaseUKPropertyDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckCeaseUKPropertyDetailsController @Inject()(val authenticate: AuthenticationPredicate,
                                                      val authorisedFunctions: FrontendAuthorisedFunctions,
                                                      val checkSessionTimeout: SessionTimeoutPredicate,
                                                      val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                      val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                      val retrieveNino: NinoPredicate,
                                                      val retrieveBtaNavBar: NavBarPredicate,
                                                      val view: CheckCeaseUKPropertyDetails,
                                                      val service: UpdateIncomeSourceService,
                                                      val customNotFoundErrorView: CustomNotFoundError)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext,
                                                      val itvcErrorHandler: ItvcErrorHandler,
                                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleRequest(isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    if (incomeSourcesEnabled) {
      Future.successful(Ok(view(
        isAgent = isAgent,
        origin = origin)(user, messages)))
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting CeaseUKProperty page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          origin
        )
    }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true
            )
        }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
    service.updateCessationDate.map{
      case Left(ex) =>
        Logger("application").error(s"[CheckCeaseUKPropertyDetailsController][submit] Error submitting cease date:${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
      case Right(result) => result match {
        case r:UpdateIncomeSourceResponseModel => Redirect(controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url)
        case r:UpdateIncomeSourceResponseError =>
          Logger("application").error(s"[CheckCeaseUKPropertyDetailsController][submit] Error submitting cease date:${r.status} ${r.reason}")
          itvcErrorHandler.showInternalServerError()
      }
    }
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
            service.updateCessationDate.map {
              case Left(ex) =>
                Logger("application").error(s"[CheckCeaseUKPropertyDetailsController][submitAgent] Error submitting cease date:${ex.getMessage}")
                itvcErrorHandlerAgent.showInternalServerError()
              case Right(result) => result match {
                case r: UpdateIncomeSourceResponseModel => Redirect(controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url)
                case r: UpdateIncomeSourceResponseError =>
                  Logger("application").error(s"[CheckCeaseUKPropertyDetailsController][submitAgent] Error submitting cease date:${r.status} ${r.reason}")
                  itvcErrorHandlerAgent.showInternalServerError()
              }
            }
        }
  }
}