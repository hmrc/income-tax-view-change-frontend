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

package controllers

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.BusinessStartDateCheckForm.{response, responseNo, responseYes}
import forms.{BusinessNameForm, BusinessStartDateCheckForm}
import forms.utils.SessionKeys.businessStartDate
import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.incomeSources.add.AddBusinessStartDateCheck

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessStartDateCheckController @Inject()(authenticate: AuthenticationPredicate,
                                                    val authorisedFunctions: AuthorisedFunctions,
                                                    checkSessionTimeout: SessionTimeoutPredicate,
                                                    retrieveNino: NinoPredicate,
                                                    val addBusinessStartDateCheck: AddBusinessStartDateCheck,
                                                    val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                    val retrieveBtaNavBar: NavBarPredicate,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    incomeSourceDetailsService: IncomeSourceDetailsService)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    implicit val languageUtils: LanguageUtils,
                                                    implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    implicit override val mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitDateFormatter {

  lazy val backUrl: String = routes.AddBusinessStartDateController.show().url
  lazy val backUrlAgent: String = routes.AddBusinessStartDateController.showAgent().url

  lazy val postAction: Call = routes.AddBusinessStartDateCheckController.submit()
  lazy val postActionAgent: Call = routes.AddBusinessStartDateCheckController.submitAgent()

  lazy val addBusinessTradeUrl: String = routes.AddBusinessTradeController.show().url
  lazy val addBusinessTradeAgentUrl: String = routes.AddBusinessTradeController.showAgent().url

  lazy val homePageCall: Call = routes.HomeController.show()
  lazy val homePageCallAgent: Call = routes.HomeController.showAgent

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = backUrl,
        homePageCall = homePageCall,
        postAction = postAction,
        itvcErrorHandler = itvcErrorHandler
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              backUrl = backUrlAgent,
              homePageCall = homePageCallAgent,
              postAction = postActionAgent,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(
        isAgent = false,
        backUrl = backUrl,
        postAction = postAction,
        nextPageUrl = addBusinessTradeUrl,
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
              postAction = postActionAgent,
              nextPageUrl = addBusinessTradeAgentUrl,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def handleRequest(isAgent: Boolean,
                    backUrl: String,
                    homePageCall: Call,
                    postAction: Call,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit request: Request[_], mtdItUser: MtdItUser[_]): Future[Result] = {
    Future.successful(
      if (isDisabled(IncomeSources)) {
        Redirect(homePageCall)
      } else {
        request.session.get(businessStartDate) match {
          case Some(date) =>
            Ok(addBusinessStartDateCheck(
              form = BusinessNameForm.form,
              postAction = postAction,
              backUrl = backUrl,
              isAgent = isAgent,
              businessStartDate = longDate(date.toLocalDate).toLongDate
            ))
          case _ =>
            Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
              "[AddBusinessStartDateCheckController][handleRequest]: failed to get businessStartDate from session")
            itvcErrorHandler.showInternalServerError()
        }
      }
    )
  }

  def handleSubmitRequest(isAgent: Boolean,
                          backUrl: String,
                          postAction: Call,
                          nextPageUrl: String,
                          itvcErrorHandler: ShowInternalServerError)
                         (implicit request: Request[_], mtdItUser: MtdItUser[_]): Future[Result] = {

    Future.successful(
      request.session.get(businessStartDate) match {
        case Some(date) =>
          BusinessStartDateCheckForm.form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(addBusinessStartDateCheck(
                form = formWithErrors,
                postAction = postAction,
                backUrl = backUrl,
                isAgent = isAgent,
                businessStartDate = date.toLocalDate.toLongDate
              )),
            formData => formData.toFormMap(response).headOption match {
              case selection if selection.contains(responseNo) =>
                Redirect(backUrl)
                  .removingFromSession(businessStartDate)
              case selection if selection.contains(responseYes) =>
                Redirect(nextPageUrl)
              case _ =>
                BadRequest(addBusinessStartDateCheck(
                  form = BusinessStartDateCheckForm.form.fill(formData),
                  postAction = postAction,
                  backUrl = backUrl,
                  isAgent = isAgent,
                  businessStartDate = date.toLocalDate.toLongDate
                ))
            }
          )
        case _ =>
          Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
            "[AddBusinessStartDateCheckController][handleSubmitRequest]: failed to get businessStartDate from headers")
          itvcErrorHandler.showInternalServerError()
      }
    )
  }
}