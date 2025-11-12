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

import audit.AuditingService
import audit.models.IncomeSourceReportingMethodAuditModel
import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.{AfterSubmissionPage, ReportingFrequencyPages}
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import models.admin.OptInOptOutContentUpdateR17
import models.core.IncomeSourceId
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels._
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.manageBusinesses.IncomeSourceRFService
import services.{CreateBusinessDetailsService, DateServiceInterface, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.IncomeSourceRFCheckDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceRFCheckDetailsController @Inject()(val checkDetailsView: IncomeSourceRFCheckDetails,
                                                     val incomeSourceRFService: IncomeSourceRFService,
                                                     val updateIncomeSourceService: UpdateIncomeSourceService,
                                                     val authActions: AuthActions,
                                                     val businessDetailsService: CreateBusinessDetailsService,
                                                     val auditingService: AuditingService,
                                                     val sessionService: SessionService,
                                                     val itvcErrorHandler: ItvcErrorHandler,
                                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                    (implicit val ec: ExecutionContext,
                                                   val mcc: MessagesControllerComponents,
                                                   val appConfig: FrontendAppConfig, dateService: DateServiceInterface
                                                    ) extends FrontendController(mcc)
  with JourneyCheckerManageBusinesses with I18nSupport {

  private lazy val errorRedirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType).url
    else routes.IncomeSourceReportingMethodNotSavedController.show(incomeSourceType).url

  lazy val redirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    if (isAgent) routes.IncomeSourceAddedController.showAgent(incomeSourceType).url
    else routes.IncomeSourceAddedController.show(incomeSourceType).url


  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = isAgent,
        incomeSourceType
      )(implicitly)
  }

  private def sendAuditEvent(isSuccessful: Boolean, newReportingMethod: TaxYearSpecific, incomeSourceType: IncomeSourceType, id: String)
                            (implicit user: MtdItUser[_]): Unit = {
    val businessName: String = user.incomeSources.getIncomeSourceBusinessName(incomeSourceType, Some(id)).getOrElse("Unknown")
    val reportingMethod = if (newReportingMethod.latencyIndicator) "Annually" else "Quarterly"

    auditingService.extendedAudit(
      IncomeSourceReportingMethodAuditModel(
        isSuccessful = isSuccessful,
        journeyType = incomeSourceType.journeyType,
        operationType = "ADD",
        reportingMethodChangeTo = reportingMethod,
        taxYear = (newReportingMethod.taxYear.toInt - 1).toString + "-" + newReportingMethod.taxYear,
        businessName = businessName
      )
    )
  }

  private def handleRequest(sources: IncomeSourceDetailsModel,
                            isAgent: Boolean,
                            incomeSourceType: IncomeSourceType)
                           (implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), AfterSubmissionPage) { sessionData =>

      val backUrl: String = controllers.manageBusinesses.add.routes.IncomeSourcesAccountingMethodController.show(incomeSourceType, isAgent).url
      val postAction: Call = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceRFCheckDetailsController.submit(isAgent, incomeSourceType) else {
        controllers.manageBusinesses.add.routes.IncomeSourceRFCheckDetailsController.submit(isAgent, incomeSourceType)
      }
      Future.successful {
        Ok(checkDetailsView(
          ReportingFrequencyCheckDetailsViewModel(incomeSourceType,
            sessionData.incomeSourceReportingFrequencyData.nonEmpty,
            sessionData.incomeSourceReportingFrequencyData.exists(_.isReportingQuarterlyCurrentYear),
            sessionData.incomeSourceReportingFrequencyData.exists(_.isReportingQuarterlyForNextYear),
            isEnabled(OptInOptOutContentUpdateR17)),
          postAction = postAction,
          isAgent,
          backUrl = backUrl
        ))
      }
    }
  }


  def submit(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      handleSubmit(isAgent = isAgent, incomeSourceType)
  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), ReportingFrequencyPages) { sessionData =>
      sessionData.addIncomeSourceData.flatMap(_.incomeSourceId) match {
        case Some(id) =>
          val newReportingMethods: Seq[TaxYearSpecific] = sessionData.incomeSourceReportingFrequencyData.map(
            data => Seq(TaxYearSpecific(dateService.getCurrentTaxYearEnd.toString, !data.isReportingQuarterlyCurrentYear))).getOrElse(Seq()) ++
            sessionData.incomeSourceReportingFrequencyData.map(
              data => Seq(TaxYearSpecific((dateService.getCurrentTaxYearEnd + 1).toString, !data.isReportingQuarterlyForNextYear))).getOrElse(Seq())

          updateReportingMethod(isAgent, IncomeSourceId(id), incomeSourceType, newReportingMethods.filterNot(_.latencyIndicator))
        case _ =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
          s"Unable to retrieve incomeSourceId from session data for $incomeSourceType on IncomeSourceReportingFrequency page")
          Future.successful(Redirect(errorRedirectUrl(isAgent, incomeSourceType)))
      }
    }
  }

  private def updateReportingMethod(isAgent: Boolean, id: IncomeSourceId, incomeSourceType: IncomeSourceType, newReportingMethods: Seq[TaxYearSpecific])
                                   (implicit user: MtdItUser[_]): Future[Result] = {
    val results = newReportingMethods.foldLeft(Future.successful(Seq.empty[UpdateIncomeSourceResponse])) { (prevFutRes, taxYearSpec) =>
      prevFutRes.flatMap { prevRes =>
        updateIncomeSourceService.updateTaxYearSpecific(user.nino, id.value, taxYearSpec).map { currRes =>
          val isSuccessful = currRes.isInstanceOf[UpdateIncomeSourceResponseModel]
          sendAuditEvent(isSuccessful, taxYearSpec, incomeSourceType, id.value)
          prevRes :+ currRes
        }
      }
    }
    handleUpdateResults(isAgent, incomeSourceType, results)
  }.recover {
    case ex: Exception =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      Redirect(errorRedirectUrl(isAgent, incomeSourceType))
  }

  private def handleUpdateResults(isAgent: Boolean,
                                  incomeSourceType: IncomeSourceType,
                                  updateResults: Future[Seq[UpdateIncomeSourceResponse]]): Future[Result] = {

    updateResults.map { results =>
      val successCount = results.count(_.isInstanceOf[UpdateIncomeSourceResponseModel])
      val errorCount = results.count(_.isInstanceOf[UpdateIncomeSourceResponseError])
      val prefix = ""

      if (successCount == results.length) {
        Logger("application").info(prefix + s"Successfully updated all new selected reporting methods for $incomeSourceType")
        Redirect(redirectUrl(isAgent, incomeSourceType))
      } else if (errorCount == results.length) {
        Logger("application").info(prefix + s"Unable to update all new selected reporting methods for $incomeSourceType")
        Redirect(errorRedirectUrl(isAgent, incomeSourceType))
      } else {
        Logger("application").info(prefix + s"Successfully updated one new selected reporting method for $incomeSourceType, the other one failed")
        Redirect(errorRedirectUrl(isAgent, incomeSourceType))
      }
    }
  }
}
