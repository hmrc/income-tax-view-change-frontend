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

package controllers.incomeSources.cease

import audit.AuditingService
import audit.models.CeaseIncomeSourceAuditModel
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import models.core.IncomeSourceId
import IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{CeaseIncomeSourceData, IncomeSourceDetailsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService}
import utils.IncomeSourcesUtils
import views.html.incomeSources.cease.CeaseCheckIncomeSourceDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseCheckIncomeSourceDetailsController @Inject()(val authenticate: AuthenticationPredicate,
                                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                                        val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                        val retrieveBtaNavBar: NavBarPredicate,
                                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                        val view: CeaseCheckIncomeSourceDetails,
                                                        val updateIncomeSourceService: UpdateIncomeSourceService,
                                                        val sessionService: SessionService,
                                                        val auditingService: AuditingService)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  private def getSessionData(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[(Either[Throwable, Option[IncomeSourceId]], Either[Throwable, Option[String]])] = {
    val incomeSourceIdFuture: Future[Either[Throwable, Option[IncomeSourceId]]] = if (incomeSourceType == SelfEmployment) {
      sessionService
        .getMongoKeyTyped[String](CeaseIncomeSourceData.incomeSourceIdField, JourneyType(Cease, SelfEmployment))
        .collect {
          case Right(incomeSourceMaybeId) =>
            Right(incomeSourceMaybeId.map(id => mkIncomeSourceId(id)))
          case Left(ex) => Left(ex)
        }
    } else {
      Future(Right(None))
    }
    val cessationEndDateFuture = sessionService.getMongoKeyTyped[String](CeaseIncomeSourceData.dateCeasedField, JourneyType(Cease, incomeSourceType))
    for {
      incomeSourceId <- incomeSourceIdFuture
      cessationEndDate <- cessationEndDateFuture
    } yield (incomeSourceId, cessationEndDate)
  }

  private def getRedirectCall(isAgent: Boolean, incomeSourceType: IncomeSourceType): Call = {
    (isAgent, incomeSourceType) match {
      case (true, SelfEmployment) => routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment)
      case (false, SelfEmployment) => routes.IncomeSourceCeasedObligationsController.show(SelfEmployment)
      case (true, UkProperty) => routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty)
      case (false, UkProperty) => routes.IncomeSourceCeasedObligationsController.show(UkProperty)
      case (true, ForeignProperty) => routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty)
      case (false, ForeignProperty) => routes.IncomeSourceCeasedObligationsController.show(ForeignProperty)
      case _ => routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType)
    }
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {

    val messagesPrefix = incomeSourceType.ceaseCheckDetailsPrefix
    val sessionDataFuture = getSessionData(incomeSourceType)

    sessionDataFuture.flatMap {
      case (Right(Some(incomeSourceId)), Right(Some(cessationEndDate))) =>
        incomeSourceDetailsService.getCheckCeaseSelfEmploymentDetailsViewModel(sources, incomeSourceId, cessationEndDate) match {
          case Right(viewModel) =>
            Future.successful(Ok(view(
              viewModel = viewModel,
              isAgent = isAgent,
              backUrl = routes.CeaseIncomeSourceController.show().url,
              messagesPrefix = messagesPrefix)))
          case Left(ex) => Future.failed(ex)
        }
      case (Right(None), Right(Some(cessationEndDate))) =>
        incomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(sources, cessationEndDate, incomeSourceType) match {
          case Right(viewModel) =>
            Future.successful(Ok(view(
              viewModel = viewModel,
              isAgent = isAgent,
              backUrl = routes.CeaseIncomeSourceController.show().url,
              messagesPrefix = messagesPrefix)))
          case Left(ex) =>
            Future.failed(ex)
        }
      case (_, _) =>
        val errorMessage = s"Unable to get required data from session for $incomeSourceType"
        Future.failed(new Exception(errorMessage))
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[CeaseCheckIncomeSourceDetailsController][handleRequest]${if (isAgent) "[Agent] "}" +
        s"Error getting CeaseCheckIncomeSourceDetails page: ${ex.getMessage}")
      Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, SelfEmployment))
  }


  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          sources = user.incomeSources,
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
    }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }

  def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val sessionDataFuture = getSessionData(incomeSourceType)

    sessionDataFuture.flatMap {
      case (Right(Some(incomeSourceId)), Right(Some(cessationEndDate))) =>
        // Update for SE Business
        updateCessationDate(cessationEndDate, incomeSourceType, incomeSourceId, isAgent)
      case (Right(None), Right(Some(cessationEndDate))) =>
        // Update for Property
        val propertyIncomeSources = if (incomeSourceType.equals(UkProperty)) {
          user.incomeSources.properties.find(propertyDetailsModel => propertyDetailsModel.isUkProperty && !propertyDetailsModel.isCeased)
        }
        else {
          user.incomeSources.properties.find(propertyDetailsModel => propertyDetailsModel.isForeignProperty && !propertyDetailsModel.isCeased)
        }

        val incomeSourceId = mkIncomeSourceId(propertyIncomeSources.head.incomeSourceId)
        updateCessationDate(cessationEndDate, incomeSourceType, incomeSourceId, isAgent)

      case (_, _) =>
        val errorMessage = s"Unable to get required data from session for $incomeSourceType"
        Future.failed(new Exception(errorMessage))
    }
  } recover {
    case ex: Exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[CheckCeaseBusinessDetailsController][handleSubmitRequest] Error Submitting Cease Date: ${
        ex.getMessage
      }")
      errorHandler.showInternalServerError()
  }

  def updateCessationDate(cessationDate: String, incomeSourceType: IncomeSourceType, incomeSourceId: IncomeSourceId, isAgent: Boolean)
                         (implicit user: MtdItUser[_]): Future[Result] = {
    val redirectCall = getRedirectCall(isAgent, incomeSourceType)

    updateIncomeSourceService.updateCessationDate(user.nino, incomeSourceId.value, cessationDate).flatMap {
      case Right(_) =>
        auditingService.extendedAudit(CeaseIncomeSourceAuditModel(
          incomeSourceType = incomeSourceType,
          cessationDate = cessationDate,
          incomeSourceId = incomeSourceId,
          updateIncomeSourceErrorResponse = None))

        Future.successful(Redirect(redirectCall))

      case Left(error) =>
        Logger("application").error(s"[CheckCeaseBusinessDetailsController][handleSubmitRequest]:" +
          s" Unsuccessful update response received")

        auditingService.extendedAudit(CeaseIncomeSourceAuditModel(
          incomeSourceType = incomeSourceType,
          cessationDate = cessationDate,
          incomeSourceId = incomeSourceId,
          updateIncomeSourceErrorResponse = Some(error)))

        Future.successful {
          Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType))
        }
    }
  }


  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType)
        }
  }
}