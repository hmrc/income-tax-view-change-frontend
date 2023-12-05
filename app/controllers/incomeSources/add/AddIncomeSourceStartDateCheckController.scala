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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.{AddIncomeSourceStartDateCheckForm => form}
import implicits.ImplicitDateFormatter
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.{SensitiveAddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.AddIncomeSourceStartDateCheck

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddIncomeSourceStartDateCheckController @Inject()(authenticate: AuthenticationPredicate,
                                                        val authorisedFunctions: AuthorisedFunctions,
                                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                                        val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                        val retrieveBtaNavBar: NavBarPredicate,
                                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                        val addIncomeSourceStartDateCheckView: AddIncomeSourceStartDateCheck,
                                                        val languageUtils: LanguageUtils,
                                                        val sessionService: SessionService)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        implicit val dateService: DateService,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitDateFormatter with IncomeSourcesUtils {

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

    withIncomeSourcesFS {
      getStartDate(incomeSourceType).flatMap {
        case Some(startDate) =>
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
          Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleRequest]: " +
            s"Failed to get income source start date from session")
          Future.successful(showInternalServerError(isAgent))
      }
    }
  }.recover {
    case exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleShowRequest][${incomeSourceType.key}] ${exception.getMessage}")
      errorHandler.showInternalServerError()
  }

  private def handleSubmitRequest(incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  isChange: Boolean)
                                 (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.addStartDateCheckMessagesPrefix
    withIncomeSourcesFS {
      getStartDate(incomeSourceType).flatMap {
        case Some(startDate) =>
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
                incomeSourceType = incomeSourceType
              )
          )
        case None =>
          Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest]: " +
            s"Failed to get income source start date from session")
          Future.successful(showInternalServerError(isAgent))
      }
    }
  }.recover {
    case exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleSubmitRequest][${incomeSourceType.key}] ${exception.getMessage}")
      errorHandler.showInternalServerError()
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
      (checkSessionTimeout andThen authenticate
        andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }

  private def handleValidForm(validForm: form,
                              isAgent: Boolean,
                              isChange: Boolean,
                              incomeSourceStartDate: LocalDate,
                              incomeSourceType: IncomeSourceType)
                             (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption
    val successUrl = getSuccessUrl(incomeSourceType, isAgent, isChange)

    (formResponse, incomeSourceType) match {
      case (Some(form.responseNo), _) => removeDateFromSessionAndGoBack(incomeSourceType, isAgent, isChange)
      case (Some(form.responseYes), SelfEmployment) => updateAccountingPeriodForSE(incomeSourceStartDate, successUrl)
      case (Some(form.responseYes), _) => Future.successful(Redirect(successUrl))
      case _ =>
        Logger("application").error(s"[AddIncomeSourceStartDateCheckController][handleValidForm] - Unexpected response, isAgent = $isAgent")
        Future.successful(showInternalServerError(isAgent))
    }
  }

  private def removeDateFromSessionAndGoBack(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean)
                                            (implicit request: Request[_]): Future[Result] = {
    val backUrl = getBackUrl(incomeSourceType, isAgent, isChange)
    val journeyType = JourneyType(Add, incomeSourceType)

    sessionService.getMongo(journeyType.toString).flatMap {
      case Right(Some(sessionData)) =>
        val oldAddIncomeSourceData = sessionData.addIncomeSourceData match {
          case Some(addIncomeSourceData) => addIncomeSourceData
          case None => throw new Exception("addIncomeSourceData field not found in session data")
        }

        val updatedAddIncomeSourceData = oldAddIncomeSourceData.copy(dateStarted = None, accountingPeriodStartDate = None, accountingPeriodEndDate = None)
        val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceData))

        sessionService.setMongoData(uiJourneySessionData).flatMap {
          case true => Future.successful(Redirect(backUrl))
          case false => Future.failed(new Exception("Unable to delete start date"))
        }
      case _ => Future.failed(new Exception(s"Unable to retrieve ${journeyType.toString} session data"))
    }
  }

  private def updateAccountingPeriodForSE(incomeSourceStartDate: LocalDate, successUrl: String)
                                         (implicit request: Request[_]): Future[Result] = {
    val journeyType = JourneyType(Add, SelfEmployment)

    sessionService.getMongo(journeyType.toString).flatMap {
      case Right(Some(sessionData)) =>
        val oldAddIncomeSourceData = sessionData.addIncomeSourceData match {
          case Some(addIncomeSourceData) => addIncomeSourceData.decrypted
          case None => throw new Exception("addIncomeSourceData field not found in session data")
        }
        val accountingPeriodEndDate = dateService.getAccountingPeriodEndDate(incomeSourceStartDate)
        val updatedAddIncomeSourceData = oldAddIncomeSourceData.copy(accountingPeriodStartDate = Some(incomeSourceStartDate.toString),
          accountingPeriodEndDate = Some(accountingPeriodEndDate.toString))
        val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceData.encrypted))

        sessionService.setMongoData(uiJourneySessionData).flatMap {
          case true => Future.successful(Redirect(successUrl))
          case false => Future.failed(new Exception("Unable to update accounting period"))
        }
      case _ => Future.failed(new Exception(s"Unable to retrieve ${journeyType.toString} session data"))
    }
  }

  private def getStartDate(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Option[LocalDate]] = {
    val journeyType = JourneyType(Add, incomeSourceType)
    sessionService.getMongo(journeyType.toString).flatMap {
      case Right(dateOpt) =>
        Future.successful(
          dateOpt.flatMap(
            _.addIncomeSourceData
              .flatMap(
                _.decrypted
                  .dateStarted
                  .map(LocalDate.parse)
              )
          )
        )
      case Left(ex) => Future.failed(ex)
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
      case (_, false, SelfEmployment) => routes.AddBusinessTradeController.show(isAgent, isChange)
      case (false, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
      case (false, false, _) => routes.IncomeSourcesAccountingMethodController.show(incomeSourceType)
      case (_, false, _) => routes.IncomeSourcesAccountingMethodController.showAgent(incomeSourceType)
      case (false, _, UkProperty) => routes.IncomeSourceCheckDetailsController.show(UkProperty)
      case (_, _, UkProperty) => routes.IncomeSourceCheckDetailsController.showAgent(UkProperty)
      case (false, _, _) => routes.IncomeSourceCheckDetailsController.show(ForeignProperty)
      case (_, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty)
    }).url
  }
}
