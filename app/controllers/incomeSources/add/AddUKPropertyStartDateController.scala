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

///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers.incomeSources.add
//
//import auth.{FrontendAuthorisedFunctions, MtdItUser}
//import config.featureswitch.{FeatureSwitching, IncomeSources}
//import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
//import controllers.agent.predicates.ClientConfirmedController
//import controllers.predicates._
//import forms.incomeSources.add.AddUKPropertyStartDateForm
//import forms.utils.SessionKeys.addUkPropertyStartDate
//import implicits.ImplicitDateFormatterImpl
//import play.api.Logger
//import play.api.i18n.{I18nSupport, Messages}
//import play.api.mvc._
//import services.{DateService, IncomeSourceDetailsService}
//import uk.gov.hmrc.http.HeaderCarrier
//import views.html.errorPages.CustomNotFoundError
//import views.html.incomeSources.add.{AddIncomeSourceStartDate, AddUKPropertyStartDate}
//
//import javax.inject.Inject
//import scala.concurrent.{ExecutionContext, Future}
//
//class AddUKPropertyStartDateController @Inject()(val authenticate: AuthenticationPredicate,
//                                                 val authorisedFunctions: FrontendAuthorisedFunctions,
//                                                 val checkSessionTimeout: SessionTimeoutPredicate,
//                                                 val incomeSourceDetailsService: IncomeSourceDetailsService,
//                                                 val retrieveBtaNavBar: NavBarPredicate,
//                                                 val retrieveIncomeSources: IncomeSourceDetailsPredicate,
//                                                 val retrieveNino: NinoPredicate,
//                                                 val view: AddIncomeSourceStartDate,
//                                                 val customNotFoundErrorView: CustomNotFoundError)
//                                                (implicit val appConfig: FrontendAppConfig,
//                                                 val dateFormatter: ImplicitDateFormatterImpl,
//                                                 val dateService: DateService,
//                                                 mcc: MessagesControllerComponents,
//                                                 val ec: ExecutionContext,
//                                                 val itvcErrorHandler: ItvcErrorHandler,
//                                                 val itvcErrorHandlerAgent: AgentItvcErrorHandler)
//  extends ClientConfirmedController with FeatureSwitching with I18nSupport {
//
//  private lazy val messagesPrefix = "incomeSources.add.UKPropertyStartDate"
//
//  def handleRequest(isAgent: Boolean)
//                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
//
//    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
//    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url else
//      controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
//    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.AddUKPropertyStartDateController.submitAgent() else
//      controllers.incomeSources.add.routes.AddUKPropertyStartDateController.submit()
//    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
//
//    if (incomeSourcesEnabled) {
//      Future.successful(Ok(view(
//        messagesPrefix = messagesPrefix,
//        form = AddUKPropertyStartDateForm()(dateFormatter, dateService, messages),
//        postAction = postAction,
//        isAgent = isAgent,
//        backUrl = backUrl)(messages, user)))
//    } else {
//      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
//    } recover {
//      case ex: Exception =>
//        Logger("application").error(s"${
//          if (isAgent) "[Agent]"
//        }" +
//          s"Error getting AddUKPropertyStartDate page: ${
//            ex.getMessage
//          }")
//        errorHandler.showInternalServerError()
//    }
//  }
//
//  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
//    val (postAction, backUrl, redirect) = {
//      if (isAgent)
//        (routes.AddUKPropertyStartDateController.submitAgent(),
//          routes.AddIncomeSourceController.showAgent().url,
//          routes.CheckUKPropertyStartDateController.showAgent())
//      else
//        (routes.AddUKPropertyStartDateController.submit(),
//          routes.AddIncomeSourceController.show().url,
//          routes.CheckUKPropertyStartDateController.show())
//
//    }
//    AddUKPropertyStartDateForm().bindFromRequest().fold(
//      hasErrors => Future.successful(BadRequest(view(
//        messagesPrefix = "incomeSources.add.UKPropertyStartDate",
//        form = hasErrors,
//        postAction = postAction,
//        backUrl = backUrl,
//        isAgent = isAgent
//      ))),
//      validatedInput =>
//        Future.successful(Redirect(redirect)
//          .addingToSession(addUkPropertyStartDate -> validatedInput.date.toString))
//    )
//  }
//
//
//  def show(): Action[AnyContent] =
//    (checkSessionTimeout andThen authenticate andThen retrieveNino
//      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
//      implicit user =>
//        handleRequest(
//          isAgent = false
//        )
//    }
//
//  def showAgent(): Action[AnyContent] = Authenticated.async {
//    implicit request =>
//      implicit user =>
//        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
//          implicit mtdItUser =>
//            handleRequest(
//              isAgent = true
//            )
//        }
//  }
//
//  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
//    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
//    implicit user =>
//      handleSubmitRequest(isAgent = false)
//  }
//
//  def submitAgent: Action[AnyContent] = Authenticated.async {
//    implicit request =>
//      implicit user =>
//        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
//          implicit mtdItUser =>
//            handleSubmitRequest(isAgent = true)
//        }
//  }
//
//  def change(): Action[AnyContent] =
//    (checkSessionTimeout andThen authenticate andThen retrieveNino
//      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
//      implicit user =>
//        Future.successful(Ok("Change UK Property Start Date - Individual"))
//    }
//
//  def changeAgent(): Action[AnyContent] = Authenticated.async {
//    implicit request =>
//      implicit user =>
//        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
//          implicit mtdItUser =>
//            Future.successful(Ok("Change UK Property Start Date - Agent"))
//        }
//  }
//}
