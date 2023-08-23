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

  def show(incomeSourceKey: String,
           isAgent: Boolean,
           isChange: Boolean
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    IncomeSourceType.get(incomeSourceKey) match {
      case Left(ex: Exception) => Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleShowRequest]: " +
        s"Failed fulfil show request: ${ex.getMessage}")
        Future.successful(getErrorHandler(isAgent).showInternalServerError())
      case Right(value) =>
        handleShowRequest(
          incomeSourceType = value,
          isAgent = isAgent,
          isUpdate = isChange
        )
    }
  }

  def submit(incomeSourceKey: String,
             isAgent: Boolean,
             isChange: Boolean
            ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>
    IncomeSourceType.get(incomeSourceKey) match {
      case Left(ex: Exception) => Logger("application").error(s"[AddIncomeSourceStartDateController][handleShowRequest]: " +
        s"Failed fulfil submit request: ${ex.getMessage}")
        Future.successful(getErrorHandler(isAgent).showInternalServerError())
      case Right(value) =>
        handleSubmitRequest(
          incomeSourceType = value,
          isAgent = isAgent,
          isUpdate = isChange
        )
    }
  }

  private def handleShowRequest(incomeSourceType: IncomeSourceType,
                                isAgent: Boolean,
                                isUpdate: Boolean)
                               (implicit user: MtdItUser[_]): Future[Result] = {

    Future.successful(
      if (isEnabled(IncomeSources))
        (getAndValidateStartDate(incomeSourceType), getCalls(isAgent, isUpdate, incomeSourceType)) match {
          case (Right(startDate), (backCall, postAction, _)) =>
            Ok(
              addIncomeSourceStartDateCheckView(
                isAgent = isAgent,
                backUrl = backCall.url,
                postAction = postAction,
                form = form(incomeSourceType.addStartDateCheckMessagesPrefix),
                incomeSourceStartDate = longDate(startDate.toLocalDate).toLongDate
              )
            )
          case (Left(ex), _) =>
            Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
              s"Failed to get income source start date from session, reason: ${ex.getMessage}")
            getErrorHandler(isAgent).showInternalServerError()
        }
      else Ok(customNotFoundErrorView())
    )
  }

  private def handleSubmitRequest(incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  isUpdate: Boolean)
                                 (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.addStartDateCheckMessagesPrefix

    Future.successful(
      if (isEnabled(IncomeSources))
        (getAndValidateStartDate(incomeSourceType), getCalls(isAgent, isUpdate, incomeSourceType)) match {
          case (Right(startDate), (backCall, postAction, successCall)) =>
            form(messagesPrefix).bindFromRequest().fold(
              formWithErrors => {
                BadRequest(
                  addIncomeSourceStartDateCheckView(
                    isAgent = isAgent,
                    form = formWithErrors,
                    backUrl = backCall.url,
                    postAction = postAction,
                    incomeSourceStartDate = longDate(startDate).toLongDate
                  )
                )
              },
              formData => {
                handleValidForm(
                  backCall = backCall,
                  validForm = formData,
                  successCall = successCall,
                  incomeSourceStartDate = startDate,
                  isAgent = isAgent,
                  incomeSourceType = incomeSourceType
                )
              }
            )
          case (Left(ex), _) =>
            Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
              s"Failed to get income source start date from session, reason: ${ex.getMessage}")
            getErrorHandler(isAgent).showInternalServerError()
        }
      else Ok(customNotFoundErrorView())
    )
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }

  private def getErrorHandler(isAgent: Boolean): ShowInternalServerError = {
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler
  }

  private def handleValidForm(backCall: Call,
                              validForm: form,
                              successCall: Call,
                              incomeSourceStartDate: String,
                              isAgent: Boolean,
                              incomeSourceType: IncomeSourceType)
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
      case (Some(_), _) => Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleValidForm] - Empty response, isAgent = $isAgent")
        if (isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
      case (None, _) => Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleValidForm] - Unexpected response, isAgent = $isAgent")
         if(isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
    }
  }

  private def getAndValidateStartDate(incomeSourceType: IncomeSourceType)
                                     (implicit user: MtdItUser[_]): Either[Throwable, String] = {

    val maybeIncomeSourceStartDate = user.session.get(incomeSourceType.startDateSessionKey)

    maybeIncomeSourceStartDate match {
      case None => Left(new Error(s"Session value not found for Key: ${incomeSourceType.startDateSessionKey}"))
      case Some(date) if Try(date.toLocalDate).toOption.isDefined => Right(date)
      case Some(invalidDate) => Left(new Error(s"Could not parse: $invalidDate as LocalDate for Key: ${incomeSourceType.startDateSessionKey}"))
    }
  }

  private def getCalls(isAgent: Boolean,
                       isUpdate: Boolean,
                       incomeSourceType: IncomeSourceType): (Call, Call, Call) = {
    (
      routes.AddIncomeSourceStartDateController.show(incomeSourceType.key, isAgent, isUpdate),
      routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType.key, isAgent, isUpdate),
      (isAgent, isUpdate, incomeSourceType) match {
        case (false, true,  SelfEmployment) => routes.CheckBusinessDetailsController.show()
        case (true,  true,  SelfEmployment) => routes.CheckBusinessDetailsController.showAgent()
        case (_,     false, SelfEmployment) => routes.AddBusinessTradeController.show(isAgent = isAgent, isChange = isUpdate)
        case (false, false, _)              => routes.IncomeSourcesAccountingMethodController.show(incomeSourceType.key)
        case (true,  false, _)              => routes.IncomeSourcesAccountingMethodController.showAgent(incomeSourceType.key)
        case (false, true, UkProperty)      => routes.CheckUKPropertyDetailsController.show()
        case (true,  true, UkProperty)      => routes.CheckUKPropertyDetailsController.showAgent()
        case (false, true, ForeignProperty) => routes.ForeignPropertyCheckDetailsController.show()
        case (true,  true, ForeignProperty) => routes.ForeignPropertyCheckDetailsController.showAgent()
      }
    )
  }
}
