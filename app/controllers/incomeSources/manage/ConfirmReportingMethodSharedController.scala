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
import audit.models.SwitchReportingMethodAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment, UkProperty}
import exceptions.MissingSessionKey
import forms.incomeSources.manage.ConfirmReportingMethodForm
import forms.utils.SessionKeys
import forms.utils.SessionKeys.incomeSourceId
import models.incomeSourceDetails.TaxYear
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.MarkerContext.NoMarker
import play.api.data.FormError
import play.api.i18n.Lang
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
                                                       auditingService: AuditingService,
                                                       val sessionService: SessionService,
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
      sessionService.get(SessionKeys.incomeSourceId).flatMap {
        case Right(incomeSourceIdMayBe) => handleShowRequest(taxYear, changeTo, isAgent, incomeSourceType, incomeSourceIdMayBe)
        case Left(exception) => Future.failed(exception)
      }
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
      sessionService.get(SessionKeys.incomeSourceId).flatMap {
        case Right(incomeSourceIdMayBe) => handleSubmitRequest(taxYear, changeTo, isAgent, incomeSourceIdMayBe, incomeSourceType)
        case Left(exception) => Future.failed(exception)
      }
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

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)
    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearModel(taxYear)
    val maybeIncomeSourceId: Option[String] = user.incomeSources.getIncomeSourceId(incomeSourceType, soleTraderBusinessId)

    withIncomeSourcesFS {
      Future.successful(
        (maybeTaxYearModel, newReportingMethod, maybeIncomeSourceId) match {
          case (Some(taxYearModel), Some(reportingMethod), Some(id)) =>

            val (backCall, _) = getRedirectCalls(taxYear, isAgent, changeTo, Some(id), incomeSourceType)

            auditingService
              .extendedAudit(
                SwitchReportingMethodAuditModel(
                  taxYear = taxYear,
                  errorMessage = None,
                  reportingMethodChangeTo = changeTo,
                  journeyType = incomeSourceType.journeyType
                )
              )

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

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)
    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearModel(taxYear)
    val incomeSourceId: Option[String] = user.incomeSources.getIncomeSourceId(incomeSourceType, maybeIncomeSourceId)
    val (backCall, successCall) = getRedirectCalls(taxYear, isAgent, changeTo, incomeSourceId, incomeSourceType)
    val errorCall = getErrorCall(incomeSourceType, isAgent)

    withIncomeSourcesFS {
      (maybeTaxYearModel, newReportingMethod) match {
        case (Some(taxYearModel), Some(reportingMethod)) =>
          ConfirmReportingMethodForm(changeTo).bindFromRequest().fold(
            formWithErrors => {

              auditingService
                .extendedAudit(
                  SwitchReportingMethodAuditModel(
                    taxYear = taxYear,
                    reportingMethodChangeTo = changeTo.toLowerCase.capitalize,
                    errorMessage = formWithErrors.errors.flatMap(_.messages.map(messagesApi(_)(Lang("GB")))).headOption,
                    journeyType = incomeSourceType.journeyType
                  )
                )

              Future.successful(
                BadRequest(
                  confirmReportingMethod(
                    isAgent = isAgent,
                    form = formWithErrors,
                    backUrl = backCall.url,
                    newReportingMethod = reportingMethod,
                    taxYearEndYear = taxYearModel.endYear.toString,
                    taxYearStartYear = taxYearModel.startYear.toString,
                    isCurrentTaxYear = dateService.getCurrentTaxYearEnd().equals(taxYearModel.endYear),
                    postAction = getPostAction(taxYear, changeTo, isAgent, incomeSourceType)
                  )
                )
              )
            },
            _ => handleValidForm(errorCall, isAgent, successCall, taxYearModel, incomeSourceId, reportingMethod)
          )
        case (None, _) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse taxYear: $taxYear"))
        case (_, None) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse reporting method: $changeTo"))
      }
    }
  }

  private def handleValidForm(errorCall: Call,
                              isAgent: Boolean,
                              successCall: Call,
                              taxYears: TaxYear,
                              incomeSourceIdMaybe: Option[String],
                              reportingMethod: String
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
        case _ => Future.failed(MissingSessionKey(incomeSourceId))
      }
    } yield updateIncomeSourceRes

    updateIncomeSourceResFuture flatMap {
      case _: UpdateIncomeSourceResponseError =>
        logAndShowError(isAgent, s"[handleValidForm]: Failed to update reporting method")
        Future.successful(Redirect(errorCall))
      case res: UpdateIncomeSourceResponseModel =>
        logAndShowError(isAgent, s"Updated tax year specific reporting method: $res")
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
    routes.ConfirmReportingMethodSharedController.submit(taxYear, changeTo, isAgent, incomeSourceType)
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
