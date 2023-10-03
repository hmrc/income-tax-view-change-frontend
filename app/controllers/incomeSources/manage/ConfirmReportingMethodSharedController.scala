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

import audit.models.SwitchReportingMethodAuditModel
import audit.AuditingService
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import models.incomeSourceDetails.TaxYear
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.MarkerContext.NoMarker
import play.api.data.FormError
import play.api.i18n.Lang
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService, UpdateIncomeSourceService}
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
                                                       val dateService: DateService)
                                                      (implicit val ec: ExecutionContext,
                                                       implicit val itvcErrorHandler: ItvcErrorHandler,
                                                       override implicit val mcc: MessagesControllerComponents,
                                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def show(id: Option[String],
           taxYear: String,
           changeTo: String,
           isAgent: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleShowRequest(taxYear, changeTo, isAgent, incomeSourceType, id)
  }

  def submit(id: String,
             taxYear: String,
             changeTo: String,
             isAgent: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleSubmitRequest(taxYear, changeTo, isAgent, id, incomeSourceType)
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

            val (backCall, _) = getRedirectCalls(taxYear, isAgent, changeTo, id, incomeSourceType)

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
                postAction = getPostAction(id, taxYear, changeTo, isAgent, incomeSourceType),
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

  private def handleSubmitRequest(taxYear: String, changeTo: String, isAgent: Boolean, incomeSourceId: String, incomeSourceType: IncomeSourceType)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)
    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearModel(taxYear)
    val (backCall, successCall) = getRedirectCalls(taxYear, isAgent, changeTo, incomeSourceId, incomeSourceType)
    val errorCall = getErrorCall(incomeSourceType, incomeSourceId, isAgent)

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
                    postAction = getPostAction(incomeSourceId, taxYear, changeTo, isAgent, incomeSourceType)
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
                              incomeSourceId: String,
                              reportingMethod: String
                             )(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    updateIncomeSourceService.updateTaxYearSpecific(
      nino = user.nino,
      incomeSourceId = incomeSourceId,
      taxYearSpecific = TaxYearSpecific(taxYears.endYear.toString, reportingMethod match {
        case "annual" => true
        case "quarterly" => false
      })
    ) flatMap {
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
                               incomeSourceId: String,
                               incomeSourceType: IncomeSourceType
                              ): (Call, Call) = {

    val (backCall, successCall) = (isAgent, incomeSourceType) match {
      case (false, SelfEmployment) =>
        routes.ManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceId) ->
        routes.ManageObligationsController.showSelfEmployment(changeTo, taxYear, incomeSourceId)
      case (_, SelfEmployment) =>
        routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceId) ->
        routes.ManageObligationsController.showAgentSelfEmployment(changeTo, taxYear, incomeSourceId)
      case (false, UkProperty) =>
        routes.ManageIncomeSourceDetailsController.showUkProperty() ->
        routes.ManageObligationsController.showUKProperty(changeTo, taxYear)
      case (_, UkProperty) =>
        routes.ManageIncomeSourceDetailsController.showUkPropertyAgent() ->
        routes.ManageObligationsController.showAgentUKProperty(changeTo, taxYear)
      case (false, _) =>
        routes.ManageIncomeSourceDetailsController.showForeignProperty() ->
        routes.ManageObligationsController.showForeignProperty(changeTo, taxYear)
      case (_, _) =>
        routes.ManageIncomeSourceDetailsController.showForeignPropertyAgent() ->
        routes.ManageObligationsController.showAgentForeignProperty(changeTo, taxYear)
    }

    (backCall, successCall)
  }

  private def getPostAction(incomeSourceId: String, taxYear: String, changeTo: String, isAgent: Boolean, incomeSourceType: IncomeSourceType): Call = {
    routes.ConfirmReportingMethodSharedController.submit(incomeSourceId, taxYear, changeTo, isAgent, incomeSourceType)
  }

  private def getErrorCall(incomeSourceType: IncomeSourceType, incomeSourceId: String, isAgent: Boolean): Call = {
    routes.ReportingMethodChangeErrorController
      .show(id = if (incomeSourceType.equals(SelfEmployment)) Some(incomeSourceId) else None, isAgent, incomeSourceType)
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
