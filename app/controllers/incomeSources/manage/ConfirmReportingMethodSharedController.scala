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
import play.api.data.Form
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
                                                       val dateService: DateService)
                                                      (implicit val ec: ExecutionContext,
                                                       implicit val itvcErrorHandler: ItvcErrorHandler,
                                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       implicit override val mcc: MessagesControllerComponents,
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
    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearStartYearEndYear(taxYear)
    val maybeIncomeSourceId: Option[String] = user.incomeSources.getIncomeSourceId(incomeSourceType, soleTraderBusinessId)
    withIncomeSourcesFS {
      Future.successful(
        (maybeTaxYearModel, newReportingMethod, maybeIncomeSourceId) match {
          case (Some(taxYearModel), Some(reportingMethod), Some(id)) =>
            val (backCall, postAction, _, _) = getRedirectCalls(taxYear, isAgent, changeTo, id, incomeSourceType)
            val isCurrentTaxYear = dateService.getCurrentTaxYearEnd().equals(taxYearModel.endYear)

            Ok(
              confirmReportingMethod(
                form = ConfirmReportingMethodForm.form,
                backUrl = backCall.url,
                postAction = postAction,
                isAgent = isAgent,
                taxYearStartYear = taxYearModel.startYear.toString,
                taxYearEndYear = taxYearModel.endYear.toString,
                newReportingMethod = reportingMethod,
                isCurrentTaxYear = isCurrentTaxYear
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

  private def handleSubmitRequest(taxYear: String,
                                  changeTo: String,
                                  isAgent: Boolean,
                                  incomeSourceId: String,
                                  incomeSourceType: IncomeSourceType
                                 )(implicit user: MtdItUser[_]): Future[Result] = {

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)
    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearStartYearEndYear(taxYear)
    val (backCall, postAction, successCall, errorCall) = getRedirectCalls(taxYear, isAgent, changeTo, incomeSourceId, incomeSourceType)

    withIncomeSourcesFS {
      (maybeTaxYearModel, newReportingMethod) match {
        case (Some(taxYears), Some(reportingMethod)) =>
          ConfirmReportingMethodForm.form.bindFromRequest().fold(
            formWithErrors =>
              handleFormWithErrors(
                isAgent = isAgent,
                taxYears = taxYears,
                backCall = backCall,
                postAction = postAction,
                formWithErrors = formWithErrors,
                reportingMethod = reportingMethod
              ),
            _ =>
              handleValidForm(
                isAgent = isAgent,
                taxYears = taxYears,
                errorCall = errorCall,
                successCall = successCall,
                incomeSourceId = incomeSourceId,
                reportingMethod = reportingMethod
              )
          )
        case (None, _) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse taxYear: $taxYear"))
        case (_, None) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse reporting method: $changeTo"))
      }
    }
  }

  private def handleFormWithErrors(backCall: Call,
                                   postAction: Call,
                                   isAgent: Boolean,
                                   taxYears: TaxYear,
                                   reportingMethod: String,
                                   formWithErrors: Form[ConfirmReportingMethodForm]
                                  )(implicit user: MtdItUser[_]): Future[Result] = {

    val isCurrentTaxYear = dateService.getCurrentTaxYearEnd().equals(taxYears.endYear)
    Future.successful(
      BadRequest(
        confirmReportingMethod(
          isAgent = isAgent,
          form = formWithErrors,
          backUrl = backCall.url,
          postAction = postAction,
          newReportingMethod = reportingMethod,
          taxYearEndYear = taxYears.endYear.toString,
          taxYearStartYear = taxYears.startYear.toString,
          isCurrentTaxYear = isCurrentTaxYear
        )
      )
    )
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
      case _: UpdateIncomeSourceResponseError => Future.successful(Redirect(errorCall))
      case res: UpdateIncomeSourceResponseModel =>
        Logger("application").info(s"Updated tax year specific reporting method: $res")
        Future.successful(Redirect(successCall))
    } recover {
      case ex: Exception => logAndShowError(isAgent, s"[handleUpdateSuccess]: Error updating reporting method: ${ex.getMessage}")
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
                              ): (Call, Call, Call, Call) = {

    val postAction: Call = routes.ConfirmReportingMethodSharedController
      .submit(incomeSourceId, taxYear, changeTo, isAgent, incomeSourceType)

    val errorCall: Call = routes.ReportingMethodChangeErrorController
      .show(id = if (incomeSourceType.equals(SelfEmployment)) Some(incomeSourceId) else None, isAgent, incomeSourceType)

    val (backCall, successCall) = (isAgent, incomeSourceType) match {
      case (false, SelfEmployment) =>
        routes.ManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceId) ->
          routes.ManageObligationsController.showSelfEmployment(changeTo, taxYear, incomeSourceId)
      case (true, SelfEmployment) =>
        routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceId) ->
          routes.ManageObligationsController.showAgentSelfEmployment(changeTo, taxYear, incomeSourceId)
      case (false, UkProperty) =>
        routes.ManageIncomeSourceDetailsController.showUkProperty() ->
          routes.ManageObligationsController.showUKProperty(changeTo, taxYear)
      case (true, UkProperty) =>
        routes.ManageIncomeSourceDetailsController.showUkPropertyAgent() ->
          routes.ManageObligationsController.showAgentUKProperty(changeTo, taxYear)
      case (false, ForeignProperty) =>
        routes.ManageIncomeSourceDetailsController.showForeignProperty() ->
          routes.ManageObligationsController.showForeignProperty(changeTo, taxYear)
      case (true, ForeignProperty) =>
        routes.ManageIncomeSourceDetailsController.showForeignPropertyAgent() ->
          routes.ManageObligationsController.showAgentForeignProperty(changeTo, taxYear)
    }

    (backCall, postAction, successCall, errorCall)
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
