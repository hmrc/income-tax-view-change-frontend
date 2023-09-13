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

  def show(isAgent: Boolean,
           isChange: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleShowRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      isChange = isChange
    )
  }

  def submit(isAgent: Boolean,
             isChange: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleSubmitRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      isChange = isChange
    )
  }

  private def handleShowRequest(incomeSourceType: IncomeSourceType,
                                isAgent: Boolean,
                                isChange: Boolean)
                               (implicit user: MtdItUser[_]): Future[Result] = {

    Future.successful(
      if (isEnabled(IncomeSources))
        getAndValidateStartDate(incomeSourceType) match {
          case Right(startDate) =>
            Ok(
              addIncomeSourceStartDateCheckView(
                isAgent = isAgent,
                backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
                form = form(incomeSourceType.addStartDateCheckMessagesPrefix),
                postAction = getPostAction(incomeSourceType, isAgent, isChange),
                incomeSourceStartDate = longDate(startDate.toLocalDate).toLongDate
              )
            )
          case Left(ex) =>
            Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
              s"Failed to get income source start date from session, reason: ${ex.getMessage}")
            showInternalServerError(isAgent)
        }
      else Ok(customNotFoundErrorView())
    )
  }

  private def handleSubmitRequest(incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  isChange: Boolean)
                                 (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.addStartDateCheckMessagesPrefix
    Future.successful(
      if (isEnabled(IncomeSources))
        getAndValidateStartDate(incomeSourceType) match {
          case Right(startDate) =>
            form(messagesPrefix).bindFromRequest().fold(
              formWithErrors => {
                BadRequest(
                  addIncomeSourceStartDateCheckView(
                    isAgent = isAgent,
                    form = formWithErrors,
                    incomeSourceStartDate = longDate(startDate).toLongDate,
                    backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
                    postAction = getPostAction(incomeSourceType, isAgent, isChange)
                  )
                )
              },
              formData => {
                handleValidForm(
                  isAgent = isAgent,
                  validForm = formData,
                  incomeSourceStartDate = startDate,
                  incomeSourceType = incomeSourceType,
                  backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
                  successUrl = getSuccessUrl(incomeSourceType, isAgent, isChange)
                )
              }
            )
          case Left(ex) =>
            Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
              s"Failed to get income source start date from session, reason: ${ex.getMessage}")
            showInternalServerError(isAgent)
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

  private def showInternalServerError(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
  }

  private def handleValidForm(backUrl: String,
                              validForm: form,
                              isAgent: Boolean,
                              successUrl: String,
                              incomeSourceStartDate: String,
                              incomeSourceType: IncomeSourceType)
                             (implicit mtdItUser: MtdItUser[_]): Result = {

    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption

    (formResponse, incomeSourceType) match {
      case (Some(form.responseNo), _) =>
        Redirect(backUrl)
          .removingFromSession(
            incomeSourceType.startDateSessionKey
          )
      case (Some(form.responseYes), SelfEmployment) =>
        Redirect(successUrl)
          .addingToSession(
            SessionKeys.addBusinessAccountingPeriodStartDate -> incomeSourceStartDate,
            SessionKeys.addBusinessAccountingPeriodEndDate -> dateService.getAccountingPeriodEndDate(incomeSourceStartDate)
          )
      case (Some(form.responseYes), _) =>
        Redirect(successUrl)
      case _ =>
        Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleValidForm] - Unexpected response, isAgent = $isAgent")
        showInternalServerError(isAgent)
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

  private def getBackUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): String = {
    routes.AddIncomeSourceStartDateController.show(isAgent, isChange, incomeSourceType).url
  }

  private def getPostAction(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Call = {
    routes.AddIncomeSourceStartDateCheckController.submit(isAgent, isChange, incomeSourceType)
  }

  private def getSuccessUrl(incomeSourceType: IncomeSourceType,
                            isAgent: Boolean,
                            isChange: Boolean): String = {

    ((isAgent, isChange, incomeSourceType) match {
      case (_,     false, SelfEmployment) => routes.AddBusinessTradeController.show(isAgent, isChange)
      case (false, _,     SelfEmployment) => routes.CheckBusinessDetailsController.show()
      case (_,     _,     SelfEmployment) => routes.CheckBusinessDetailsController.showAgent()
      case (false, false, _)              => routes.IncomeSourcesAccountingMethodController.show(incomeSourceType.key)
      case (_,     false, _)              => routes.IncomeSourcesAccountingMethodController.showAgent(incomeSourceType.key)
      case (false, _,     UkProperty)     => routes.CheckUKPropertyDetailsController.show()
      case (_,     _,     UkProperty)     => routes.CheckUKPropertyDetailsController.showAgent()
      case (false, _,     _)              => routes.ForeignPropertyCheckDetailsController.show()
      case (_,     _,     _)              => routes.ForeignPropertyCheckDetailsController.showAgent()
    }).url
  }
}
