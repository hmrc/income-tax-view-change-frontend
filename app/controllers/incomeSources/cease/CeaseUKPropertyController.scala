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
import forms.CeaseUKPropertyForm
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.incomeSources.cease.CeaseUKProperty
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseUKPropertyController @Inject()(val authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val view: CeaseUKProperty,
                                          val customNotFoundErrorView: CustomNotFoundError)
                                         (implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                         )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String, postAction: String, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val postAction = if (isAgent) controllers.incomeSources.cease.routes.CeaseUKPropertyController.submitAgent else
      controllers.incomeSources.cease.routes.CeaseUKPropertyController.submit

    if (incomeSourcesEnabled) {
      Future.successful(Ok(view(
        ceaseUKPropertyForm = CeaseUKPropertyForm.form,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        origin = origin)(user, messages)))
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting CeaseUKProperty page: ${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          itvcErrorHandler = itvcErrorHandler,
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show(origin).url,
          postAction = controllers.incomeSources.cease.routes.CeaseUKPropertyController.submit.url,
          origin
        )
    }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              itvcErrorHandler = itvcErrorHandlerAgent,
              backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url,
              postAction = controllers.incomeSources.cease.routes.CeaseUKPropertyController.submitAgent.url
            )
        }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      CeaseUKPropertyForm.form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(view(
          ceaseUKPropertyForm = hasErrors,
          postAction = controllers.incomeSources.cease.routes.CeaseUKPropertyController.submit,
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url,
          isAgent = false
        )).addingToSession("ceaseUKPropertyDeclare" -> "false")),
        _ =>
          Future.successful(Redirect(controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.show())
            .addingToSession("ceaseUKPropertyDeclare" -> "true"))
      )
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
            CeaseUKPropertyForm.form.bindFromRequest().fold(
              hasErrors => Future.successful(BadRequest(view(
                ceaseUKPropertyForm = hasErrors,
                postAction = controllers.incomeSources.cease.routes.CeaseUKPropertyController.submitAgent,
                backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url,
                isAgent = true
              )).addingToSession("ceaseUKPropertyDeclare" -> "false")),
              _ =>
                Future.successful(Redirect(controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.showAgent())
                  .addingToSession("ceaseUKPropertyDeclare" -> "true"))
            )
        }
  }
}
