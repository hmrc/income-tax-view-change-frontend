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
import forms.incomeSources.add.CheckUKPropertyStartDateForm
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.CheckUKPropertyStartDate

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckUKPropertyStartDateController @Inject()(val authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: FrontendAuthorisedFunctions,
                                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                                   val dateFormatter: ImplicitDateFormatterImpl,
                                                   val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveNino: NinoPredicate,
                                                   val view: CheckUKPropertyStartDate,
                                                   val customNotFoundErrorView: CustomNotFoundError)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleRequest(isAgent: Boolean, startDate: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url else
      controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.submitAgent() else
      controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.submit()
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      Future.successful(Ok(view(
        checkUKPropertyStartDateForm = CheckUKPropertyStartDateForm.form,
        startDate = startDate,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl)(user, messages)))
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting CheckUKPropertyStartDate page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def show(): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        val startDate = user.session.get(SessionKeys.addUkPropertyStartDate).get
        val formattedStartDate = dateFormatter.longDate(LocalDate.parse(startDate)).toLongDate
        handleRequest(
          isAgent = false,
          startDate = formattedStartDate
        )
    }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            val startDate = mtdItUser.session.get(SessionKeys.addUkPropertyStartDate).get
            val formattedStartDate = dateFormatter.longDate(LocalDate.parse(startDate)).toLongDate
            handleRequest(
              isAgent = true,
              startDate = formattedStartDate
            )
        }
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val (postAction, backUrl, redirectToAccountingMethod) = {
      if (isAgent)
        (routes.CheckUKPropertyStartDateController.submitAgent(),
          routes.AddIncomeSourceStartDateController.showUKPropertyAgent,
          routes.UKPropertyAccountingMethodController.showAgent())
      else
        (routes.CheckUKPropertyStartDateController.submit(),
          routes.AddIncomeSourceStartDateController.showUKProperty,
          routes.UKPropertyAccountingMethodController.show())
    }
    val startDate = user.session.get(SessionKeys.addUkPropertyStartDate).get
    val formattedStartDate = dateFormatter.longDate(LocalDate.parse(startDate)).toLongDate
    CheckUKPropertyStartDateForm.form.bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(view(
        checkUKPropertyStartDateForm = hasErrors,
        postAction = postAction,
        backUrl = backUrl.url,
        isAgent = isAgent,
        startDate = formattedStartDate
      ))),
      validatedInput => {
        val goBackAndChangeDate = validatedInput.equals(Some("no"))
        if (goBackAndChangeDate) {
          Future.successful(Redirect(backUrl)
            .removingFromSession("addUkPropertyStartDate"))
        } else {
          Future.successful(Redirect(redirectToAccountingMethod))
        }
      }
    )
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
}