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
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.cease.DateUKPropertyCeasedForm
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.{DateService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.DateUKPropertyCeased

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DateUKPropertyCeasedController @Inject()(val authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: FrontendAuthorisedFunctions,
                                               val checkSessionTimeout: SessionTimeoutPredicate,
                                               val dateUKPropertyCeasedForm: DateUKPropertyCeasedForm,
                                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val retrieveNino: NinoPredicate,
                                               val view: DateUKPropertyCeased,
                                               val customNotFoundErrorView: CustomNotFoundError)
                                              (implicit val appConfig: FrontendAppConfig,
                                               mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleRequest(isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl: String = if (isAgent) controllers.incomeSources.cease.routes.CeaseUKPropertyController.showAgent().url else
      controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.submitAgent() else
      controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.submit()
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      Future.successful(Ok(view(
        dateUKPropertyCeasedForm = dateUKPropertyCeasedForm.apply,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        origin = origin)(user, messages)))
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting DateUKPropertyCeased page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          origin
        )
    }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true
            )
        }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      dateUKPropertyCeasedForm.apply.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(view(
          dateUKPropertyCeasedForm = hasErrors,
          postAction = controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.submit(),
          backUrl = controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url,
          isAgent = false
        ))),
        _ =>
          Future.successful(Redirect(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.show()))
      )
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            dateUKPropertyCeasedForm.apply.bindFromRequest().fold(
              hasErrors => Future.successful(BadRequest(view(
                dateUKPropertyCeasedForm = hasErrors,
                postAction = controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.submitAgent(),
                backUrl = controllers.incomeSources.cease.routes.CeaseUKPropertyController.showAgent().url,
                isAgent = true
              ))),
              _ =>
                Future.successful(Redirect(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent()))
            )
        }
  }

}