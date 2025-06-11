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

package controllers.manageBusinesses.manage

import audit.AuditingService
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney._
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.ReportingMethod
import models.core.IncomeSourceId
import models.incomeSourceDetails.TaxYear.getTaxYearModel
import models.incomeSourceDetails.{LatencyYear, ManageIncomeSourceData, TaxYear, UIJourneySessionData}
import play.api.Logger
import play.api.MarkerContext.NoMarker
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.manage.ConfirmReportingMethod

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmReportingMethodSharedController @Inject()(val authActions: AuthActions,
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
  with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses with I18nSupport {

  def show(taxYear: String,
           changeTo: String,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authActions.asMTDIndividual.async { implicit user =>

    withSessionDataAndNewIncomeSourcesFS(IncomeSourceJourneyType(Manage, incomeSourceType), journeyState = AfterSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleShowRequest(taxYear, changeTo, false, incomeSourceType, incomeSourceIdOpt)
    }
  }

  def showAgent(taxYear: String,
           changeTo: String,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async { implicit user =>

    withSessionDataAndNewIncomeSourcesFS(IncomeSourceJourneyType(Manage, incomeSourceType), journeyState = AfterSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleShowRequest(taxYear, changeTo, true, incomeSourceType, incomeSourceIdOpt)
    }
  }

  def submit(taxYear: String,
             changeTo: String,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authActions.asMTDIndividual.async { implicit user =>

    withSessionDataAndNewIncomeSourcesFS(IncomeSourceJourneyType(Manage, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleSubmitRequest(taxYear, changeTo, false, incomeSourceIdOpt, incomeSourceType)
    }
  }

  def submitAgent(taxYear: String,
             changeTo: String,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async { implicit user =>

    withSessionDataAndNewIncomeSourcesFS(IncomeSourceJourneyType(Manage, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleSubmitRequest(taxYear, changeTo, true, incomeSourceIdOpt, incomeSourceType)
    }
  }

  private def handleShowRequest(taxYear: String,
                                changeTo: String,
                                isAgent: Boolean,
                                incomeSourceType: IncomeSourceType,
                                soleTraderBusinessId: Option[IncomeSourceId]
                               )(implicit user: MtdItUser[_]): Future[Result] = {

    val maybeIncomeSourceId: Option[IncomeSourceId] = user.incomeSources.getIncomeSourceId(incomeSourceType, soleTraderBusinessId.map(_.value))

    (getTaxYearModel(taxYear), getReportingMethod(changeTo), maybeIncomeSourceId) match {
      case (Some(taxYearModel), Some(reportingMethod), Some(id)) =>
        validateTaxYearAndReportingMethod(taxYearModel, ReportingMethod(reportingMethod), id, isAgent, changeTo, incomeSourceType)
      case (_, _, _) => Future.successful(logAndShowError(isAgent, s"[handleShowRequest]: Could not parse the values from session taxYear, changeTo and incomesourceId: $taxYear, $changeTo and $maybeIncomeSourceId"))
    }
  }

  private def validateTaxYearAndReportingMethod(taxYearModel: TaxYear,
                                                reportingMethod: ReportingMethod,
                                                id: IncomeSourceId,
                                                isAgent: Boolean,
                                                changeTo: String,
                                                incomeSourceType: IncomeSourceType
                                               )(implicit user: MtdItUser[_]): Future[Result] = {
    user.incomeSources.getLatencyDetails(incomeSourceType, id.value) match {
      case Some(latencyDetails) =>
        if (LatencyYear.isValidLatencyYear(taxYearModel, latencyDetails)) {
          handleValidTaxYearAndReportingMethod(taxYearModel, reportingMethod, id, isAgent, changeTo, incomeSourceType)
        } else {
          Future.successful(logAndShowError(isAgent, s"[handleShowRequest]: Could not parse taxYear: $taxYearModel"))
        }
      case None => Future.successful(logAndShowError(isAgent, s"[handleShowRequest]: Could not retrieve latency details"))
    }
  }

  private def handleValidTaxYearAndReportingMethod(taxYearModel: TaxYear,
                                                   reportingMethod: ReportingMethod,
                                                   id: IncomeSourceId,
                                                   isAgent: Boolean,
                                                   changeTo: String,
                                                   incomeSourceType: IncomeSourceType
                                                  )(implicit user: MtdItUser[_]): Future[Result] = {
    val (backCall, _) = getRedirectCalls(taxYearModel.toString, isAgent, changeTo, Some(id), incomeSourceType)
    val journeyType = IncomeSourceJourneyType(Manage, incomeSourceType)

    sessionService.getMongo(journeyType).flatMap {
      case Right(Some(sessionData)) =>
        val oldManageIncomeSourceSessionData = sessionData.manageIncomeSourceData.getOrElse(ManageIncomeSourceData())
        val updatedAddIncomeSourceSessionData = oldManageIncomeSourceSessionData.copy(reportingMethod = Some(reportingMethod.name), taxYear = Some(taxYearModel.endYear))
        val uiJourneySessionData: UIJourneySessionData = sessionData.copy(manageIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

        sessionService.setMongoData(uiJourneySessionData).map { _ =>
          Ok(
            confirmReportingMethod(
              isAgent = isAgent,
              backUrl = backCall.url,
              newReportingMethod = reportingMethod.name,
              taxYearEndYear = taxYearModel.endYear.toString,
              taxYearStartYear = taxYearModel.startYear.toString,
              postAction = getPostAction(taxYearModel.toString, changeTo, isAgent, incomeSourceType),
              isCurrentTaxYear = dateService.getCurrentTaxYearEnd.equals(taxYearModel.endYear),
              incomeSourceType = incomeSourceType
            )
          )
        }
      case _ => Future.failed(new Exception(s"failed to retrieve session data for ${journeyType.toString}"))
    }
  }
  
  private def logAndShowError(isAgent: Boolean, errorMessage: String)(implicit user: MtdItUser[_]): Result = {
    Logger("application").error("[ConfirmReportingMethodSharedController]" + errorMessage)
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
  }

  private def handleSubmitRequest(taxYear: String, changeTo: String, isAgent: Boolean, maybeIncomeSourceId: Option[IncomeSourceId],
                                  incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {

    withNewIncomeSourcesFS {
      val incomeSourceId: Option[IncomeSourceId] = user.incomeSources.getIncomeSourceId(incomeSourceType, maybeIncomeSourceId.map(m => m.value))
      val (_, successCall) = getRedirectCalls(taxYear, isAgent, changeTo, incomeSourceId, incomeSourceType)

      (getTaxYearModel(taxYear), getReportingMethod(changeTo)) match {
        case (Some(_), Some(_)) => Future.successful(Redirect(successCall))
        case (None, _) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse taxYear: $taxYear"))
        case (_, None) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse reporting method: $changeTo"))
      }
    }
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

    val successCall = routes.CheckYourAnswersController.show(isAgent, incomeSourceType)

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

}
