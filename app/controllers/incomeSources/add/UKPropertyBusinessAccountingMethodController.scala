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
import forms.incomeSources.add.UKPropertyBusinessAccountingMethodForm
import forms.utils.SessionKeys.addUkPropertyAccountingMethod
import models.incomeSourceDetails.PropertyDetailsModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.UKPropertyBusinessAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UKPropertyBusinessAccountingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: FrontendAuthorisedFunctions,
                                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                                   val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveNino: NinoPredicate,
                                                   val view: UKPropertyBusinessAccountingMethod,
                                                   val customNotFoundErrorView: CustomNotFoundError)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleRequest(isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent().url else
      controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyBusinessAccountingMethodController.submitAgent() else
      controllers.incomeSources.add.routes.UKPropertyBusinessAccountingMethodController.submit()

    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      val userProperties: List[PropertyDetailsModel] = user.incomeSources.properties
      if (shouldRedirectToCheckDetailsPage(userProperties)) {
        if (isAgent) {
          Future.successful(Redirect(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent())
            .addingToSession(addUkPropertyAccountingMethod -> "cash"))
        } else {
          Future.successful(Redirect(controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show())
            .addingToSession(addUkPropertyAccountingMethod -> "cash"))
        }
      } else {
        Future.successful(Ok(view(
          form = UKPropertyBusinessAccountingMethodForm.form,
          postAction = postAction,
          isAgent = isAgent,
          backUrl = backUrl,
          btaNavPartial = user.btaNavPartial
        )(user, messages)))
      }
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting AddUkPropertyAccountingMethod page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def shouldRedirectToCheckDetailsPage(userProperties: List[PropertyDetailsModel]): Boolean = {
    if(userProperties.filter(_.isUkProperty).size > 0) {
      true
    } else {
      false
    }
  }

  def show(): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false
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

  private def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val submitUrl: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyBusinessAccountingMethodController.submitAgent() else
      controllers.incomeSources.add.routes.UKPropertyBusinessAccountingMethodController.submit()
    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent().url else
      controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.show().url
    val redirectUrl: Call = if (isAgent) controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent() else
      controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show()

    if (incomeSourcesEnabled) {
      UKPropertyBusinessAccountingMethodForm.form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(view(
          form = hasErrors,
          postAction = submitUrl,
          backUrl = backUrl,
          isAgent = isAgent
        ))),
        validatedInput => {
          if (validatedInput.equals(Some("cash"))) {
            Future.successful(Redirect(redirectUrl)
              .addingToSession(addUkPropertyAccountingMethod -> "cash"))
          } else {
            Future.successful(Redirect(redirectUrl)
              .addingToSession(addUkPropertyAccountingMethod -> "accruals"))
          }
        }
      )
    } else {
      Future.successful(Ok(customNotFoundErrorView()))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"Error getting UKPropertyBusinessAccountingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false)
  }

  def submitAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser => handleSubmitRequest(isAgent = true)
        }
  }

}
