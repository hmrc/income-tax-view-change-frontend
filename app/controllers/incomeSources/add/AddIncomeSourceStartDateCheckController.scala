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
import play.api.http.HttpVerbs
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

  def handleRequest(incomeSourceKey: String,
                    isAgent: Boolean,
                    isChange: Boolean
                   ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    user.method match {
      case HttpVerbs.GET => show(IncomeSourceType.get(incomeSourceKey), isAgent, isChange)
      case HttpVerbs.POST => submit(IncomeSourceType.get(incomeSourceKey), isAgent, isChange)
    }
  }

  private def show(incomeSourceType: IncomeSourceType,
                   isAgent: Boolean,
                   isUpdate: Boolean)
                  (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    Future.successful(
      if (isEnabled(IncomeSources)) {
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
      } else Ok(customNotFoundErrorView())
    )
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
        s"Error getting AddIncomeSourceStartDateCheck page: ${ex.getMessage}")
      getErrorHandler(isAgent).showInternalServerError()
  }

  private def submit(incomeSourceType: IncomeSourceType,
                     isAgent: Boolean,
                     isUpdate: Boolean)
                    (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.addStartDateCheckMessagesPrefix

    Future.successful(
      if (isEnabled(IncomeSources)) {
        (getAndValidateStartDate(incomeSourceType), getCalls(isAgent, isUpdate, incomeSourceType)) match {
          case (Right(startDate), (backCall, postAction, successCall)) =>
            form(messagesPrefix).bindFromRequest().fold(
              formWithErrors =>
                BadRequest(
                  addIncomeSourceStartDateCheckView(
                    isAgent = isAgent,
                    form = formWithErrors,
                    backUrl = backCall.url,
                    postAction = postAction,
                    incomeSourceStartDate = longDate(startDate).toLongDate
                  )
                ),
              formData =>
                handleValidForm(
                  validForm = formData,
                  backCall = backCall,
                  successCall = successCall,
                  incomeSourceStartDate = startDate,
                  incomeSourceType = incomeSourceType
                )
            )
          case (Left(ex), _) =>
            Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
              s"Failed to get income source start date from session, reason: ${ex.getMessage}")
            getErrorHandler(isAgent).showInternalServerError()
        }
      } else Ok(customNotFoundErrorView())
    )
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
        s"Error getting AddIncomeSourceStartDateCheck page: ${ex.getMessage}")
      getErrorHandler(isAgent).showInternalServerError()
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent) {
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    } else {
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
    }
  }

  private def handleValidForm(backCall: Call,
                              validForm: form,
                              successCall: Call,
                              incomeSourceStartDate: String,
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

  private def getErrorHandler(isAgent: Boolean): ShowInternalServerError = {
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler
  }

  private def getCalls(isAgent: Boolean,
                       isUpdate: Boolean,
                       incomeSourceType: IncomeSourceType): (Call, Call, Call) = {
    (
      routes.AddIncomeSourceStartDateController.handleRequest(incomeSourceType.key, isAgent, isUpdate),
      routes.AddIncomeSourceStartDateCheckController.handleRequest(incomeSourceType.key, isAgent, isUpdate),
      incomeSourceType match {
        case SelfEmployment =>
          (isAgent, isUpdate) match {
            case (false, false) => routes.AddBusinessTradeController.show()
            case (false, true) => routes.CheckBusinessDetailsController.show()
            case (true, false) => routes.AddBusinessTradeController.showAgent()
            case (true, true) => routes.CheckBusinessDetailsController.showAgent()
          }
        case UkProperty =>
          (isAgent, isUpdate) match {
            case (false, false) => routes.IncomeSourcesAccountingMethodController.show(UkProperty.key)
            case (false, true) => routes.CheckUKPropertyDetailsController.show()
            case (true, false) => routes.IncomeSourcesAccountingMethodController.showAgent(UkProperty.key)
            case (true, true) => routes.CheckUKPropertyDetailsController.showAgent()
          }
        case ForeignProperty =>
          (isAgent, isUpdate) match {
            case (false, false) => routes.IncomeSourcesAccountingMethodController.show(ForeignProperty.key)
            case (false, true) => routes.ForeignPropertyCheckDetailsController.show()
            case (true, false) => routes.IncomeSourcesAccountingMethodController.showAgent(ForeignProperty.key)
            case (true, true) => routes.ForeignPropertyCheckDetailsController.showAgent()
          }
      }
    )
  }
}
