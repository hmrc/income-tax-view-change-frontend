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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{BeforeSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.AddIncomeSourceStartDateCheckForm.{responseNo, responseYes}
import forms.incomeSources.add.{AddIncomeSourceStartDateCheckForm => form}
import implicits.ImplicitDateFormatter
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.AddIncomeSourceStartDateCheck

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddIncomeSourceStartDateCheckController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                                        val addIncomeSourceStartDateCheckView: AddIncomeSourceStartDateCheck,
                                                        val languageUtils: LanguageUtils,
                                                        val sessionService: SessionService,
                                                        auth: AuthenticatorPredicate)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        implicit val dateService: DateService,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitDateFormatter with IncomeSourcesUtils with JourneyChecker {

  def show(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleShowRequest(incomeSourceType, isAgent, isChange)
  }

  def submit(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleSubmitRequest(incomeSourceType, isAgent, isChange)
  }

  private def handleShowRequest(incomeSourceType: IncomeSourceType,
                                isAgent: Boolean,
                                isChange: Boolean)
                               (implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(JourneyType(Add, incomeSourceType), BeforeSubmissionPage) {
      _.addIncomeSourceData
        .flatMap(_.dateStarted) match {
          case Some(startDate: LocalDate) =>
            Future.successful {
              Ok(
                addIncomeSourceStartDateCheckView(
                  isAgent = isAgent,
                  incomeSourceStartDate = longDate(startDate).toLongDate,
                  backUrl = backUrl(incomeSourceType, isAgent, isChange),
                  postAction = postAction(incomeSourceType, isAgent, isChange),
                  form = form(incomeSourceType.addStartDateCheckMessagesPrefix)
                )
              )
            }
          case None =>
            Logger("application").error("[AddIncomeSourceStartDateCheckController][handleRequest]: " +
              "Failed to get income source start date from session")
            Future.successful(showInternalServerError(isAgent))
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleShowRequest][${incomeSourceType.key}] ${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  private def handleSubmitRequest(incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  isChange: Boolean)
                                 (implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    withSessionData(JourneyType(Add, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      sessionData.addIncomeSourceData.flatMap(_.dateStarted) match {
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
                    backUrl = backUrl(incomeSourceType, isAgent, isChange),
                    postAction = postAction(incomeSourceType, isAgent, isChange)
                  )
                )
              },
            formData =>
              handleValidForm(
                isAgent = isAgent,
                isChange = isChange,
                validForm = formData,
                sessionData = sessionData,
                incomeSourceStartDate = startDate,
                incomeSourceType = incomeSourceType
              )
          )
        case None =>
          Logger("application").error("[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
            "Failed to get income source start date from session")
          Future.successful(showInternalServerError(isAgent))
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest][${incomeSourceType.key}] ${ex.getMessage} - ${ex.getCause}")
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
    val successUrl = redirectUrl(incomeSourceType, isAgent, isChange)

    (formResponse, incomeSourceType) match {
      case (Some(`responseYes`), SelfEmployment) => updateAccountingPeriodForSE(incomeSourceStartDate, successUrl, isAgent, sessionData)
      case (Some(`responseYes`),              _) => Future.successful(Redirect(successUrl))
      case (Some(`responseNo`),               _) => removeDateFromSessionAndGoBack(incomeSourceType, isAgent, isChange, sessionData)
      case _ =>
        Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleValidForm] - Unexpected response, isAgent = $isAgent")
        Future.successful(showInternalServerError(isAgent))
    }
  }

  private def removeDateFromSessionAndGoBack(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean, sessionData: UIJourneySessionData)
                                            (implicit request: Request[_]): Future[Result] = {

    sessionData.addIncomeSourceData match {
      case Some(addIncomeSourceData) =>
        sessionService.setMongoData(
          sessionData.copy(
            addIncomeSourceData = Some(addIncomeSourceData.sanitiseDates)
          )
        ) flatMap ( _ =>
          Future.successful(
            Redirect(
              backUrl(incomeSourceType, isAgent, isChange)
            )
          )
        )
      case None =>
        Logger("application").error("Could not find addIncomeSourceData in session data")
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
    }
  }

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else         itvcErrorHandler

  private lazy val backUrl: (IncomeSourceType, Boolean, Boolean) => String = (incomeSourceType, isAgent, isChange) =>
    routes.AddIncomeSourceStartDateController.show(isAgent, isChange, incomeSourceType).url

  private lazy val postAction: (IncomeSourceType, Boolean, Boolean) => Call = (incomeSourceType, isAgent, isChange) =>
    routes.AddIncomeSourceStartDateCheckController.submit(isAgent, isChange, incomeSourceType)

  private lazy val redirectUrl: (IncomeSourceType, Boolean, Boolean) => String = (incomeSourceType, isAgent, isChange) =>
    ((isChange, incomeSourceType) match {
      case (false, SelfEmployment) => routes.AddBusinessTradeController               .show(isAgent, isChange)
      case (false, _)              => routes.IncomeSourcesAccountingMethodController  .show(isAgent, isChange, incomeSourceType)
      case (_,     _)              => routes.IncomeSourceCheckDetailsController       .show(isAgent, incomeSourceType)
    }).url
}
