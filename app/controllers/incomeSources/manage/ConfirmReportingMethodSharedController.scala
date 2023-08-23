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
import play.api.i18n.Messages
import play.api.mvc._
import services.{IncomeSourceDetailsService, UpdateIncomeSourceService}
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
                                                       val retrieveBtaNavBar: NavBarPredicate)
                                                      (implicit val ec: ExecutionContext,
                                                       implicit val itvcErrorHandler: ItvcErrorHandler,
                                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       implicit override val mcc: MessagesControllerComponents,
                                                       implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def show(id: Option[String],
           taxYear: String,
           changeTo: String,
           incomeSourceKey: String,
           isAgent: Boolean
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleShowRequest(
      incomeSourceId = id,
      incomeSourceType = IncomeSourceType.get(incomeSourceKey),
      isAgent = isAgent,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def submit(id: String,
             taxYear: String,
             changeTo: String,
             incomeSourceKey: String,
             isAgent: Boolean
            ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleSubmitRequest(
      incomeSourceId = id,
      incomeSourceType = IncomeSourceType.get(incomeSourceKey),
      isAgent = isAgent,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  private def handleShowRequest(incomeSourceId: Option[String],
                                incomeSourceType: IncomeSourceType,
                                isAgent: Boolean,
                                taxYear: String,
                                changeTo: String)
                               (implicit user: MtdItUser[_]): Future[Result] = {

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)
    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearStartYearEndYear(taxYear)
    val maybeIncomeSourceId: Option[String] = getIncomeSourceId(incomeSourceId, incomeSourceType)

    withIncomeSourcesFS {
      Future.successful(
        (maybeTaxYearModel, newReportingMethod, maybeIncomeSourceId) match {
          case (None, _, _) => logAndShowError(isAgent, s"[handleShowRequest]: Could not parse taxYear: $taxYear")
          case (_, None, _) => logAndShowError(isAgent, s"[handleShowRequest]: Could not parse reporting method: $changeTo")
          case (_, _, None) => logAndShowError(isAgent, s"[handleShowRequest]: Could not find incomeSourceId: $incomeSourceId")
          case (Some(taxYearModel), Some(reportingMethod), Some(id)) =>
            val (backCall, postAction, _) = getRedirectCalls(id, taxYear, isAgent, changeTo, incomeSourceType)
            Ok(
              confirmReportingMethod(
                isAgent = isAgent,
                backUrl = backCall.url,
                postAction = postAction,
                reportingMethod = reportingMethod,
                form = ConfirmReportingMethodForm.form,
                taxYearEndYear = taxYearModel.endYear.toString,
                taxYearStartYear = taxYearModel.startYear.toString
              )
            )
        }
      )
    }
  }

  private def logAndShowError(isAgent: Boolean, errorMessage: String)(implicit user: MtdItUser[_]): Result = {
    Logger("application").error("[ConfirmReportingMethodSharedController]" + errorMessage)
    if (isAgent) itvcErrorHandler.showInternalServerError()
    else itvcErrorHandlerAgent.showInternalServerError()
  }

  private def handleSubmitRequest(incomeSourceId: String,
                                  incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  taxYear: String,
                                  changeTo: String)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)
    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearStartYearEndYear(taxYear)
    val (backCall, postAction, successCall): (Call, Call, Call) = getRedirectCalls(incomeSourceId, taxYear, isAgent, changeTo, incomeSourceType)

    withIncomeSourcesFS {
      (maybeTaxYearModel, newReportingMethod) match {
        case (None, _) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse taxYear: $taxYear"))
        case (_, None) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse reporting method: $changeTo"))
        case (Some(taxYears), Some(reportingMethod)) =>
          ConfirmReportingMethodForm.form.bindFromRequest().fold(
            formWithErrors =>
              handleFormWithErrors(formWithErrors, isAgent, taxYears, reportingMethod, postAction, backCall),
            _ =>
              handleValidForm(isAgent, incomeSourceId, taxYears, reportingMethod, successCall)
          )
      }
    }
  }

  private def handleFormWithErrors(formWithErrors: Form[ConfirmReportingMethodForm],
                              isAgent: Boolean,
                              taxYears: TaxYear,
                              reportingMethod: String,
                              postAction: Call,
                              backCall: Call)
                             (implicit user: MtdItUser[_]): Future[Result] = {
    Future.successful(
      BadRequest(
        confirmReportingMethod(
          isAgent = isAgent,
          form = formWithErrors,
          backUrl = backCall.url,
          postAction = postAction,
          taxYearEndYear = taxYears.endYear.toString,
          reportingMethod = reportingMethod,
          taxYearStartYear = taxYears.startYear.toString
        )
      )
    )
  }

  private def handleValidForm(isAgent: Boolean,
                                  incomeSourceId: String,
                                  taxYears: TaxYear,
                                  reportingMethod: String,
                                  successCall: Call)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val latencyIndicator = reportingMethod match {
      case "annual" => true
      case "quarterly" => false
    }

    val taxYearSpecific = TaxYearSpecific(taxYear = taxYears.endYear.toString, latencyIndicator = latencyIndicator)

    updateIncomeSourceService.updateTaxYearSpecific(
      nino = user.nino,
      incomeSourceId = incomeSourceId,
      taxYearSpecific = taxYearSpecific
    ).flatMap {
      case err: UpdateIncomeSourceResponseError => Future.successful(logAndShowError(isAgent, s"Failed to Update tax year specific reporting method: $err"))
      case res: UpdateIncomeSourceResponseModel =>
        Logger("application").info(s"Updated tax year specific reporting method: $res")
        Future.successful(Redirect(successCall))
    }.recover {
      case ex: Exception => logAndShowError(isAgent, s"[handleUpdateSuccess]: Error updating reporting method: ${ex.getMessage}")
    }
  }

  private def getReportingMethod(reportingMethod: String): Option[String] = {
    Set("annual", "quarterly")
      .find(_ == reportingMethod.toLowerCase)
  }

  private def getIncomeSourceId(maybeSoleTraderBusinessId: Option[String],
                                incomeSourceType: IncomeSourceType)
                               (implicit user: MtdItUser[_]): Option[String] = {
    incomeSourceType match {
      case SelfEmployment =>
        maybeSoleTraderBusinessId.filter { incomeSourceId =>
          user.incomeSources.businesses
            .exists(business => business.incomeSourceId == incomeSourceId && !business.isCeased)
        }
      case UkProperty =>
        user.incomeSources.properties
          .find(property => property.isUkProperty && !property.isCeased)
          .map(_.incomeSourceId)
      case ForeignProperty =>
        user.incomeSources.properties
          .find(property => property.isForeignProperty && !property.isCeased)
          .map(_.incomeSourceId)
    }
  }

  private def getRedirectCalls(incomeSourceId: String,
                               taxYear: String,
                               isAgent: Boolean,
                               changeTo: String,
                               incomeSourceType: IncomeSourceType): (Call, Call, Call) = {

    val (backCall, successCall) = (isAgent, incomeSourceType) match {
      case (false, UkProperty) =>
        (
          routes.ManageIncomeSourceDetailsController.showUkProperty(),
          routes.ManageObligationsController.showUKProperty(changeTo, taxYear)
        )
      case (false, ForeignProperty) =>
        (
          routes.ManageIncomeSourceDetailsController.showForeignProperty(),
          routes.ManageObligationsController.showForeignProperty(changeTo, taxYear)
        )
      case (false, SelfEmployment) =>
        (
          routes.ManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceId),
          routes.ManageObligationsController.showSelfEmployment(changeTo, taxYear, incomeSourceId)
        )
      case (true, UkProperty) =>
        (
          routes.ManageIncomeSourceDetailsController.showUkPropertyAgent(),
          routes.ManageObligationsController.showAgentUKProperty(changeTo, taxYear)
        )
      case (true, ForeignProperty) =>
        (
          routes.ManageIncomeSourceDetailsController.showForeignPropertyAgent(),
          routes.ManageObligationsController.showAgentForeignProperty(changeTo, taxYear)
        )
      case (true, SelfEmployment) =>
        (
          routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceId),
          routes.ManageObligationsController.showAgentSelfEmployment(changeTo, taxYear, incomeSourceId)
        )
    }

    (
      backCall,
      routes.ConfirmReportingMethodSharedController.submit(incomeSourceId, taxYear, changeTo, incomeSourceType.key, isAgent),
      successCall
    )
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
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
