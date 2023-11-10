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
import models.incomeSourceDetails.viewmodels.CheckDetailsViewModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.IncomeSourceCheckDetails
import audit.models.CreateIncomeSourceAuditModel

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCheckDetailsController @Inject()(val checkDetailsView: IncomeSourceCheckDetails,
                                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                                   val authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   val retrieveNino: NinoPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
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
  with IncomeSourcesUtils with FeatureSwitching {

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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

  private def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, incomeSourceType: IncomeSourceType)
                           (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(incomeSourceType).url
    else controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(incomeSourceType).url
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submitAgent(incomeSourceType) else {
      controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submit(incomeSourceType)
    }
    getDetails(incomeSourceType)(user).map {
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

  private def getDetails(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Either[Throwable, CheckDetailsViewModel]] = {
    {
      if (incomeSourceType == SelfEmployment) getBusinessModel else getPropertyModel(incomeSourceType)
    } map {
      case Right(checkDetailsViewModel: CheckDetailsViewModel) =>
        Right(checkDetailsViewModel)
      case Left(ex) =>
        Left(new IllegalArgumentException(s"Missing required session data: ${ex.getMessage}"))
    }
  }

  private def getPropertyModel(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Either[Throwable, CheckDetailsViewModel]] = {
    sessionService.getMongoKeyTyped[LocalDate](dateStartedField, JourneyType(Add, incomeSourceType)).flatMap { startDate: Either[Throwable, Option[LocalDate]] =>
      sessionService.getMongoKeyTyped[String](incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType)).map { accMethod: Either[Throwable, Option[String]] =>
        (startDate, accMethod) match {
          case (Right(dateMaybe), Right(methodMaybe)) =>
            (dateMaybe, methodMaybe) match {
              case (Some(date), Some(method)) =>
                Right(CheckDetailsViewModel(
                  businessStartDate = Some(date),
                  cashOrAccrualsFlag = method,
                  incomeSourceType = incomeSourceType
                ))
              case (_, _) =>
                Left(new Error(s"Start date or accounting method not found in session. Start date: $dateMaybe, AccMethod: $methodMaybe"))
            }
          case (_, _) => Left(new Error(s"Error while retrieving date started or accounting method from session"))
        }
      }
    }
  }

  private def getBusinessModel(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, CheckDetailsViewModel]] = {
    val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)
    val showAccountingMethodPage: Boolean = userActiveBusinesses.isEmpty
    val errorTracePrefix = "[IncomeSourceCheckDetailsController][getBusinessModel]:"
    sessionService.getMongo(JourneyType(Add, SelfEmployment).toString).map {
      case Right(Some(uiJourneySessionData)) =>
        uiJourneySessionData.addIncomeSourceData match {
          case Some(addIncomeSourceData) =>

            val address = addIncomeSourceData.address.getOrElse(throw MissingSessionKey(s"$errorTracePrefix address"))
            Right(CheckDetailsViewModel(
              businessName = addIncomeSourceData.businessName,
              businessStartDate = addIncomeSourceData.dateStarted,
              accountingPeriodEndDate = Some(addIncomeSourceData.accountingPeriodEndDate
                .getOrElse(throw MissingSessionKey(s"$errorTracePrefix accountingPeriodEndDate"))),
              businessTrade = Some(addIncomeSourceData.businessTrade
                .getOrElse(throw MissingSessionKey(s"$errorTracePrefix businessTrade"))),
              businessAddressLine1 = Some(address.lines.headOption
                .getOrElse(throw MissingSessionKey(s"$errorTracePrefix businessAddressLine1"))),
              businessAddressLine2 = address.lines.lift(1),
              businessAddressLine3 = address.lines.lift(2),
              businessAddressLine4 = address.lines.lift(3),
              businessPostalCode = address.postcode,
              businessCountryCode = addIncomeSourceData.countryCode,
              incomeSourcesAccountingMethod = addIncomeSourceData.incomeSourcesAccountingMethod,
              cashOrAccrualsFlag = addIncomeSourceData.incomeSourcesAccountingMethod
                .getOrElse(throw MissingSessionKey(s"$errorTracePrefix incomeSourcesAccountingMethod")),
              showedAccountingMethod = showAccountingMethodPage,
              incomeSourceType = SelfEmployment
            ))

          case None => throw new Exception(s"$errorTracePrefix failed to retrieve addIncomeSourceData")
        }
      case _ => throw new Exception(s"$errorTracePrefix failed to retrieve uiJourneySessionData ")
    }

  }

  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val (redirect, errorRedirect) = (isAgent, incomeSourceType) match {
      case (true, SelfEmployment) => (routes.BusinessReportingMethodController.showAgent _, routes.IncomeSourceNotAddedController.showAgent(SelfEmployment).url)
      case (false, SelfEmployment) => (routes.BusinessReportingMethodController.show _, routes.IncomeSourceNotAddedController.show(SelfEmployment).url)
      case (true, UkProperty) => (routes.UKPropertyReportingMethodController.showAgent _, routes.IncomeSourceNotAddedController.showAgent(UkProperty).url)
      case (false, UkProperty) => (routes.UKPropertyReportingMethodController.show _, routes.IncomeSourceNotAddedController.show(UkProperty).url)
      case (true, ForeignProperty) => (routes.ForeignPropertyReportingMethodController.showAgent _, routes.IncomeSourceNotAddedController.showAgent(ForeignProperty).url)
      case (false, ForeignProperty) => (routes.ForeignPropertyReportingMethodController.show _, routes.IncomeSourceNotAddedController.show(ForeignProperty).url)
    }
    {
      incomeSourceType match {
        case SelfEmployment => getBusinessModel
        case _ => getPropertyModel(incomeSourceType)
      }
    }.flatMap {
      case Right(viewModel) =>
        businessDetailsService.createRequest(viewModel).flatMap {
          case Right(CreateIncomeSourceResponse(id)) =>
            auditingService.extendedAudit(CreateIncomeSourceAuditModel(incomeSourceType, viewModel, None, None, Some(CreateIncomeSourceResponse(id))))
            sessionService.deleteMongoData(JourneyType(Add, incomeSourceType)).flatMap { _ =>
              Future.successful(Redirect(redirect(id).url))
            }
          case Left(ex) =>
            auditingService.extendedAudit(CreateIncomeSourceAuditModel(incomeSourceType, viewModel, Some(enums.FailureCategory.ApiFailure), Some(ex.getMessage), None))
            Future.failed(ex)
        }
      case Left(ex) =>
        Logger("application").error(
          s"[IncomeSourceCheckDetailsController][handleSubmit] - Error: ${ex.getMessage}")
        Future.successful(Redirect(errorRedirect))
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"[IncomeSourceCheckDetailsController][handleSubmit]${ex.getMessage}")
        Redirect(errorRedirect)
    }
  }

}
