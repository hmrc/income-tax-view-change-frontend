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

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.add.ForeignPropertyStartDateCheckForm
import forms.incomeSources.add.ForeignPropertyStartDateCheckForm.{response, responseNo, responseYes}
import forms.utils.SessionKeys.foreignPropertyStartDate
import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.ForeignPropertyStartDateCheck

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForeignPropertyStartDateCheckController @Inject()(authenticate: AuthenticationPredicate,
                                                        val authorisedFunctions: AuthorisedFunctions,
                                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                                        val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                        val retrieveBtaNavBar: NavBarPredicate,
                                                        val retrieveNino: NinoPredicate,
                                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                        val view: ForeignPropertyStartDateCheck,
                                                        val customNotFoundErrorView: CustomNotFoundError,
                                                        val languageUtils: LanguageUtils)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitDateFormatter {

  lazy val backUrl: String = routes.ForeignPropertyStartDateController.show().url
  lazy val backUrlAgent: String = routes.ForeignPropertyStartDateController.showAgent().url
  lazy val nextPage: String = routes.ForeignPropertyAccountingMethodController.show().url
  lazy val nextPageAgent: String = routes.ForeignPropertyAccountingMethodController.showAgent().url


  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user => handleRequest(isAgent = false, backUrl = backUrl)
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser => handleRequest(isAgent = true, backUrl = backUrlAgent)
        }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(
        isAgent = false,
        backUrl = backUrl,
        nextPageUrl = nextPage,
        itvcErrorHandler = itvcErrorHandler
      )
  }

  def submitAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              backUrl = backUrlAgent,
              nextPageUrl = nextPageAgent,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def handleRequest(isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    if (incomeSourcesEnabled) {
      user.session.get(foreignPropertyStartDate) match {
        case Some(date) =>
          Future(Ok(view(
            form = ForeignPropertyStartDateCheckForm.form,
            backUrl = backUrl,
            isAgent = isAgent,
            foreignPropertyStartDate = longDate(date.toLocalDate)(messages).toLongDate
          )(user, messages)))
        case _ =>
          Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
            s"[ForeignPropertyStartDateCheckController][handleRequest]: failed to get $foreignPropertyStartDate from session")
          Future(itvcErrorHandler.showInternalServerError())
      }
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting ForeignPropertyStartDateCheck page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }


  def handleSubmitRequest(isAgent: Boolean,
                          backUrl: String,
                          nextPageUrl: String,
                          itvcErrorHandler: ShowInternalServerError)
                         (implicit mtdItUser: MtdItUser[_],messages: Messages): Future[Result] = {
    mtdItUser.session.get(foreignPropertyStartDate) match {
      case Some(date) =>
        ForeignPropertyStartDateCheckForm.form.bindFromRequest().fold(
          formWithErrors =>
            Future(BadRequest(view(
              form = formWithErrors,
              backUrl = backUrl,
              isAgent = isAgent,
              foreignPropertyStartDate = longDate(date.toLocalDate)(messages).toLongDate
            )(mtdItUser,messages))),
          _.toFormMap(response).headOption match {
            case selection if selection.contains(responseNo) =>
              Future.successful(
                Redirect(backUrl)
                  .removingFromSession(foreignPropertyStartDate)
              )
            case selection if selection.contains(responseYes) =>
              Future.successful(Redirect(nextPageUrl))
            case e =>
              Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
                s"[ForeignPropertyStartDateCheckController][handleSubmitRequest]: invalid form submission: $e")
              Future(itvcErrorHandler.showInternalServerError())
          }
        )
      case _ =>
        Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
          s"[ForeignPropertyStartDateCheckController][handleSubmitRequest]: failed to get $foreignPropertyStartDate from session")
        Future(itvcErrorHandler.showInternalServerError())
    }

  }


}