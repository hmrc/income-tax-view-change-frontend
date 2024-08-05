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
import cats.implicits.catsSyntaxOptionId
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{BeforeSubmissionPage, ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.{AddIncomeSourceStartDateCheckForm => form}
import implicits.ImplicitDateFormatter
import models.incomeSourceDetails.AddIncomeSourceData.{accountingPeriodEndDateLens, accountingPeriodLens, accountingPeriodStartDateLens, addIncomeSourceDataLens}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.add.AddIncomeSourceStartDateCheck
import routes._
import form._

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
  extends ClientConfirmedController with I18nSupport with FeatureSwitching
    with ImplicitDateFormatter with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  def show(isAgent: Boolean,
           isChange: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>

    handleShowRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      isChange = isChange
    )
  }

  def submit(isAgent: Boolean,
             isChange: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>

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

    withSessionData(JourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
      val dateStartedOpt = sessionData.addIncomeSourceData.flatMap(_.dateStarted)
      dateStartedOpt match {
        case Some(startDate: LocalDate) =>
          Future.successful {
            Ok(
              addIncomeSourceStartDateCheckView(
                isAgent = isAgent,
                backUrl = backUrl(incomeSourceType, isAgent, isChange),
                form = form(incomeSourceType.addStartDateCheckMessagesPrefix),
                postAction = postAction(incomeSourceType, isAgent, isChange),
                incomeSourceStartDate = longDate(startDate).toLongDate,
                incomeSourceType = incomeSourceType
              )
            )
          }
        case None =>
          Logger("application").error("" +
            "Failed to get income source start date from session")
          Future.successful(showInternalServerError(isAgent))
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
    withSessionData(JourneyType(Add, incomeSourceType), BeforeSubmissionPage) { sessionData =>
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
                    backUrl = backUrl(incomeSourceType, isAgent, isChange),
                    postAction = postAction(incomeSourceType, isAgent, isChange),
                    incomeSourceType = incomeSourceType
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
          Future.successful(showInternalServerError(isAgent))
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

    val formResponse: Option[String] = validForm.toFormMap(response).headOption

    (formResponse, incomeSourceType) match {
      case (Some(`responseNo`),               _) => removeDateFromSessionAndGoBack(incomeSourceType, isAgent, isChange, sessionData)
      case (Some(`responseYes`), SelfEmployment) => updateAccountingPeriodForSE(incomeSourceStartDate, incomeSourceType, isAgent, isChange, sessionData)
      case (Some(`responseYes`),              _) => Future.successful(Redirect(successUrl(incomeSourceType, isAgent, isChange)))
      case _ =>
        Logger("application").error(s"Unexpected response, isAgent = $isAgent")
        Future.successful(showInternalServerError(isAgent))
    }
  }

  private def removeDateFromSessionAndGoBack(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean, sessionData: UIJourneySessionData)
                                            (implicit request: Request[_]): Future[Result] = {

    sessionData.addIncomeSourceData match {
      case Some(data) =>
        sessionService.setMongoData(
          addIncomeSourceDataLens.replace(data.sanitiseDates.some)(sessionData)
        ) flatMap(
          _ => Future.successful(Redirect(backUrl(incomeSourceType, isAgent, isChange)))
        )
      case None =>
        Logger("application").error("Unable to find addIncomeSourceData in session data")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
    }
  }

  private def updateAccountingPeriodForSE(incomeSourceStartDate: LocalDate,
                                          incomeSourceType: IncomeSourceType,
                                          isAgent: Boolean,
                                          isChange: Boolean,
                                          sessionData: UIJourneySessionData)
                                         (implicit request: Request[_]): Future[Result] = {

    val accountingPeriodEndDate = dateService.getAccountingPeriodEndDate(incomeSourceStartDate)

    sessionData.addIncomeSourceData match {
      case Some(_) =>

        val updatedAddIncomeSourceData =
          accountingPeriodLens.replace(
            (incomeSourceStartDate.some, accountingPeriodEndDate.some)
          )(sessionData)

        sessionService.setMongoData(updatedAddIncomeSourceData)
          .flatMap(_ =>
            Future.successful(Redirect(successUrl(incomeSourceType, isAgent, isChange)))
          )
      case None =>
        Logger("application").error("Unable to find addIncomeSourceData in session data")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
    }
  }

  lazy val backUrl: (IncomeSourceType, Boolean, Boolean) => String = (incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean) =>
    AddIncomeSourceStartDateController.show(isAgent, isChange, incomeSourceType).url

  lazy val postAction: (IncomeSourceType, Boolean, Boolean) => Call = (incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean) =>
    AddIncomeSourceStartDateCheckController.submit(isAgent, isChange, incomeSourceType)

  lazy val successUrl: (IncomeSourceType, Boolean, Boolean) => String = (incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean) =>
    ((isAgent, isChange, incomeSourceType) match {
      case (_, false, SelfEmployment) => AddBusinessTradeController.show(isAgent, isChange)
      case (_, false,              _) => IncomeSourcesAccountingMethodController.show(incomeSourceType, isAgent)
      case (false, _,              _) => IncomeSourceCheckDetailsController.show(incomeSourceType)
      case (_,     _,              _) => IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    }).url
}
