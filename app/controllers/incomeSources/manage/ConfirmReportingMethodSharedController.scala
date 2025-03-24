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
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney._
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import exceptions.MissingSessionKey
import forms.incomeSources.manage.ConfirmReportingMethodForm
import models.core.IncomeSourceId
import models.incomeSourceDetails.TaxYear.getTaxYearModel
import models.incomeSourceDetails.{LatencyYear, ManageIncomeSourceData, TaxYear}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.MarkerContext.NoMarker
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.manage.{ConfirmReportingMethod, ManageIncomeSources}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmReportingMethodSharedController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                                       val authActions: AuthActions,
                                                       val updateIncomeSourceService: UpdateIncomeSourceService,
                                                       val confirmReportingMethod: ConfirmReportingMethod,
                                                       val sessionService: SessionService,
                                                       val auditingService: AuditingService,
                                                       val dateService: DateService)
                                                      (implicit val ec: ExecutionContext,
                                                       val itvcErrorHandler: ItvcErrorHandler,
                                                       val mcc: MessagesControllerComponents,
                                                       val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with FeatureSwitching with IncomeSourcesUtils with JourneyChecker with I18nSupport {

  def show(taxYear: String,
           changeTo: String,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authActions.asMTDIndividual.async { implicit user =>
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Manage, incomeSourceType), journeyState = AfterSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleShowRequest(taxYear, changeTo, isAgent = false, incomeSourceType, incomeSourceIdOpt)
    }
  }

  def showAgent(taxYear: String,
           changeTo: String,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async { implicit user =>
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Manage, incomeSourceType), journeyState = AfterSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleShowRequest(taxYear, changeTo, isAgent = true, incomeSourceType, incomeSourceIdOpt)
    }
  }

  def submit(taxYear: String,
             changeTo: String,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authActions.asMTDIndividual.async { implicit user =>

    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Manage, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleSubmitRequest(taxYear, changeTo, isAgent = false, incomeSourceIdOpt, incomeSourceType)
    }
  }

  def submitAgent(taxYear: String,
             changeTo: String,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async { implicit user =>

    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Manage, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleSubmitRequest(taxYear, changeTo, isAgent = true, incomeSourceIdOpt, incomeSourceType)
    }
  }

  private def handleShowRequest(taxYear: String,
                                changeTo: String,
                                isAgent: Boolean,
                                incomeSourceType: IncomeSourceType,
                                soleTraderBusinessId: Option[IncomeSourceId])
                               (implicit user: MtdItUser[_]): Future[Result] = {

    val maybeIncomeSourceId: Option[IncomeSourceId] = user.incomeSources.getIncomeSourceId(incomeSourceType, soleTraderBusinessId.map(m => m.value))

    Future.successful(
      (getTaxYearModel(taxYear), getReportingMethod(changeTo), maybeIncomeSourceId) match {
        case (Some(taxYearModel), Some(reportingMethod), Some(id)) =>
user.incomeSources.getLatencyDetails(incomeSourceType, id.value) match {
              case Some(latencyDetails) =>
                if (LatencyYear.isValidLatencyYear(taxYearModel, latencyDetails)) {
          val (backCall, _) = getRedirectCalls(taxYear, isAgent, changeTo, Some(id), incomeSourceType)

          Ok(
            confirmReportingMethod(
              backUrl = backCall.url,
              isAgent = isAgent,
              newReportingMethod = reportingMethod,
              form = ConfirmReportingMethodForm(changeTo),
              taxYearEndYear = taxYearModel.endYear.toString,
              taxYearStartYear = taxYearModel.startYear.toString,
              postAction = getPostAction(taxYear, changeTo, isAgent, incomeSourceType),
              isCurrentTaxYear = dateService.getCurrentTaxYearEnd.equals(taxYearModel.endYear)
            ))
                }
                else {
                  logAndShowError(isAgent, s"[handleShowRequest]: Could not parse taxYear: $taxYear")
                }
              case None => logAndShowError(isAgent, s"[handleShowRequest]: Could not retrieve latency details")
          }
        case (None, _, _) => logAndShowError(isAgent, s"[handleShowRequest]: Could not parse taxYear: $taxYear")
        case (_, None, _) => logAndShowError(isAgent, s"[handleShowRequest]: Could not parse reporting method: $changeTo")
        case (_, _, None) => logAndShowError(isAgent, s"[handleShowRequest]: Could not find incomeSourceId for $incomeSourceType")
      }
    )
  }


  private def logAndShowError(isAgent: Boolean, errorMessage: String)(implicit user: MtdItUser[_]): Result = {
    Logger("application").error("[ConfirmReportingMethodSharedController]" + errorMessage)
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
  }

  private def handleSubmitRequest(taxYear: String, changeTo: String, isAgent: Boolean, maybeIncomeSourceId: Option[IncomeSourceId],
                                  incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {

    val incomeSourceId: Option[IncomeSourceId] = user.incomeSources.getIncomeSourceId(incomeSourceType, maybeIncomeSourceId.map(m => m.value))
    val incomeSourceBusinessName: Option[String] = user.incomeSources.getIncomeSourceBusinessName(incomeSourceType, maybeIncomeSourceId.map(m => m.value))
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
                    isCurrentTaxYear = dateService.getCurrentTaxYearEnd.equals(taxYearModel.endYear)
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

  private def formatReportingMethod(reportingMethod: String): String = {
    reportingMethod match {
      case "annual" => "Annually"
      case "quarterly" => "Quarterly"
    }
  }

  private def handleValidForm(errorCall: Call,
                              isAgent: Boolean,
                              successCall: Call,
                              taxYear: TaxYear,
                              incomeSourceIdMaybe: Option[IncomeSourceId],
                              reportingMethod: String,
                              incomeSourceBusinessName: Option[String],
                              incomeSourceType: IncomeSourceType
                             )(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val updateIncomeSourceResFuture = for {
      updateIncomeSourceRes <- incomeSourceIdMaybe match {
        case Some(incomeSourceId) => updateIncomeSourceService.updateTaxYearSpecific(
          nino = user.nino,
          incomeSourceId = incomeSourceId.value,
          taxYearSpecific = TaxYearSpecific(taxYear.endYear.toString, reportingMethod match {
            case "annual" => true
            case "quarterly" => false
          })
        )
        case _ => Future.failed(MissingSessionKey(ManageIncomeSourceData.incomeSourceIdField))
      }
    } yield updateIncomeSourceRes

    updateIncomeSourceResFuture flatMap {
      case _: UpdateIncomeSourceResponseError =>
        logAndShowError(isAgent, "[handleValidForm]: Failed to update reporting method")
        auditingService
          .extendedAudit(
            IncomeSourceReportingMethodAuditModel(
              isSuccessful = false,
              journeyType = incomeSourceType.journeyType,
              operationType = "MANAGE",
              reportingMethodChangeTo = formatReportingMethod(reportingMethod),
              taxYear = taxYear.startYear.toString + "-" + taxYear.endYear.toString,
              businessName = incomeSourceBusinessName.getOrElse("Unknown")
            )
          )
        Future.successful(Redirect(errorCall))
      case _: UpdateIncomeSourceResponseModel =>
        withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Manage, incomeSourceType), journeyState = AfterSubmissionPage) {
          uiJourneySessionData =>
            val newUIJourneySessionData = {
              uiJourneySessionData.copy(manageIncomeSourceData =
                Some(ManageIncomeSourceData(Some(incomeSourceIdMaybe.get.value), Some(reportingMethod), Some(taxYear.endYear), Some(true))))
            }
            sessionService.setMongoData(newUIJourneySessionData)
            Logger("application").debug("Updated tax year specific reporting method")
            auditingService
              .extendedAudit(
                IncomeSourceReportingMethodAuditModel(
                  isSuccessful = true,
                  journeyType = incomeSourceType.journeyType,
                  operationType = "MANAGE",
                  reportingMethodChangeTo = formatReportingMethod(reportingMethod),
                  taxYear = taxYear.startYear.toString + "-" + taxYear.endYear.toString,
                  businessName = incomeSourceBusinessName.getOrElse("Unknown")
                )
              )
            Future.successful(Redirect(successCall))
        }
    }
  } recover {
    case ex: Exception =>
      logAndShowError(isAgent, s"[handleValidForm]: Error updating reporting method: ${ex.getMessage} - ${ex.getCause}")
  }

  private def getReportingMethod(reportingMethod: String): Option[String] = {
    Set("annual", "quarterly")
      .find(_ == reportingMethod.toLowerCase)
  }

  private def getRedirectCalls(taxYear: String,
                               isAgent: Boolean,
                               changeTo: String,
                               incomeSourceId: Option[IncomeSourceId],
                               incomeSourceType: IncomeSourceType
                              ): (Call, Call) = {
    val successCall = if(isAgent) {
      routes.ManageObligationsController.showAgent(changeTo, taxYear, incomeSourceType)
    } else {
      routes.ManageObligationsController.show(changeTo, taxYear, incomeSourceType)
    }

    val backCallId = if (incomeSourceType == SelfEmployment) incomeSourceId.map(v => v.value) else None
    val backCall = routes.ManageIncomeSourceDetailsController.show(isAgent, incomeSourceType, backCallId)

    (backCall, successCall)
  }

  private def getPostAction(taxYear: String, changeTo: String, isAgent: Boolean, incomeSourceType: IncomeSourceType): Call = {
    if(isAgent){
        routes.ConfirmReportingMethodSharedController.submitAgent(taxYear, changeTo, incomeSourceType)
      } else {
        routes.ConfirmReportingMethodSharedController.submit(taxYear, changeTo, incomeSourceType)
      }
  }

  private def getErrorCall(incomeSourceType: IncomeSourceType, isAgent: Boolean): Call = {
    routes.ReportingMethodChangeErrorController
      .show(isAgent, incomeSourceType)
  }
}
