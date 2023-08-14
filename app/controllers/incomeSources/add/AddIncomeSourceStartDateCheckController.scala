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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.add.{AddBusinessStartDateCheckForm, AddForeignPropertyStartDateCheckForm, AddIncomeSourceStartDateCheckForm, AddUKPropertyStartDateCheckForm, IncomeSourceStartDateCheckForm}
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

  def showUKProperty: Action[AnyContent] = show(UkProperty)
  def showUKPropertyAgent: Action[AnyContent] = showAgent(UkProperty)
  def showForeignProperty: Action[AnyContent] = show(ForeignProperty)
  def showForeignPropertyAgent: Action[AnyContent] = showAgent(ForeignProperty)
  def showSoleTraderBusiness: Action[AnyContent] = show(SelfEmployment)
  def showSoleTraderBusinessAgent: Action[AnyContent] = showAgent(SelfEmployment)
  def submitUKProperty: Action[AnyContent] = submit(UkProperty)
  def submitUKPropertyAgent: Action[AnyContent] = submitAgent(UkProperty)
  def submitForeignProperty: Action[AnyContent] = submit(ForeignProperty)
  def submitForeignPropertyAgent: Action[AnyContent] = submitAgent(ForeignProperty)
  def submitSoleTraderBusiness: Action[AnyContent] = submit(SelfEmployment)
  def submitSoleTraderBusinessAgent: Action[AnyContent] = submitAgent(SelfEmployment)

  private def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
    }
  }

  private def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleSubmitRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
    }
  }

  private def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }

  private def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    if(isEnabled(IncomeSources)) {
      (getStartDate(incomeSourceType), getCalls(isAgent, incomeSourceType)) match {
        case (None, _) =>
          Logger("application")
            .error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
              s"failed to get start date for $incomeSourceType from session")
          Future(
            if(isAgent) itvcErrorHandlerAgent.showInternalServerError()
            else itvcErrorHandler.showInternalServerError()
          )
        case (Some(startDate), (backCall, postAction, _)) =>
          Future(
            Ok(
              addIncomeSourceStartDateCheckView(
                form = AddIncomeSourceStartDateCheckForm(incomeSourceType.addIncomeSourceStartDateCheckMessagesPrefix),
                postAction = postAction,
                backUrl = backCall.url,
                isAgent = isAgent,
                incomeSourceStartDate = longDate(startDate.toLocalDate).toLongDate
              )
            )
          )
      }
    } else {
      Future(
        Ok(
          customNotFoundErrorView()
        )
      )
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
        s"Error getting AddIncomeSourceStartDateCheck page: ${ex.getMessage}")
      if(isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
  }

  def handleSubmitRequest(isAgent: Boolean,
                          incomeSourceType: IncomeSourceType)
                         (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    if (isEnabled(IncomeSources)) {
      (getStartDate(incomeSourceType), getCalls(isAgent, incomeSourceType)) match {
        case (None, _) =>
          Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
            s"failed to get start date for $incomeSourceType from session")
          Future(
            if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
            else itvcErrorHandler.showInternalServerError()
          )
        case (Some(startDate), (backCall, postAction, successCall)) =>

          AddIncomeSourceStartDateCheckForm(incomeSourceType.addIncomeSourceStartDateCheckMessagesPrefix)
            .bindFromRequest().fold(
              formWithErrors =>
                Future(
                  BadRequest(
                    addIncomeSourceStartDateCheckView(
                      form = formWithErrors,
                      postAction = postAction,
                      backUrl = backCall.url,
                      isAgent = isAgent,
                      incomeSourceStartDate = longDate(startDate.toLocalDate).toLongDate
                    )
                  )
                ),
              _.toFormMap(AddIncomeSourceStartDateCheckForm.response).headOption match {
                case Some(AddIncomeSourceStartDateCheckForm.responseNo) =>
                  Future.successful(
                    Redirect(backCall)
                      .removingFromSession(
                        incomeSourceType match {
                          case SelfEmployment => businessStartDate
                          case UkProperty => addUkPropertyStartDate
                          case SelfEmployment => foreignPropertyStartDate
                        }
                      )
                  )
                case Some(AddIncomeSourceStartDateCheckForm.responseYes) =>
                  Future.successful(Redirect(successCall))
                case ex =>
                  Logger("application").error(s"[ForeignPropertyStartDateCheckController][handleSubmitRequest]: invalid form submission: $ex")
                  Future(
                    if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
                    else itvcErrorHandler.showInternalServerError()
                  )
              }
            )
      }
    } else {
      Future(
        Ok(
          customNotFoundErrorView()
        )
      )
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
        s"Error getting AddIncomeSourceStartDateCheck page: ${ex.getMessage}")
      if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
  }

  private def getStartDate(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Option[String] = {
    incomeSourceType match {
      case SelfEmployment => user.session.get(businessStartDate)
      case UkProperty => user.session.get(addUkPropertyStartDate)
      case ForeignProperty => user.session.get(foreignPropertyStartDate)
    }
  }

  private def getCalls(isAgent: Boolean, incomeSourceType: IncomeSourceType): (Call, Call, Call) = {

    (isAgent, incomeSourceType) match {
      case (false, SelfEmployment) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitSoleTraderBusiness,
          controllers.incomeSources.add.routes.AddBusinessTradeController.show()
        )
      case (false, UkProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitUKProperty,
          controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.show()
        )
      case (false, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitForeignProperty,
          controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show()
        )
      case (true, SelfEmployment) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitSoleTraderBusinessAgent,
          controllers.incomeSources.add.routes.AddBusinessTradeController.showAgent()
        )
      case (true, UkProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitUKPropertyAgent,
          controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.showAgent()
        )
      case (true, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitForeignPropertyAgent,
          controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent()
        )
    }
  }

  private def getMessagesPrefix(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix
      case ForeignProperty => ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix
      case UkProperty => UkProperty.addIncomeSourceStartDateCheckMessagesPrefix
    }
  }
}