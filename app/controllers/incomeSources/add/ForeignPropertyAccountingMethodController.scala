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
import models.incomeSourceDetails.BusinessDetailsModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.ForeignPropertyAccountingMethod
import forms.incomeSources.add.ForeignPropertyAccountingMethodForm
import forms.utils.SessionKeys.addForeignPropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import views.html.helper.form


class ForeignPropertyAccountingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                          val authorisedFunctions: FrontendAuthorisedFunctions,
                                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                          val retrieveBtaNavBar: NavBarPredicate,
                                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                          val retrieveNino: NinoPredicate,
                                                          val customNotFoundErrorView: CustomNotFoundError,
                                                          val view: ForeignPropertyAccountingMethod)
                                                         (implicit val appConfig: FrontendAppConfig,
                                                          mcc: MessagesControllerComponents,
                                                          val ec: ExecutionContext,
                                                          val itvcErrorHandler: ItvcErrorHandler,
                                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                                         )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user => handleRequest(isAgent = false)
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

  def handleRequest(isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = isAgent, isUpdate = false).url
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.submitAgent() else
      controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.submit()

    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      Future.successful(Ok(view(
        form = ForeignPropertyAccountingMethodForm.form,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        btaNavPartial = user.btaNavPartial
      )(user, messages)))
    } else {
      Future.successful(if (isAgent) Redirect(controllers.routes.HomeController.showAgent) else Redirect(controllers.routes.HomeController.show()))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting foreign property page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false)
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {

    val (postAction, backAction, redirectAction) = {
      if (isAgent)
        (routes.ForeignPropertyAccountingMethodController.submitAgent(),
          routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = isAgent, isUpdate = false),
          routes.ForeignPropertyCheckDetailsController.showAgent())
      else
        (routes.ForeignPropertyAccountingMethodController.submit(),
          routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = isAgent, isUpdate = false),
          routes.ForeignPropertyCheckDetailsController.show())
    }


    ForeignPropertyAccountingMethodForm.form.bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(view(
        form = hasErrors,
        postAction = postAction,
        backUrl = backAction.url,
        isAgent = true
      ))),
      validatedInput => {
        if (validatedInput.equals(Some("cash"))) {
          Future.successful(Redirect(redirectAction)
            .addingToSession(addForeignPropertyAccountingMethod -> "cash"))
        } else {
          Future.successful(Redirect(redirectAction)
            .addingToSession(addForeignPropertyAccountingMethod -> "accruals"))
        }
      }
    )
  }

  def changeForeignPropertyAccountingMethod(): Action[AnyContent] = Action {
    Ok("Change Foreign Property Accounting Method WIP")
  }

  def changeForeignPropertyAccountingMethodAgent(): Action[AnyContent] = Action {
    Ok("Agent change Foreign Property Accounting Method WIP")
  }

}
