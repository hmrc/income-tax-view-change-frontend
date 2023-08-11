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
import forms.incomeSources.add.{AddBusinessStartDateCheckForm, AddForeignPropertyStartDateCheckForm, AddUKPropertyStartDateCheckForm, IncomeSourceStartDateCheckForm}
import forms.utils.SessionKeys.{addUkPropertyStartDate, businessStartDate, foreignPropertyStartDate}
import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.{AddIncomeSourceStartDateCheck, ForeignPropertyStartDateCheck}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddIncomeSourceStartDateCheckController @Inject()(authenticate: AuthenticationPredicate,
                                                     val authorisedFunctions: AuthorisedFunctions,
                                                     val checkSessionTimeout: SessionTimeoutPredicate,
                                                     val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                     val retrieveBtaNavBar: NavBarPredicate,
                                                     val retrieveNino: NinoPredicate,
                                                     val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                     val addIncomeSourceStartDateCheckView: AddIncomeSourceStartDateCheck,
                                                     val customNotFoundErrorView: CustomNotFoundError,
                                                     val languageUtils: LanguageUtils)
                                                    (implicit val appConfig: FrontendAppConfig,
                                                     mcc: MessagesControllerComponents,
                                                     val ec: ExecutionContext,
                                                     val itvcErrorHandler: ItvcErrorHandler,
                                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitDateFormatter {

  def showUKProperty: Action[AnyContent] = handleRequestMethod(UKProperty)(isGet = true)
  def showUKPropertyAgent: Action[AnyContent] = handleRequestMethodAgent(UKProperty)(isGet = true)
  def showForeignProperty: Action[AnyContent] = handleRequestMethod(ForeignProperty)(isGet = true)
  def showForeignPropertyAgent: Action[AnyContent] = handleRequestMethodAgent(ForeignProperty)(isGet = true)
  def showSoleTraderBusiness: Action[AnyContent] = handleRequestMethod(SoleTraderBusiness)(isGet = true)
  def showSoleTraderBusinessAgent: Action[AnyContent] = handleRequestMethodAgent(SoleTraderBusiness)(isGet = true)

  def submitUKProperty: Action[AnyContent] = handleRequestMethod(UKProperty)(isGet = false)
  def submitUKPropertyAgent: Action[AnyContent] = handleRequestMethodAgent(UKProperty)(isGet = false)
  def submitForeignProperty: Action[AnyContent] = handleRequestMethod(ForeignProperty)(isGet = false)
  def submitForeignPropertyAgent: Action[AnyContent] = handleRequestMethodAgent(ForeignProperty)(isGet = false)
  def submitSoleTraderBusiness: Action[AnyContent] = handleRequestMethod(SoleTraderBusiness)(isGet = false)
  def submitSoleTraderBusinessAgent: Action[AnyContent] = handleRequestMethodAgent(SoleTraderBusiness)(isGet = false)

  private def handleRequestMethod(incomeSourceType: IncomeSourceType)
                                 (isGet: Boolean): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      if (isGet) {
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
      } else {
        handleSubmitRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
      }
  }

  private def handleRequestMethodAgent(incomeSourceType: IncomeSourceType)
                                      (isGet: Boolean): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            if (isGet) {
              handleRequest(
                isAgent = true,
                incomeSourceType = incomeSourceType
              )
            } else {
              handleSubmitRequest(
                isAgent = false,
                incomeSourceType = incomeSourceType
              )
            }
        }
  }

  lazy val backUrl: String = routes.AddIncomeSourceStartDateController.showForeignProperty.url
  lazy val backUrlAgent: String = routes.AddIncomeSourceStartDateController.showForeignPropertyAgent.url
  lazy val nextPage: String = routes.ForeignPropertyAccountingMethodController.show().url
  lazy val nextPageAgent: String = routes.ForeignPropertyAccountingMethodController.showAgent().url

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false, backUrl = backUrl)
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

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    (isEnabled(IncomeSources), getStartDate(incomeSourceType), getCallsAndFormModel(isAgent, incomeSourceType)) match {
      case (false, _, _) =>
        Future(
          Ok(
            customNotFoundErrorView()
          )
        )
      case (_, None, (_, _, _, errorHandler, _, _)) =>
        Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
          s"failed to get start date for $incomeSourceType from session")
        Future(
          errorHandler.showInternalServerError()
        )
      case (_, Some(startDate), (backCall, postAction, _, _, messagesPrefix, form)) =>
        Future(
          Ok(
            addIncomeSourceStartDateCheckView(
              form = form,
              postAction = postAction,
              backUrl = backCall.url,
              isAgent = isAgent,
              incomeSourceStartDate = longDate(startDate.toLocalDate).toLongDate,
              messagesPrefix = messagesPrefix
            )
          )
        )
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"Error getting AddIncomeSourceStartDateCheck page: ${ex.getMessage}")
      if(isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
  }

  val response = "start-date-check"


  def handleSubmitRequest(isAgent: Boolean,
                          incomeSourceType: IncomeSourceType)
                         (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    (isEnabled(IncomeSources), getStartDate(incomeSourceType), getCallsAndFormModel(isAgent, incomeSourceType)) match {
      case (false, _, _) =>
        Future(
          Ok(
            customNotFoundErrorView()
          )
        )
      case (_, None, (_, _, _, errorHandler, _, _)) =>
        Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
          s"failed to get start date for $incomeSourceType from session")
        Future(
          errorHandler.showInternalServerError()
        )
      case (_, Some(startDate), (backCall, postAction, successCall, _, messagesPrefix, form)) =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future(
              BadRequest(
                addIncomeSourceStartDateCheckView(
                  form = formWithErrors,
                  postAction = postAction,
                  backUrl = backCall.url,
                  isAgent = isAgent,
                  incomeSourceStartDate = longDate(startDate.toLocalDate).toLongDate,
                  messagesPrefix = messagesPrefix
                )
              )
            ),
          formData =>
            (formData) match {
              case (f: IncomeSourceStartDateCheckForm) => f.toFormMap(response) match {
                case Some("Yes") =>
              }
            }

        )
    }

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
            case Some("No") =>
              Future.successful(
                Redirect(backUrl)
                  .removingFromSession(foreignPropertyStartDate)
              )
            case Some("Yes") =>
              Future.successful(Redirect(nextPageUrl))
            case e =>
              Logger("application").error(s"[ForeignPropertyStartDateCheckController][handleSubmitRequest]: invalid form submission: $e")
              Future(itvcErrorHandler.showInternalServerError())
          }
        )
      case _ =>
        Logger("application").error(s"[ForeignPropertyStartDateCheckController][handleSubmitRequest]: failed to get $foreignPropertyStartDate from session")
        Future(itvcErrorHandler.showInternalServerError())
    }

  }

  private def getStartDate(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Option[String] = {
    incomeSourceType match {
      case SoleTraderBusiness => user.session.get(businessStartDate)
      case UKProperty => user.session.get(addUkPropertyStartDate)
      case ForeignProperty => user.session.get(foreignPropertyStartDate)
    }
  }

  private def getCallsAndFormModel(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                                  (implicit user: MtdItUser[_]): (Call, Call, Call, ShowInternalServerError, String, Form[_]) = {

    (isAgent, incomeSourceType) match {
      case (false, SoleTraderBusiness) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitSoleTraderBusiness,
          controllers.incomeSources.add.routes.AddBusinessTradeController.show(),
          itvcErrorHandler,
          "soleTraderBusinessMessagesPrefix",
          BusinessStartDateCheckForm,form
        )
      case (false, UKProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitUKProperty,
          controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent(),
          itvcErrorHandler,
          uKPropertyMessagesPrefix,
          AddUKPropertyStartDateForm()
        )
      case (false, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitForeignProperty,
          controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent(),
          itvcErrorHandler,
          foreignPropertyMessagesPrefix,
          addForeignPropertyStartDateForm(user, implicitly)
        )
      case (true, SoleTraderBusiness) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent,
          controllers.incomeSources.add.routes.AddBusinessTradeController.showAgent(),
          itvcErrorHandlerAgent,
          soleTraderBusinessMessagesPrefix,
          AddBusinessStartDateForm()
        )
      case (true, UKProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitUKPropertyAgent,
          controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent(),
          itvcErrorHandlerAgent,
          uKPropertyMessagesPrefix,
          AddUKPropertyStartDateForm()
        )
      case (true, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitForeignPropertyAgent,
          controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.showAgent(),
          itvcErrorHandlerAgent,
          foreignPropertyMessagesPrefix,
          addForeignPropertyStartDateForm(user, implicitly)
        )
    }
  }

  private sealed trait IncomeSourceType
  private case object UKProperty extends IncomeSourceType
  private case object ForeignProperty extends IncomeSourceType
  private case object SoleTraderBusiness extends IncomeSourceType
}