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
import forms.incomeSources.add.{AddIncomeSourceStartDateCheckForm => form}
import forms.utils.SessionKeys
import forms.utils.SessionKeys.{addUkPropertyStartDate, businessStartDate, foreignPropertyStartDate}
import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDateCheck
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
                                                     implicit val dateService: DateService,
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

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    if(isDisabled(IncomeSources))
      Future {
        Ok(customNotFoundErrorView())
      }
    else
      (getAndValidateStartDate(incomeSourceType), getCalls(isAgent, incomeSourceType)) match {
        case (Left(ex), _) =>
          Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
            s"Failed to get income source start date from session, reason: ${ex.getMessage}")
          Future {
            getErrorHandler(isAgent).showInternalServerError()
          }
        case (Right(startDate), (backCall, postAction, _)) =>
          Future {
            Ok(
              addIncomeSourceStartDateCheckView(
                form = form(incomeSourceType.addIncomeSourceStartDateCheckMessagesPrefix),
                postAction = postAction,
                backUrl = backCall.url,
                isAgent = isAgent,
                incomeSourceStartDate = longDate(startDate.toLocalDate).toLongDate
              )
            )
          }
      }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
        s"Error getting AddIncomeSourceStartDateCheck page: ${ex.getMessage}")
      getErrorHandler(isAgent).showInternalServerError()
  }

  private def handleSubmitRequest(isAgent: Boolean,
                          incomeSourceType: IncomeSourceType)
                         (implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    if (isDisabled(IncomeSources))
      Future {
        Ok(customNotFoundErrorView())
      }
    else {
      (getAndValidateStartDate(incomeSourceType), getCalls(isAgent, incomeSourceType)) match {
        case (Left(ex), _) =>
          Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
            s"Failed to get income source start date from session, reason: ${ex.getMessage}")
          Future {
            getErrorHandler(isAgent).showInternalServerError()
          }
        case (Right(startDate), (backCall, postAction, successCall)) =>
          form(incomeSourceType.addIncomeSourceStartDateCheckMessagesPrefix).bindFromRequest().fold(
            formWithErrors =>
              Future {
                BadRequest(
                  addIncomeSourceStartDateCheckView(
                    form = formWithErrors,
                    postAction = postAction,
                    backUrl = backCall.url,
                    isAgent = isAgent,
                    incomeSourceStartDate = longDate(startDate).toLongDate
                  )
                )
              },
            formData =>
              Future.successful(
                handleValidForm(
                  validForm = formData,
                  incomeSourceType = incomeSourceType,
                  backCall = backCall,
                  successCall = successCall,
                  incomeSourceStartDate = startDate,
                  isAgent = isAgent
                )
              )
          )
      }
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
        s"Error getting AddIncomeSourceStartDateCheck page: ${ex.getMessage}")
      getErrorHandler(isAgent).showInternalServerError()
  }

  private def handleValidForm(validForm: form,
                              incomeSourceType: IncomeSourceType,
                              backCall: Call,
                              successCall: Call,
                              incomeSourceStartDate: String,
                              isAgent: Boolean)
                             (implicit mtdItUser: MtdItUser[_]): Result = {

    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption

    (formResponse, incomeSourceType) match {
      case (Some(form.responseNo), _) =>
        Redirect(backCall)
          .removingFromSession(
            incomeSourceType match {
              case SelfEmployment => businessStartDate
              case UkProperty => addUkPropertyStartDate
              case ForeignProperty => foreignPropertyStartDate
            }
          )
      case (Some(form.responseYes), SelfEmployment) =>
        Redirect(successCall)
          .addingToSession(
            SessionKeys.addBusinessAccountingPeriodStartDate -> incomeSourceStartDate,
            SessionKeys.addBusinessAccountingPeriodEndDate -> dateService.getAccountingPeriodEndDate(incomeSourceStartDate)
          )
      case (Some(form.responseYes), _) =>
        Redirect(successCall)
      case (Some(_), _) => Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleValidForm] - Unexpected response")
        if (isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
      case (None, _) => Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleValidForm] - Unexpected response")
         if(isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
    }
  }

  private def getAndValidateStartDate(incomeSourceType: IncomeSourceType)
                                     (implicit user: MtdItUser[_]): Either[Throwable, String] = {

    def getSessionKey(incomeSourceType: IncomeSourceType): String = {
      incomeSourceType match {
        case SelfEmployment => businessStartDate
        case UkProperty => addUkPropertyStartDate
        case ForeignProperty => foreignPropertyStartDate
      }
    }

    val maybeIncomeSourceStartDate = user.session.get(getSessionKey(incomeSourceType))

    maybeIncomeSourceStartDate match {
      case None => Left(new Error(s"Session value not found for Key: ${getSessionKey(incomeSourceType)}"))
      case Some(date) if Try(date.toLocalDate).toOption.isDefined => Right(date)
      case Some(invalidDate) => Left(new Error(s"Could not parse: $invalidDate as LocalDate for Key: ${getSessionKey(incomeSourceType)}"))
    }
  }

  private def getErrorHandler(isAgent: Boolean): ShowInternalServerError = {
    if(isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler
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
          controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(UkProperty.key)
        )
      case (false, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitForeignProperty,
          controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(ForeignProperty.key)
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
          controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(UkProperty.key)
        )
      case (true, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submitForeignPropertyAgent,
          controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(ForeignProperty.key)
        )
    }
  }
}