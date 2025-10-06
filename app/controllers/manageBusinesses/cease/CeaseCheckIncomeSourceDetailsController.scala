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

package controllers.manageBusinesses.cease

import audit.AuditingService
import audit.models.CeaseIncomeSourceAuditModel
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.BeforeSubmissionPage
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import models.core.IncomeSourceId
import models.incomeSourceDetails.viewmodels.CheckCeaseIncomeSourceDetailsViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.cease.CeaseCheckIncomeSourceDetails

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseCheckIncomeSourceDetailsController @Inject()(
                                                         val authActions: AuthActions,
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
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  private lazy val backUrl: Boolean => String = (isAgent: Boolean) => if (isAgent) routes.CeaseIncomeSourceController.showAgent().url
  else routes.CeaseIncomeSourceController.show().url

  private def getRedirectCall(isAgent: Boolean, incomeSourceType: IncomeSourceType): Call = {
    if (isAgent) routes.IncomeSourceCeasedObligationsController.showAgent(incomeSourceType)
    else routes.IncomeSourceCeasedObligationsController.show(incomeSourceType)
  }

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] =
    withSessionData(IncomeSourceJourneyType(Cease, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val messagesPrefix = incomeSourceType.ceaseCheckAnswersPrefix
      val incomeSourceIdOpt = sessionData.ceaseIncomeSourceData.flatMap(_.incomeSourceId)
      val endDateOpt = sessionData.ceaseIncomeSourceData.flatMap(_.endDate)

      getViewModel(incomeSourceType, endDateOpt, incomeSourceIdOpt) match {
        case Some(viewModel) =>
          Future.successful {
            Ok(view(
              viewModel = viewModel,
              isAgent = isAgent,
              backUrl = backUrl(isAgent),
              messagesPrefix = messagesPrefix))
          }
        case None =>
          Future.successful {
            Redirect(controllers.manageBusinesses.cease.routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType))
          }
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent] "}" +
          s"Error getting CeaseCheckIncomeSourceDetails page: ${ex.getMessage} - ${ex.getCause}")
        Redirect(controllers.manageBusinesses.cease.routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType))
    }

  def getViewModel(incomeSourceType: IncomeSourceType, endDateOpt: Option[LocalDate], incomeSourceIdOpt: Option[String])
                  (implicit user: MtdItUser[_]): Option[CheckCeaseIncomeSourceDetailsViewModel] = {
    (incomeSourceIdOpt, endDateOpt, incomeSourceType) match {
      case (Some(id), Some(endDate), SelfEmployment) =>
        incomeSourceDetailsService.getCheckCeaseSelfEmploymentDetailsViewModel(user.incomeSources, IncomeSourceId(id), endDate) match {
          case Right(viewModel) => Some(viewModel)
          case Left(ex) =>
            Logger("application").error(s"Unable to get view model for SelfEmployment: ${ex.getMessage} - ${ex.getCause}")
            None
        }
      case (None, Some(endDate), _) =>
        incomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(user.incomeSources, endDate, incomeSourceType) match {
          case Right(viewModel) => Some(viewModel)
          case Left(ex) =>
            Logger("application").error(s"Unable to get view model for $incomeSourceType: ${ex.getMessage} - ${ex.getCause}")
            None
        }
      case (_, _, _) =>
        Logger("application").error(s"Unable to get required data from session for $incomeSourceType")
        None
    }
  }


  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleRequest(
        isAgent = true,
        incomeSourceType = incomeSourceType
      )
  }

  def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                         (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Cease, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val incomeSourceIdOpt = sessionData.ceaseIncomeSourceData.flatMap(_.incomeSourceId)
      val endDateOpt = sessionData.ceaseIncomeSourceData.flatMap(_.endDate)

      incomeSourceType match {
        case SelfEmployment => ceaseSelfEmployment(incomeSourceIdOpt, endDateOpt, isAgent)(implicitly, errorHandler)
        case _ => ceaseProperty(endDateOpt, incomeSourceType, isAgent)(implicitly, errorHandler)
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"Error Submitting Cease Date: ${ex.getMessage}", ex.getCause)
        errorHandler.showInternalServerError()
    }
  }

  private def ceaseSelfEmployment(incomeSourceIdOpt: Option[String], endDateOpt: Option[LocalDate], isAgent: Boolean)
                                 (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    (incomeSourceIdOpt, endDateOpt) match {
      case (Some(incomeSourceId), Some(endDate)) => updateCessationDate(endDate, SelfEmployment, IncomeSourceId(incomeSourceId), isAgent)
      case _ => Future.successful {
        Logger("application").error(s"Missing income source id or end date")
        errorHandler.showInternalServerError()
      }
    }
  }

  private def ceaseProperty(endDateOpt: Option[LocalDate], incomeSourceType: IncomeSourceType, isAgent: Boolean)
                           (implicit user: MtdItUser[_],  errorHandler: ShowInternalServerError): Future[Result] = {
    endDateOpt match {
      case Some(endDate) =>
        getActiveProperty(incomeSourceType) match {
          case Some(property) =>
            val incomeSourceId = IncomeSourceId(property.incomeSourceId)
            updateCessationDate(endDate, incomeSourceType, incomeSourceId, isAgent)
          case None =>
            Logger("application").error(s"Unable to retrieve property income source.")
            Future.successful {
              errorHandler.showInternalServerError()
            }
        }
      case None =>
        Logger("application").error(s"Missing end date")
        Future.successful {
          errorHandler.showInternalServerError()
        }
    }
  }


  def updateCessationDate(cessationDate: LocalDate, incomeSourceType: IncomeSourceType, incomeSourceId: IncomeSourceId, isAgent: Boolean)
                         (implicit user: MtdItUser[_]): Future[Result] = {
    val redirectCall = getRedirectCall(isAgent, incomeSourceType)

    updateIncomeSourceService.updateCessationDate(user.nino, incomeSourceId.value, cessationDate).flatMap {
      case Right(_) =>
        auditingService.extendedAudit(CeaseIncomeSourceAuditModel(
          incomeSourceType = incomeSourceType,
          cessationDate = cessationDate.toString,
          incomeSourceId = incomeSourceId,
          updateIncomeSourceErrorResponse = None))

        Future.successful(Redirect(redirectCall))

      case Left(error) =>
        Logger("application").error("" +
          " Unsuccessful update response received")

        auditingService.extendedAudit(CeaseIncomeSourceAuditModel(
          incomeSourceType = incomeSourceType,
          cessationDate = cessationDate.toString,
          incomeSourceId = incomeSourceId,
          updateIncomeSourceErrorResponse = Some(error)))

        Future.successful {
          Redirect(controllers.manageBusinesses.cease.routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType))
        }
    }
  }


  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit request =>
      handleSubmitRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType)(implicitly, itvcErrorHandler)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleSubmitRequest(
        isAgent = true,
        incomeSourceType = incomeSourceType)(implicitly, itvcErrorHandlerAgent)
  }
}
