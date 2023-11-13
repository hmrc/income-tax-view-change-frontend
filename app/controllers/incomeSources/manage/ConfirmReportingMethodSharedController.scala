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

package controllers.incomeSources.manage

import audit.AuditingService
import audit.models.IncomeSourceReportingMethodAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import exceptions.MissingSessionKey
import forms.incomeSources.manage.ConfirmReportingMethodForm
import models.incomeSourceDetails.TaxYear.getTaxYearModel
import models.incomeSourceDetails.{ManageIncomeSourceData, TaxYear}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceListResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.MarkerContext.NoMarker
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.{ConfirmReportingMethod, ManageIncomeSources}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmReportingMethodSharedController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                                       val checkSessionTimeout: SessionTimeoutPredicate,
                                                       val authenticate: AuthenticationPredicate,
                                                       val authorisedFunctions: AuthorisedFunctions,
                                                       val retrieveNino: NinoPredicate,
                                                       val updateIncomeSourceService: UpdateIncomeSourceService,
                                                       val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                       val confirmReportingMethod: ConfirmReportingMethod,
                                                       val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                       val retrieveBtaNavBar: NavBarPredicate,
                                                       val sessionService: SessionService,
                                                       val auditingService: AuditingService,
                                                       val dateService: DateService)
                                                      (implicit val ec: ExecutionContext,
                                                       implicit val itvcErrorHandler: ItvcErrorHandler,
                                                       override implicit val mcc: MessagesControllerComponents,
                                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def show(taxYear: String,
           changeTo: String,
           isAgent: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    withIncomeSourcesFS {
      if (incomeSourceType == SelfEmployment) {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, incomeSourceType)).flatMap {
          case Right(incomeSourceIdMayBe) => handleShowRequest(taxYear, changeTo, isAgent, incomeSourceType, incomeSourceIdMayBe)
          case Left(exception) => Future.failed(exception)
        }
      }
      else handleShowRequest(taxYear, changeTo, isAgent, incomeSourceType, None)
    }.recover {
      case exception =>
        Logger("application").error(s"[ConfirmReportingMethodSharedController][show] ${exception.getMessage}")
        showInternalServerError(isAgent)
    }
  }

  def submit(taxYear: String,
             changeTo: String,
             isAgent: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    withIncomeSourcesFS {
      if (incomeSourceType == SelfEmployment) {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, incomeSourceType)).flatMap {
          case Right(incomeSourceIdMayBe) => handleSubmitRequest(taxYear, changeTo, isAgent, incomeSourceIdMayBe, incomeSourceType)
          case Left(exception) => Future.failed(exception)
        }
      }
      else handleSubmitRequest(taxYear, changeTo, isAgent, None, incomeSourceType)
    }.recover {
      case exception =>
        Logger("application").error(s"[ConfirmReportingMethodSharedController][submit] ${exception.getMessage}")
        showInternalServerError(isAgent)
    }
  }

  private def handleShowRequest(taxYear: String,
                                changeTo: String,
                                isAgent: Boolean,
                                incomeSourceType: IncomeSourceType,
                                soleTraderBusinessId: Option[String])
                               (implicit user: MtdItUser[_]): Future[Result] = {

    val maybeIncomeSourceId: Option[String] = user.incomeSources.getIncomeSourceId(incomeSourceType, soleTraderBusinessId)

    withIncomeSourcesFS {
      Future.successful(
        (getTaxYearModel(taxYear), getReportingMethod(changeTo), maybeIncomeSourceId) match {
          case (Some(taxYearModel), Some(reportingMethod), Some(id)) =>

            val (backCall, _) = getRedirectCalls(taxYear, isAgent, changeTo, Some(id), incomeSourceType)

            Ok(
              confirmReportingMethod(
                isAgent = isAgent,
                backUrl = backCall.url,
                newReportingMethod = reportingMethod,
                form = ConfirmReportingMethodForm(changeTo),
                taxYearEndYear = taxYearModel.endYear.toString,
                taxYearStartYear = taxYearModel.startYear.toString,
                postAction = getPostAction(taxYear, changeTo, isAgent, incomeSourceType),
                isCurrentTaxYear = dateService.getCurrentTaxYearEnd().equals(taxYearModel.endYear)
              )
            )
          case (None, _, _) => logAndShowError(isAgent, s"[handleShowRequest]: Could not parse taxYear: $taxYear")
          case (_, None, _) => logAndShowError(isAgent, s"[handleShowRequest]: Could not parse reporting method: $changeTo")
          case (_, _, None) => logAndShowError(isAgent, s"[handleShowRequest]: Could not find incomeSourceId for $incomeSourceType")
        }
      )
    }
  }

  private def logAndShowError(isAgent: Boolean, errorMessage: String)(implicit user: MtdItUser[_]): Result = {
    Logger("application").error("[ConfirmReportingMethodSharedController]" + errorMessage)
    (if (isAgent) itvcErrorHandler else itvcErrorHandlerAgent).showInternalServerError()
  }

  private def handleSubmitRequest(taxYear: String, changeTo: String, isAgent: Boolean, maybeIncomeSourceId: Option[String], incomeSourceType: IncomeSourceType)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    val incomeSourceId: Option[String] = user.incomeSources.getIncomeSourceId(incomeSourceType, maybeIncomeSourceId)
    val incomeSourceBusinessName: Option[String] = user.incomeSources.getIncomeSourceBusinessName(incomeSourceType, maybeIncomeSourceId)
    val (backCall, successCall) = getRedirectCalls(taxYear, isAgent, changeTo, incomeSourceId, incomeSourceType)
    val errorCall = getErrorCall(incomeSourceType, isAgent)

    withIncomeSourcesFS {
      (getTaxYearModel(taxYear), getReportingMethod(changeTo)) match {
        case (Some(taxYearModel), Some(reportingMethod)) =>
          ConfirmReportingMethodForm(changeTo).bindFromRequest().fold(
            formWithErrors => {
              Future.successful(
                BadRequest(
                  confirmReportingMethod(
                    isAgent = isAgent,
                    form = formWithErrors,
                    backUrl = backCall.url,
                    newReportingMethod = reportingMethod,
                    taxYearEndYear = taxYearModel.endYear.toString,
                    taxYearStartYear = taxYearModel.startYear.toString,
                    postAction = getPostAction(taxYear, changeTo, isAgent, incomeSourceType),
                    isCurrentTaxYear = dateService.getCurrentTaxYearEnd().equals(taxYearModel.endYear)
                  )
                )
              )
            },
            _ => handleValidForm(errorCall, isAgent, successCall, taxYearModel, incomeSourceId, reportingMethod, incomeSourceBusinessName, incomeSourceType)
          )
        case (None, _) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse taxYear: $taxYear"))
        case (_, None) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse reporting method: $changeTo"))
      }
    }
  }

  def formatReportingMethod(reportingMethod: String): String = {
    reportingMethod match {
      case "annual" => "Annually"
      case "quarterly" => "Quarterly"
    }
  }

  private def handleValidForm(errorCall: Call,
                              isAgent: Boolean,
                              successCall: Call,
                              taxYears: TaxYear,
                              incomeSourceIdMaybe: Option[String],
                              reportingMethod: String,
                              incomeSourceBusinessName: Option[String],
                              incomeSourceType: IncomeSourceType
                             )(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {


    val updateIncomeSourceResFuture = for {
      updateIncomeSourceRes <- incomeSourceIdMaybe match {
        case Some(incomeSourceId) => updateIncomeSourceService.updateTaxYearSpecific(
          nino = user.nino,
          incomeSourceId = incomeSourceId,
          taxYearSpecific = TaxYearSpecific(taxYears.endYear.toString, reportingMethod match {
            case "annual" => true
            case "quarterly" => false
          })
        )
        case _ => Future.failed(MissingSessionKey(ManageIncomeSourceData.incomeSourceIdField))
      }
    } yield updateIncomeSourceRes

    updateIncomeSourceResFuture flatMap {
      case _: UpdateIncomeSourceListResponseError =>
        logAndShowError(isAgent, s"[handleValidForm]: Failed to update reporting method")
        auditingService
          .extendedAudit(
            IncomeSourceReportingMethodAuditModel(
              isSuccessful = false,
              journeyType = incomeSourceType.journeyType,
              operationType = "MANAGE",
              reportingMethodChangeTo = formatReportingMethod(reportingMethod),
              taxYear = taxYears.startYear.toString + "-" + taxYears.endYear.toString,
              businessName = incomeSourceBusinessName.getOrElse("Unknown")
            )
          )
        Future.successful(Redirect(errorCall))
      case res: UpdateIncomeSourceResponseModel =>
        logAndShowError(isAgent, s"Updated tax year specific reporting method: $res")
        auditingService
          .extendedAudit(
            IncomeSourceReportingMethodAuditModel(
              isSuccessful = true,
              journeyType = incomeSourceType.journeyType,
              operationType = "MANAGE",
              reportingMethodChangeTo = formatReportingMethod(reportingMethod),
              taxYear = taxYears.startYear.toString + "-" + taxYears.endYear.toString,
              businessName = incomeSourceBusinessName.getOrElse("Unknown")
            )
          )
        Future.successful(Redirect(successCall))
    } recover {
      case ex: Exception =>
        logAndShowError(isAgent, s"[handleUpdateSuccess]: Error updating reporting method: ${ex.getMessage}")
    }
  }

  private def getReportingMethod(reportingMethod: String): Option[String] = {
    Set("annual", "quarterly")
      .find(_ == reportingMethod.toLowerCase)
  }

  private def getRedirectCalls(taxYear: String,
                               isAgent: Boolean,
                               changeTo: String,
                               incomeSourceId: Option[String],
                               incomeSourceType: IncomeSourceType
                              ): (Call, Call) = {

    val (backCall, successCall) = (isAgent, incomeSourceType, incomeSourceId) match {
      case (false, SelfEmployment, Some(incomeSourceId)) =>
        routes.ManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceId) ->
          routes.ManageObligationsController.showSelfEmployment(changeTo, taxYear)
      case (_, SelfEmployment, Some(incomeSourceId)) =>
        routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceId) ->
          routes.ManageObligationsController.showAgentSelfEmployment(changeTo, taxYear)
      case (false, UkProperty, _) =>
        routes.ManageIncomeSourceDetailsController.showUkProperty() ->
          routes.ManageObligationsController.showUKProperty(changeTo, taxYear)
      case (_, UkProperty, _) =>
        routes.ManageIncomeSourceDetailsController.showUkPropertyAgent() ->
          routes.ManageObligationsController.showAgentUKProperty(changeTo, taxYear)
      case (false, _, _) =>
        routes.ManageIncomeSourceDetailsController.showForeignProperty() ->
          routes.ManageObligationsController.showForeignProperty(changeTo, taxYear)
      case (_, _, _) =>
        routes.ManageIncomeSourceDetailsController.showForeignPropertyAgent() ->
          routes.ManageObligationsController.showAgentForeignProperty(changeTo, taxYear)
    }

    (backCall, successCall)
  }

  private def getPostAction(taxYear: String, changeTo: String, isAgent: Boolean, incomeSourceType: IncomeSourceType): Call = {
    routes.ConfirmReportingMethodSharedController
      .submit(taxYear, changeTo, isAgent, incomeSourceType)
  }

  private def getErrorCall(incomeSourceType: IncomeSourceType, isAgent: Boolean): Call = {
    routes.ReportingMethodChangeErrorController
      .show(isAgent, incomeSourceType)
  }

  private def authenticatedAction(isAgent: Boolean
                                 )(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
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
}
