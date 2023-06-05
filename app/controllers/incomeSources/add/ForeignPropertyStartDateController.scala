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

package controllers.incomeSources.add

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.add.ForeignPropertyStartDateForm
import forms.utils.SessionKeys.{ceaseUKPropertyEndDate, foreignPropertyStartDate}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.ForeignPropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForeignPropertyStartDateController @Inject()(val authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: FrontendAuthorisedFunctions,
                                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                                   val form: ForeignPropertyStartDateForm,
                                                   val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveNino: NinoPredicate,
                                                   val view: ForeignPropertyStartDate,
                                                   val customNotFoundErrorView: CustomNotFoundError)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleRequest(isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    val controller = controllers.incomeSources.add.routes.ForeignPropertyStartDateController
    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl: String = user.headers.get(REFERER).getOrElse(controllers.incomeSources.add.routes.AddIncomeSourceController.show().url)
    val postAction: Call = if (isAgent) controller.submitAgent() else controller.submit()
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      Future.successful(Ok(view(
        foreignPropertyStartDateForm = form.apply(user,messages),
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        origin = origin)(user, messages)))
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting ForeignPropertyStartDate page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def handleSubmitRequest(isAgent: Boolean, backUrl: String, postAction: Call, redirect: Call)
                         (implicit user: MtdItUser[_], messages: Messages): Future[Result] = {
    form.apply(user = user, messages = messages).bindFromRequest().fold(

      hasErrors => Future.successful(BadRequest(view(
        foreignPropertyStartDateForm = hasErrors,
        postAction = postAction,
        backUrl = backUrl,
        isAgent = isAgent)(user, messages))),

      validatedInput =>
        Future.successful(
          Redirect(redirect).addingToSession(foreignPropertyStartDate -> validatedInput.date.toString))
    )
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
      handleSubmitRequest(
        isAgent = false,
        backUrl = routes.AddIncomeSourceController.show().url,
        postAction = routes.ForeignPropertyStartDateController.submit(),
        redirect = routes.ForeignPropertyStartDateCheckController.show())
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              backUrl = routes.AddIncomeSourceController.showAgent().url,
              postAction = routes.ForeignPropertyStartDateController.submitAgent(),
              redirect = routes.ForeignPropertyStartDateCheckController.showAgent())
        }
  }

}