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
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.{BeforeSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.incomeSources.add.{AddIncomeSourceStartDateCheckForm => form}
import implicits.ImplicitDateFormatter
import models.admin.AccountingMethodJourney
import models.incomeSourceDetails.UIJourneySessionData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import utils.JourneyChecker
import views.html.incomeSources.add.AddIncomeSourceStartDateCheck

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
  extends FrontendController(mcc) with I18nSupport with ImplicitDateFormatter with JourneyChecker {

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  def show(isAgent: Boolean,
           isChange: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>

    handleShowRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      isChange = isChange
    )
  }

  def submit(isAgent: Boolean,
             isChange: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>

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

    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
      val dateStartedOpt = sessionData.addIncomeSourceData.flatMap(_.dateStarted)
      dateStartedOpt match {
        case Some(startDate: LocalDate) =>
          Future.successful {
            Ok(
              addIncomeSourceStartDateCheckView(
                isAgent = isAgent,
                backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
                form = form(incomeSourceType.addStartDateCheckMessagesPrefix),
                postAction = getPostAction(incomeSourceType, isAgent, isChange),
                incomeSourceStartDate = longDate(startDate).toLongDate
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
                                  isChange: Boolean)
                                 (implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Add, incomeSourceType), BeforeSubmissionPage) { sessionData =>
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
                    backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
                    postAction = getPostAction(incomeSourceType, isAgent, isChange)
                  )
                )
              },
            formData =>
              handleValidForm(
                isAgent = isAgent,
                isChange = isChange,
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
                              isChange: Boolean,
                              incomeSourceStartDate: LocalDate,
                              incomeSourceType: IncomeSourceType,
                              sessionData: UIJourneySessionData)
                             (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption
    val successUrl = getSuccessUrl(incomeSourceType, isAgent, isChange)

    (formResponse, incomeSourceType) match {
      case (Some(form.responseNo), _) => removeDateFromSessionAndGoBack(incomeSourceType, isAgent, isChange, sessionData)
      case (Some(form.responseYes), SelfEmployment) => updateAccountingPeriodForSE(incomeSourceStartDate, successUrl, isAgent, sessionData)
      case (Some(form.responseYes), _) => Future.successful(Redirect(successUrl))
      case _ =>
        Logger("application").error(s"Unexpected response, isAgent = $isAgent")
        Future.successful(errorHandler(isAgent).showInternalServerError())
    }
  }

  private def removeDateFromSessionAndGoBack(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean, sessionData: UIJourneySessionData)
                                            (implicit request: Request[_]): Future[Result] = {

    val backUrl = getBackUrl(incomeSourceType, isAgent, isChange)

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


  private def getBackUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): String = {
    routes.AddIncomeSourceStartDateController.show(isAgent, isChange, incomeSourceType).url
  }

  private def getPostAction(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Call = {
    routes.AddIncomeSourceStartDateCheckController.submit(isAgent, isChange, incomeSourceType)
  }

  private def getSuccessUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean)
                           (implicit user: MtdItUser[_]): String = {

    ((isEnabled(AccountingMethodJourney), isAgent, isChange, incomeSourceType) match {
      case (_, true, false, SelfEmployment) => routes.AddBusinessTradeController.showAgent(isChange)
      case (_, _, false, SelfEmployment) => routes.AddBusinessTradeController.show(isChange)
      case (true, _, false, _) => routes.IncomeSourcesAccountingMethodController.show(incomeSourceType, isAgent)
      case (_, false, _, _) => routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
      case (_, _, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    }).url
  }
}
