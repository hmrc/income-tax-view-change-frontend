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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.add.{AddIncomeSourceStartDateCheckForm => form}
import forms.utils.SessionKeys.{addBusinessAccountingPeriodEndDate, addBusinessAccountingPeriodStartDate}
import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService, SessionService}
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
                                                        val languageUtils: LanguageUtils,
                                                        val sessionService: SessionService)
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

    if (isEnabled(IncomeSources))
      getAndValidateStartDate(incomeSourceType) flatMap {
        case Right(startDate) =>
          Future.successful(Ok(
            addIncomeSourceStartDateCheckView(
              isAgent = isAgent,
              backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
              form = form(incomeSourceType.addStartDateCheckMessagesPrefix),
              postAction = getPostAction(incomeSourceType, isAgent, isChange),
              incomeSourceStartDate = longDate(startDate.toLocalDate).toLongDate
            )
          ))
        case Left(ex) =>
          Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
            s"Failed to get income source start date from session, reason: ${ex.getMessage}")
          Future.successful(showInternalServerError(isAgent))
      }
    else Future.successful(Ok(customNotFoundErrorView()))
  }

  private def handleSubmitRequest(incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  isChange: Boolean)
                                 (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.addStartDateCheckMessagesPrefix
    if (isEnabled(IncomeSources))
      getAndValidateStartDate(incomeSourceType) flatMap {
        case Right(startDate) =>
          form(messagesPrefix).bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(
                addIncomeSourceStartDateCheckView(
                  isAgent = isAgent,
                  form = formWithErrors,
                  incomeSourceStartDate = longDate(startDate).toLongDate,
                  backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
                  postAction = getPostAction(incomeSourceType, isAgent, isChange)
                )
              ))
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
          Future.successful(showInternalServerError(isAgent))
      }
    else Future.successful(Ok(customNotFoundErrorView())
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

  private def handleValidForm(backUrl: String,
                              validForm: form,
                              isAgent: Boolean,
                              successUrl: String,
                              incomeSourceStartDate: String,
                              incomeSourceType: IncomeSourceType)
                             (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption

    (formResponse, incomeSourceType) match {
      case (Some(form.responseNo), _) =>
        removeSessionData(Seq(incomeSourceType.startDateSessionKey), Redirect(backUrl), showInternalServerError(isAgent))
      case (Some(form.responseYes), SelfEmployment) =>
        setSessionData(Seq(
          (addBusinessAccountingPeriodStartDate, incomeSourceStartDate),
          (addBusinessAccountingPeriodEndDate, dateService.getAccountingPeriodEndDate(incomeSourceStartDate))
        ),
          Redirect(successUrl)
        ) map {
          case Left(_) => showInternalServerError(isAgent)
          case Right(result) => result
        }
      case (Some(form.responseYes), _) =>
        Future.successful(Redirect(successUrl))
      case _ =>
        Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleValidForm] - Unexpected response, isAgent = $isAgent")
        Future.successful(showInternalServerError(isAgent))
    }
  }

  private def removeSessionData(keys: Seq[String], successResult: Result, errorResult: Result)(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    sessionService.remove(keys, successResult) map {
      case Left(_) => errorResult
      case Right(result) => result
    }
  }

  private def setSessionData(keyValuePairs: Seq[(String, String)], result: Result)(implicit mtdItUser: MtdItUser[_]): Future[Either[Throwable, Result]] = {
    keyValuePairs.foldLeft[Future[Either[Throwable, Result]]](Future {
      Right(result)
    }) { (acc, keyValue) =>
      val result = for {
        resAccumulator <- acc
      } yield resAccumulator match {
        case Right(res) =>
          sessionService.set(keyValue._1, keyValue._2, res)
        case Left(ex) =>
          Future {
            Left(ex)
          }
      }
      result.flatten
    }
  }

  private def getAndValidateStartDate(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Either[Throwable, String]] = {
    sessionService.get(incomeSourceType.startDateSessionKey) map {
      case Left(ex) => Left(new Error(s"Could not retrieve start date from session storage: ${ex.getMessage}"))
      case Right(Some(date)) if Try(date.toLocalDate).toOption.isDefined => Right(date)
      case Right(Some(date)) => Left(new Error(s"Could not parse $date as LocalDate"))
      case Right(None) => Left(new Error(s"Session value not found for Key: ${incomeSourceType.startDateSessionKey}"))
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
      case (false, false, _)              => routes.IncomeSourcesAccountingMethodController.show(incomeSourceType)
      case (_,     false, _)              => routes.IncomeSourcesAccountingMethodController.showAgent(incomeSourceType)
      case (false, _,     UkProperty)     => routes.CheckUKPropertyDetailsController.show()
      case (_,     _,     UkProperty)     => routes.CheckUKPropertyDetailsController.showAgent()
      case (false, _,     _)              => routes.ForeignPropertyCheckDetailsController.show()
      case (_,     _,     _)              => routes.ForeignPropertyCheckDetailsController.showAgent()
    }).url
  }
}
