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
import controllers.routes
import forms.incomeSources.add.BusinessStartDateForm
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.add.AddIncomeSourceStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessStartDateController @Inject()(authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               checkSessionTimeout: SessionTimeoutPredicate,
                                               retrieveNino: NinoPredicate,
                                               val addIncomeSourceStartDate: AddIncomeSourceStartDate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               incomeSourceDetailsService: IncomeSourceDetailsService)
                                              (implicit val appConfig: FrontendAppConfig,
                                               implicit val itvcErrorHandler: ItvcErrorHandler,
                                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                               implicit val dateFormatter: ImplicitDateFormatterImpl,
                                               implicit val dateService: DateService,
                                               implicit override val mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def show: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = backUrl,
        postAction = postAction,
        homePageCall = homePageCall,
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
              postAction = postActionAgent,
              homePageCall = homePageCallAgent,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def handleRequest(isAgent: Boolean,
                    backUrl: String,
                    postAction: Call,
                    homePageCall: Call,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(homePageCall))
    } else {
      Future.successful(Ok(addIncomeSourceStartDate(
        form = BusinessStartDateForm(),
        postAction = postAction,
        backUrl = backUrl,
        isAgent = isAgent,
        messagesPrefix = "add-business-start-date"
      )))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
          s"[AddBusinessStartDateController][handleRequest] - Error: ${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handelSubmitRequest(isAgent = false)
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handelSubmitRequest(isAgent = true)
        }
  }

  def handelSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val (postActionLocal, backAction, redirectAction) = if (isAgent) (postActionAgent, backUrlAgent, redirectCallAgent) else (postAction, backUrl, redirectCall)
    BusinessStartDateForm().bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(addIncomeSourceStartDate(
          form = formWithErrors,
          postAction = postActionLocal,
          backUrl = backAction,
          isAgent = isAgent,
          messagesPrefix = "add-business-start-date"
        ))),
      formData => {
        val businessStartDate = formData.date
        Future.successful(
          Redirect(redirectAction)
            .addingToSession(SessionKeys.addBusinessStartDate -> businessStartDate.toString)
        )
      }
    )
  }

  def changeBusinessStartDate(): Action[AnyContent] = Action {
    Ok("Change Business Start Date WIP")
  }

  def changeBusinessStartDateAgent(): Action[AnyContent] = Action {
    Ok("Agent Change Business Start Date WIP")
  }

  lazy val backUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.show().url
  lazy val backUrlAgent: String = controllers.incomeSources.add.routes.AddBusinessNameController.showAgent().url

  lazy val postAction: Call = controllers.incomeSources.add.routes.AddBusinessStartDateController.submit()
  lazy val postActionAgent: Call = controllers.incomeSources.add.routes.AddBusinessStartDateController.submitAgent()

  lazy val homePageCall: Call = routes.HomeController.show()
  lazy val homePageCallAgent: Call = routes.HomeController.showAgent

  lazy val redirectCall: Call = controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.show()
  lazy val redirectCallAgent: Call = controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.showAgent()

}
