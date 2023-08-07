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
import forms.incomeSources.cease.BusinessEndDateForm
import forms.utils.SessionKeys.{ceaseBusinessEndDate, ceaseBusinessIncomeSourceId}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.BusinessEndDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessEndDateController @Inject()(val authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val businessEndDateForm: BusinessEndDateForm,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val businessEndDate: BusinessEndDate,
                                          val customNotFoundErrorView: CustomNotFoundError)
                                         (implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(id: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          id
        )
    }

  def showAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              id
            )
        }
  }

  def handleRequest(isAgent: Boolean, id: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl: String = if (isAgent) controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url else
      controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.cease.routes.BusinessEndDateController.submitAgent(id) else
      controllers.incomeSources.cease.routes.BusinessEndDateController.submit(id)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      Future.successful(Ok(businessEndDate(
        BusinessEndDateForm = businessEndDateForm.apply(user, id),
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        btaNavPartial = user.btaNavPartial
      )(user, messages)))
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessEndDate page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def submit(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(id = id, isAgent = false)
  }

  def submitAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(id = id, isAgent = true)
        }
  }

  def handleSubmitRequest(id: String, isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {

    val (postAction, backAction, redirectAction) = {
      if (isAgent)
        (routes.BusinessEndDateController.submitAgent _,
          routes.CeaseIncomeSourceController.showAgent(),
          routes.CheckCeaseBusinessDetailsController.showAgent())
      else
        (routes.BusinessEndDateController.submit _,
          routes.CeaseIncomeSourceController.show(),
          routes.CheckCeaseBusinessDetailsController.show())
    }

    businessEndDateForm.apply(user, id).bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(businessEndDate(
        BusinessEndDateForm = hasErrors,
        postAction = postAction(id),
        backUrl = backAction.url,
        isAgent = isAgent,
        btaNavPartial = user.btaNavPartial
      ))),
      validatedInput =>
        Future.successful(Redirect(redirectAction)
          .addingToSession(ceaseBusinessEndDate -> validatedInput.date.toString)
          .addingToSession(ceaseBusinessIncomeSourceId -> id))
    )
  }
}
