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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.{BeforeSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.manageBusinesses.add.{AddIncomeSourceStartDateCheckForm => form}
import implicits.ImplicitDateFormatter
import models.UIJourneySessionData
import models.core.{Mode, NormalMode}
import models.admin.AccountingMethodJourney
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.AddIncomeSourceStartDateCheck

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddIncomeSourceStartDateCheckController @Inject()(val authActions: AuthActions,
                                                        val addIncomeSourceStartDateCheckView: AddIncomeSourceStartDateCheck,
                                                        val languageUtils: LanguageUtils,
                                                        val sessionService: SessionService,
                                                        val dateService: DateService,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with ImplicitDateFormatter with JourneyCheckerManageBusinesses {

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  def show(isAgent: Boolean,
           mode: Mode,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>

    handleShowRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      mode = mode
    )
  }

  def submit(isAgent: Boolean,
             mode: Mode,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>

    handleSubmitRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      mode = mode
    )
  }

  private def handleShowRequest(incomeSourceType: IncomeSourceType,
                                isAgent: Boolean,
                                mode: Mode)
                               (implicit user: MtdItUser[_]): Future[Result] = {

    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
      val dateStartedOpt = sessionData.addIncomeSourceData.flatMap(_.dateStarted)
      dateStartedOpt match {
        case Some(startDate: LocalDate) =>
          Future.successful {
            Ok(
              addIncomeSourceStartDateCheckView(
                isAgent = isAgent,
                backUrl = getBackUrl(incomeSourceType, isAgent, mode),
                form = form(incomeSourceType.addStartDateCheckMessagesPrefix),
                postAction = getPostAction(incomeSourceType, isAgent, mode),
                incomeSourceStartDate = longDate(startDate).toLongDate,
                incomeSourceType = incomeSourceType
              )
            )
          }
        case None =>
          Logger("application").error("" +
            "Failed to get income source start date from session")
          Future.successful(errorHandler(isAgent).showInternalServerError())
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController]${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  private def handleSubmitRequest(incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  mode: Mode)
                                 (implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val dateStartedOpt = sessionData.addIncomeSourceData.flatMap(_.dateStarted)
      dateStartedOpt match {
        case Some(startDate) =>
          val messagesPrefix = incomeSourceType.addStartDateCheckMessagesPrefix
          form(messagesPrefix).bindFromRequest().fold(
            formWithErrors =>
              Future.successful {
                BadRequest(
                  addIncomeSourceStartDateCheckView(
                    isAgent = isAgent,
                    form = formWithErrors,
                    incomeSourceStartDate = longDate(startDate).toLongDate,
                    backUrl = getBackUrl(incomeSourceType, isAgent, mode),
                    postAction = getPostAction(incomeSourceType, isAgent, mode),
                    incomeSourceType = incomeSourceType
                  )
                )
              },
            formData =>
              handleValidForm(
                isAgent = isAgent,
                mode = mode,
                validForm = formData,
                incomeSourceStartDate = startDate,
                incomeSourceType = incomeSourceType,
                sessionData = sessionData
              )
          )
        case None =>
          Logger("application").error("" +
            "Failed to get income source start date from session")
          Future.successful(errorHandler(isAgent).showInternalServerError())
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController]${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  private def handleValidForm(validForm: form,
                              isAgent: Boolean,
                              mode: Mode,
                              incomeSourceStartDate: LocalDate,
                              incomeSourceType: IncomeSourceType,
                              sessionData: UIJourneySessionData)
                             (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption
    val successUrl = getSuccessUrl(incomeSourceType, isAgent, mode)

    (formResponse, incomeSourceType) match {
      case (Some(form.responseNo), _) => removeDateFromSessionAndGoBack(incomeSourceType, isAgent, mode, sessionData)
      case (Some(form.responseYes), SelfEmployment) => updateAccountingPeriodForSE(incomeSourceStartDate, successUrl, isAgent, sessionData)
      case (Some(form.responseYes), _) => Future.successful(Redirect(successUrl))
      case _ =>
        Logger("application").error(s"Unexpected response, isAgent = $isAgent")
        Future.successful(errorHandler(isAgent).showInternalServerError())
    }
  }

  private def removeDateFromSessionAndGoBack(incomeSourceType: IncomeSourceType, isAgent: Boolean, mode: Mode, sessionData: UIJourneySessionData)
                                            (implicit request: Request[_]): Future[Result] = {

    val backUrl = getBackUrl(incomeSourceType, isAgent, mode)

    sessionData.addIncomeSourceData match {
      case Some(addIncomeSourceData) =>
        val updatedAddIncomeSourceData = addIncomeSourceData.copy(
          accountingPeriodStartDate = None,
          accountingPeriodEndDate = None,
          dateStarted = None
        )
        val journeySessionData: UIJourneySessionData =
          sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceData))

        sessionService.setMongoData(journeySessionData).flatMap(_ => Future.successful(Redirect(backUrl)))

      case None =>
        Logger("application").error("Unable to find addIncomeSourceData in session data")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
      case _ =>
        Logger("application").error("Unable to retrieve session data from Mongo")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
    }
  }

  private def updateAccountingPeriodForSE(incomeSourceStartDate: LocalDate, successUrl: String, isAgent: Boolean, sessionData: UIJourneySessionData)
                                         (implicit request: Request[_]): Future[Result] = {

    sessionData.addIncomeSourceData match {
      case Some(addIncomeSourceData) =>
        val accountingPeriodEndDate = dateService.getAccountingPeriodEndDate(incomeSourceStartDate)
        val updatedAddIncomeSourceData = addIncomeSourceData.copy(
          accountingPeriodStartDate = Some(incomeSourceStartDate),
          accountingPeriodEndDate = Some(accountingPeriodEndDate)
        )
        val journeySessionData: UIJourneySessionData =
          sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceData))

        sessionService.setMongoData(journeySessionData).flatMap(_ => Future.successful(Redirect(successUrl)))

      case None =>
        Logger("application").error("Unable to find addIncomeSourceData in session data")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
      case _ =>
        Logger("application").error("Unable to retrieve session data from Mongo")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
    }
  }


  private def getBackUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, mode: Mode): String = {
    routes.AddIncomeSourceStartDateController.show(isAgent, mode, incomeSourceType).url
  }

  private def getPostAction(incomeSourceType: IncomeSourceType, isAgent: Boolean, mode: Mode): Call = {
    routes.AddIncomeSourceStartDateCheckController.submit(isAgent, mode, incomeSourceType)
  }
  
  private def getSuccessUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, mode: Mode)
                           (implicit user: MtdItUser[_]): String = {

    ((isEnabled(AccountingMethodJourney), isAgent, mode, incomeSourceType) match {
      case (_, true, NormalMode, SelfEmployment) => routes.AddBusinessTradeController.showAgent(mode)
      case (_, _, NormalMode, SelfEmployment) => routes.AddBusinessTradeController.show(mode)
      case (true, _, NormalMode, _) => routes.IncomeSourcesAccountingMethodController.show(incomeSourceType, isAgent)
      case (_, false, _, _) => routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
      case (_, _, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    }).url
  }
}
