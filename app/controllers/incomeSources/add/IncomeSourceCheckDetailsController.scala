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

import audit.AuditingService
import audit.models.CreateIncomeSourceAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import exceptions.MissingSessionKey
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.AddIncomeSourceData.{dateStartedField, incomeSourcesAccountingMethodField}
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, UIJourneySessionData}
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.IncomeSourceCheckDetails

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCheckDetailsController @Inject()(val checkDetailsView: IncomeSourceCheckDetails,
                                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                                   val authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                   val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val businessDetailsService: CreateBusinessDetailsService,
                                                   val auditingService: AuditingService)
                                                  (implicit val ec: ExecutionContext,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val appConfig: FrontendAppConfig,
                                                   implicit val sessionService: SessionService,
                                                   implicit val itvcErrorHandler: ItvcErrorHandler,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with IncomeSourcesUtils with FeatureSwitching with JourneyChecker {

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        incomeSourceType
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              incomeSourceType
            )
        }
  }

  private def handleRequest(sources: IncomeSourceDetailsModel,
                            isAgent: Boolean,
                            incomeSourceType: IncomeSourceType)
                           (implicit user: MtdItUser[_]): Future[Result] = withCustomSession(JourneyType(Add, incomeSourceType)) {
    case None => Future.failed(new Error("Unable to find session"))
    case Some(sessionData) =>
      val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(incomeSourceType).url
      else controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(incomeSourceType).url
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submitAgent(incomeSourceType) else {
        controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submit(incomeSourceType)
      }
      getDetails(incomeSourceType, sessionData)(user).map {
        case Right(viewModel) =>
          Ok(checkDetailsView(
            viewModel,
            postAction = postAction,
            isAgent,
            backUrl = backUrl
          ))
        case Left(ex) =>
          Logger("application").error(
            s"[IncomeSourceCheckDetailsController][handleRequest] - Error: ${ex.getMessage}")
          errorHandler.showInternalServerError()
      } recover {
        case ex: Exception =>
          Logger("application").error(
            s"[IncomeSourceCheckDetailsController][handleRequest] - Error: Unable to construct getCheckPropertyViewModel ${ex.getMessage}")
          errorHandler.showInternalServerError()
      }
  }

  private def getDetails(incomeSourceType: IncomeSourceType, sessionData: UIJourneySessionData)
                        (implicit user: MtdItUser[_]): Future[Either[Throwable, CheckDetailsViewModel]] = {
    {
      if (incomeSourceType == SelfEmployment) getBusinessModel(sessionData) else getPropertyModel(incomeSourceType, sessionData)
    } map {
      case Right(checkDetailsViewModel: CheckDetailsViewModel) =>
        Right(checkDetailsViewModel)
      case Left(ex) =>
        Left(new IllegalArgumentException(s"Missing required session data: ${ex.getMessage}"))
    }
  }

  private def getPropertyModel(incomeSourceType: IncomeSourceType, sessionData: UIJourneySessionData)
                              (implicit user: MtdItUser[_]): Future[Either[Throwable, CheckPropertyViewModel]] = {
    sessionData.addIncomeSourceData match {
      case Some(addIncomeSourceData) =>
        (addIncomeSourceData.dateStarted, addIncomeSourceData.incomeSourcesAccountingMethod) match {
          case (Some(date), Some(method)) =>
            Future.successful(Right(CheckPropertyViewModel(
              tradingStartDate = date,
              cashOrAccrualsFlag = method,
              incomeSourceType = incomeSourceType
            )))
          case (_, _) =>
            Future(Left(new Error(s"Start date or accounting method not found in session. Start date: ${addIncomeSourceData.dateStarted}, AccMethod: ${addIncomeSourceData.incomeSourcesAccountingMethod}")))
        }
      case None => Future(Left(new Error("No data found in session")))
    }
  }

  private def getBusinessModel(sessionData: UIJourneySessionData)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, CheckBusinessDetailsViewModel]] = {
    val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)
    val showAccountingMethodPage: Boolean = userActiveBusinesses.isEmpty
    val errorTracePrefix = "[IncomeSourceCheckDetailsController][getBusinessModel]:"
    sessionData.addIncomeSourceData match {
      case Some(addIncomeSourceData) =>
        val address = addIncomeSourceData.address.getOrElse(throw MissingSessionKey(s"$errorTracePrefix address"))
        Future.successful(Right(CheckBusinessDetailsViewModel(
          businessName = addIncomeSourceData.businessName,
          businessStartDate = addIncomeSourceData.dateStarted,
          accountingPeriodEndDate = addIncomeSourceData.accountingPeriodEndDate
            .getOrElse(throw MissingSessionKey(s"$errorTracePrefix accountingPeriodEndDate")),
          businessTrade = addIncomeSourceData.businessTrade
            .getOrElse(throw MissingSessionKey(s"$errorTracePrefix businessTrade")),
          businessAddressLine1 = address.lines.headOption
            .getOrElse(throw MissingSessionKey(s"$errorTracePrefix businessAddressLine1")),
          businessAddressLine2 = address.lines.lift(1),
          businessAddressLine3 = address.lines.lift(2),
          businessAddressLine4 = address.lines.lift(3),
          businessPostalCode = address.postcode,
          businessCountryCode = addIncomeSourceData.countryCode,
          incomeSourcesAccountingMethod = addIncomeSourceData.incomeSourcesAccountingMethod,
          cashOrAccrualsFlag = addIncomeSourceData.incomeSourcesAccountingMethod
            .getOrElse(throw MissingSessionKey(s"$errorTracePrefix incomeSourcesAccountingMethod")),
          showedAccountingMethod = showAccountingMethodPage
        )))

      case None => throw new Exception(s"$errorTracePrefix failed to retrieve addIncomeSourceData")
    }

  }

  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmit(isAgent = false, incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmit(isAgent = true, incomeSourceType)
        }
  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = withCustomSession(JourneyType(Add, incomeSourceType)) {
    case None => Future.failed(new Error("Unable to find session"))
    case Some(sessionData) =>

      val redirectUrl: (Boolean, IncomeSourceType, String) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
        routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType, id).url

      val errorRedirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
        if (isAgent) routes.IncomeSourceNotAddedController.showAgent(incomeSourceType).url
        else routes.IncomeSourceNotAddedController.show(incomeSourceType).url

      {
        incomeSourceType match {
          case SelfEmployment => getBusinessModel(sessionData)
          case _ => getPropertyModel(incomeSourceType, sessionData)
        }
      }.flatMap {
        case Right(viewModel) =>
          businessDetailsService.createRequest(viewModel).flatMap {
            case Right(CreateIncomeSourceResponse(id)) =>
              auditingService.extendedAudit(CreateIncomeSourceAuditModel(incomeSourceType, viewModel, None, None, Some(CreateIncomeSourceResponse(id))))
              Future.successful {
                Redirect(redirectUrl(isAgent, incomeSourceType, id))
              }
            case Left(ex) =>
              auditingService.extendedAudit(
                CreateIncomeSourceAuditModel(incomeSourceType, viewModel, Some(enums.FailureCategory.ApiFailure), Some(ex.getMessage), None)
              )
              Future.failed(ex)
          }
        case Left(ex) =>
          Logger("application").error(
            s"[IncomeSourceCheckDetailsController][handleSubmit] - Error: ${ex.getMessage}")
          Future.successful {
            Redirect(errorRedirectUrl(isAgent, incomeSourceType))
          }
      }
  }.recover {
    case ex: Exception =>
      val errorRedirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
        if (isAgent) routes.IncomeSourceNotAddedController.showAgent(incomeSourceType).url
        else routes.IncomeSourceNotAddedController.show(incomeSourceType).url
      Logger("application").error(s"[IncomeSourceCheckDetailsController][handleSubmit]: ${ex.getMessage}")
      Redirect(errorRedirectUrl(isAgent, incomeSourceType))
  }
}
