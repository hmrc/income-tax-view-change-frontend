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
import audit.models.ManageIncomeSourceCheckYourAnswersAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney._
import enums.JourneyType.{JourneyType, Manage}
import enums.{AnnualReportingMethod, QuarterlyReportingMethod, ReportingMethod}
import exceptions.MissingSessionKey
import models.core.IncomeSourceId
import models.incomeSourceDetails.viewmodels.CheckYourAnswersViewModel
import models.incomeSourceDetails.{ManageIncomeSourceData, TaxYear}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.mvc._
import services.{SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.manage.CheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val checkYourAnswers: CheckYourAnswers,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           val updateIncomeSourceService: UpdateIncomeSourceService,
                                           val sessionService: SessionService,
                                           val auditingService: AuditingService,
                                           val auth: AuthenticatorPredicate)
                                          (implicit val ec: ExecutionContext,
                                           implicit override val mcc: MessagesControllerComponents,
                                           implicit val itvcErrorHandler: ItvcErrorHandler,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>
    withSessionData(JourneyType(Manage, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
      val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
      val chnageToStringOpt = sessionData.manageIncomeSourceData.flatMap(_.reportingMethod)
      val taxYearStringOpt = sessionData.manageIncomeSourceData.flatMap(_.taxYear)
      val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
      handleShowRequest(taxYearStringOpt,
        chnageToStringOpt,
        isAgent,
        incomeSourceType,
        incomeSourceIdOpt,
        backUrl = {
          if (isAgent) controllers.routes.HomeController.showAgent
          else controllers.routes.HomeController.show()
        }.url
      )
    }
  }

  private def handleShowRequest(taxYearStringOpt: Option[Int], chnageToStringOpt: Option[String], isAgent: Boolean, incomeSourceType: IncomeSourceType, incomeSourceIdOpt: Option[IncomeSourceId], backUrl: String)(implicit user: MtdItUser[_]): Future[Result] = {
    val maybeIncomeSourceId: Option[IncomeSourceId] = user.incomeSources.getIncomeSourceId(incomeSourceType, incomeSourceIdOpt.map(m => m.value))
    withIncomeSourcesFS {
      Future.successful(
        (taxYearStringOpt, chnageToStringOpt, maybeIncomeSourceId) match {
          case (Some(taxYearStringOpt), Some(changeToStringOpt), Some(id)) => {
            Ok(checkYourAnswers(
              isAgent,
              backUrl,
              CheckYourAnswersViewModel(id, changeToStringOpt, TaxYear(startYear = taxYearStringOpt - 1, endYear = taxYearStringOpt), incomeSourceType),
              incomeSourceType
            )
            )
          }
          case (_, _, _) => logAndShowError(isAgent, s"[handleShowRequest]: Could not parse the values from session taxYear, changeTo and incomesourceId: $taxYearStringOpt, $chnageToStringOpt and $maybeIncomeSourceId")
        }
      )
    }
  }

  private def logAndShowError(isAgent: Boolean, errorMessage: String)(implicit user: MtdItUser[_]): Result = {
    Logger("application").error("[CheckYourAnswersController]" + errorMessage)
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
  }


  def submit(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>

    val successCall = getSuccessCall(isAgent, incomeSourceType)
    val errorCall = getErrorCall(incomeSourceType, isAgent)

    withIncomeSourcesFS {
      withSessionData(JourneyType(Manage, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
        sessionData.manageIncomeSourceData.map(x => (x.taxYear, x.reportingMethod, x.incomeSourceId)) match {
          case Some((Some(taxYear), Some(reportingMethod), incomeSourceIdStringOpt)) =>
            val incomeSourceBusinessName: Option[String] = user.incomeSources.getIncomeSourceBusinessName(incomeSourceType, incomeSourceIdStringOpt)
            val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
            val incomeSourceId: Option[IncomeSourceId] = user.incomeSources.getIncomeSourceId(incomeSourceType, incomeSourceIdOpt.map(m => m.value))

            handleSubmitRequest(errorCall, isAgent, successCall, TaxYear(startYear = taxYear - 1, endYear = taxYear), incomeSourceId, ReportingMethod(reportingMethod), incomeSourceBusinessName, incomeSourceType)
          case _ => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Missing session values"))
        }
      }
    }
  }

  private def handleSubmitRequest(errorCall: Call,
                                  isAgent: Boolean,
                                  successCall: Call,
                                  taxYear: TaxYear,
                                  incomeSourceIdMaybe: Option[IncomeSourceId],
                                  reportingMethod: ReportingMethod,
                                  incomeSourceBusinessName: Option[String],
                                  incomeSourceType: IncomeSourceType
                                 )(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val updateIncomeSourceResFuture = updateIncomeSource(taxYear.endYear, incomeSourceIdMaybe, reportingMethod)

    updateIncomeSourceResFuture flatMap {
      case _: UpdateIncomeSourceResponseError =>
        handleUpdateError(errorCall, isAgent, reportingMethod, taxYear, incomeSourceBusinessName, incomeSourceType)
      case _: UpdateIncomeSourceResponseModel =>
        incomeSourceIdMaybe match {
          case Some(incomeSourceId) =>
            handleSuccessfulUpdate(errorCall, successCall, isAgent, reportingMethod, taxYear, incomeSourceId, incomeSourceBusinessName, incomeSourceType)
          case _ => Future.failed(MissingSessionKey(ManageIncomeSourceData.incomeSourceIdField))
        }
    } recover {
      case ex: Exception =>
        logAndShowError(isAgent, s"Error updating reporting method: ${ex.getMessage} - ${ex.getCause}")
        Redirect(errorCall)
    }
  }

  private def handleUpdateError(errorCall: Call,
                                isAgent: Boolean,
                                reportingMethod: ReportingMethod,
                                taxYear: TaxYear,
                                incomeSourceBusinessName: Option[String],
                                incomeSourceType: IncomeSourceType
                               )(implicit user: MtdItUser[_]): Future[Result] = {
    logAndShowError(isAgent, "Failed to update reporting method")
    auditingService
      .extendedAudit(
        ManageIncomeSourceCheckYourAnswersAuditModel(
          isSuccessful = false,
          journeyType = incomeSourceType.journeyType,
          operationType = "MANAGE",
          reportingMethodChangeTo = formatReportingMethod(reportingMethod),
          taxYear = taxYear.startYear.toString + "-" + taxYear.endYear.toString,
          businessName = incomeSourceBusinessName.getOrElse("Unknown")
        )
      )
    Future.successful(Redirect(errorCall))
  }

  private def handleSuccessfulUpdate(errorCall: Call,
                                     successCall: Call,
                                     isAgent: Boolean,
                                     reportingMethod: ReportingMethod,
                                     taxYear: TaxYear,
                                     incomeSourceId: IncomeSourceId,
                                     incomeSourceBusinessName: Option[String],
                                     incomeSourceType: IncomeSourceType
                                    )(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(JourneyType(Manage, incomeSourceType), journeyState = BeforeSubmissionPage) { uiJourneySessionData =>
      val newUIJourneySessionData = uiJourneySessionData.copy(manageIncomeSourceData =
        Some(ManageIncomeSourceData(Some(incomeSourceId.value), Some(reportingMethod.name), Some(taxYear.endYear), Some(true))))

      sessionService.setMongoData(newUIJourneySessionData).map { _ =>
        Logger("application").debug("[CheckYourAnswersController] Updated tax year specific reporting method")
        auditingService
          .extendedAudit(
            ManageIncomeSourceCheckYourAnswersAuditModel(
              isSuccessful = true,
              journeyType = incomeSourceType.journeyType,
              operationType = "MANAGE",
              reportingMethodChangeTo = formatReportingMethod(reportingMethod),
              taxYear = taxYear.startYear.toString + "-" + taxYear.endYear.toString,
              businessName = incomeSourceBusinessName.getOrElse("Unknown")
            )
          )
        Redirect(successCall)
      }
    }
  }


  private def updateIncomeSource(taxYearEnd: Int, incomeSourceIdMaybe: Option[IncomeSourceId], reportingMethod: ReportingMethod)(implicit user: MtdItUser[_], hc: HeaderCarrier) = {
    val updateIncomeSourceResFuture = for {
      updateIncomeSourceRes <- incomeSourceIdMaybe match {
        case Some(incomeSourceId) => updateIncomeSourceService.updateTaxYearSpecific(
          nino = user.nino,
          incomeSourceId = incomeSourceId.value,
          taxYearSpecific = TaxYearSpecific(taxYearEnd.toString, reportingMethod match {
            case AnnualReportingMethod => true
            case QuarterlyReportingMethod => false
          })
        )
        case _ => Future.failed(MissingSessionKey(ManageIncomeSourceData.incomeSourceIdField))
      }
    } yield updateIncomeSourceRes
    updateIncomeSourceResFuture
  }

  private def getSuccessCall(isAgent: Boolean, incomeSourceType: IncomeSourceType): Call = {
    (isAgent, incomeSourceType) match {
      case (false, SelfEmployment) => controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent, incomeSourceType)
      case (_, SelfEmployment) => controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent, incomeSourceType)
      case (false, UkProperty) => controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent, incomeSourceType)
      case (_, UkProperty) => controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent, incomeSourceType)
      case (false, _) => controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent, incomeSourceType)
      case (_, _) => controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent, incomeSourceType)
    }
  }


  private def formatReportingMethod(reportingMethod: ReportingMethod): String = {
    reportingMethod match {
      case AnnualReportingMethod => "Annually"
      case QuarterlyReportingMethod => "Quarterly"
    }
  }

  private def getErrorCall(incomeSourceType: IncomeSourceType, isAgent: Boolean): Call = {
    controllers.manageBusinesses.manage.routes.ReportingMethodChangeErrorController
      .show(isAgent, incomeSourceType)
  }

}