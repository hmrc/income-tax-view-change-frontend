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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney._
import enums.JourneyType.{JourneyType, Manage}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import models.core.IncomeSourceId
import models.incomeSourceDetails.TaxYear.getTaxYearModel
import models.incomeSourceDetails.{LatencyYear, ManageIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.MarkerContext.NoMarker
import play.api.mvc._
import services.{DateService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.manage.{ConfirmReportingMethod, ManageIncomeSources}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmReportingMethodSharedController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                                       val authorisedFunctions: AuthorisedFunctions,
                                                       val updateIncomeSourceService: UpdateIncomeSourceService,
                                                       val confirmReportingMethod: ConfirmReportingMethod,
                                                       val sessionService: SessionService,
                                                       val auditingService: AuditingService,
                                                       val dateService: DateService,
                                                       val auth: AuthenticatorPredicate)
                                                      (implicit val ec: ExecutionContext,
                                                       implicit val itvcErrorHandler: ItvcErrorHandler,
                                                       override implicit val mcc: MessagesControllerComponents,
                                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  def show(taxYear: String,
           changeTo: String,
           isAgent: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>
    withSessionData(JourneyType(Manage, incomeSourceType), journeyState = AfterSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleShowRequest(taxYear, changeTo, isAgent, incomeSourceType, incomeSourceIdOpt)
    }
  }

  def submit(taxYear: String,
             changeTo: String,
             isAgent: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>

    withSessionData(JourneyType(Manage, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleSubmitRequest(taxYear, changeTo, isAgent, incomeSourceIdOpt, incomeSourceType)
    }
  }

  private def handleShowRequest(taxYear: String,
                                changeTo: String,
                                isAgent: Boolean,
                                incomeSourceType: IncomeSourceType,
                                soleTraderBusinessId: Option[IncomeSourceId])
                               (implicit user: MtdItUser[_]): Future[Result] = {

    val maybeIncomeSourceId: Option[IncomeSourceId] = user.incomeSources.getIncomeSourceId(incomeSourceType, soleTraderBusinessId.map(m => m.value))

    (getTaxYearModel(taxYear), getReportingMethod(changeTo), maybeIncomeSourceId) match {
      case (Some(taxYearModel), Some(reportingMethod), Some(id)) =>
        user.incomeSources.getLatencyDetails(incomeSourceType, id.value) match {
          case Some(latencyDetails) =>
            if (LatencyYear.isValidLatencyYear(taxYearModel, latencyDetails)) {
              val (backCall, _) = getRedirectCalls(taxYear, isAgent, changeTo, Some(id), incomeSourceType)
              val journeyType = JourneyType(Manage, incomeSourceType)
              sessionService.getMongo(journeyType.toString).flatMap {
                case Right(Some(sessionData)) =>
                  val oldManageIncomeSourceSessionData = sessionData.manageIncomeSourceData.getOrElse(ManageIncomeSourceData())
                  val updatedAddIncomeSourceSessionData = oldManageIncomeSourceSessionData.copy(reportingMethod = Some(reportingMethod), taxYear = Some(taxYearModel.endYear))
                  val uiJourneySessionData: UIJourneySessionData = sessionData.copy(manageIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

                  sessionService.setMongoData(uiJourneySessionData)
                case _ => Future.failed(new Exception(s"failed to retrieve session data for ${journeyType.toString}"))
              }

              Future.successful(Ok(
                confirmReportingMethod(
                  isAgent = isAgent,
                  backUrl = backCall.url,
                  newReportingMethod = reportingMethod,
                  form = ConfirmReportingMethodForm(changeTo),
                  taxYearEndYear = taxYearModel.endYear.toString,
                  taxYearStartYear = taxYearModel.startYear.toString,
                  postAction = getPostAction(taxYear, changeTo, isAgent, incomeSourceType),
                  isCurrentTaxYear = dateService.getCurrentTaxYearEnd.equals(taxYearModel.endYear)
                ))
              )
            }
            else {
              Future.successful(logAndShowError(isAgent, s"[handleShowRequest]: Could not parse taxYear: $taxYear"))
            }
          case None => Future.successful(logAndShowError(isAgent, s"[handleShowRequest]: Could not retrieve latency details"))
        }
      case (_, _, _) => Future.successful(logAndShowError(isAgent, s"[handleShowRequest]: Could not parse the values from session taxYear, changeTo and incomesourceId: $taxYear, $changeTo and $maybeIncomeSourceId"))
    }

  }


  private def logAndShowError(isAgent: Boolean, errorMessage: String)(implicit user: MtdItUser[_]): Result = {
    Logger("application").error("[ConfirmReportingMethodSharedController]" + errorMessage)
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
  }

  private def handleSubmitRequest(taxYear: String, changeTo: String, isAgent: Boolean, maybeIncomeSourceId: Option[IncomeSourceId],
                                  incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {

    val incomeSourceId: Option[IncomeSourceId] = user.incomeSources.getIncomeSourceId(incomeSourceType, maybeIncomeSourceId.map(m => m.value))
    val (backCall, successCall) = getRedirectCalls(taxYear, isAgent, changeTo, incomeSourceId, incomeSourceType)

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
            _ => Future.successful(Redirect(successCall))
          )
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
    routes.ConfirmReportingMethodSharedController
      .submit(taxYear, changeTo, isAgent, incomeSourceType)
  }

}
