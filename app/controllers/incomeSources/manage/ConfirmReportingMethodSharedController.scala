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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceJourney, SelfEmployment, UkProperty}
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
import views.html.errorPages.CustomNotFoundError
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
                                                       val customNotFoundErrorView: CustomNotFoundError,
                                                       val confirmReportingMethod: ConfirmReportingMethod,
                                                       val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                       val retrieveBtaNavBar: NavBarPredicate)
                                                      (implicit val ec: ExecutionContext,
                                                       implicit val itvcErrorHandler: ItvcErrorHandler,
                                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       implicit override val mcc: MessagesControllerComponents,
                                                       implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def showSoleTraderBusiness(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = Some(id),
      incomeSourceJourney = SelfEmployment
    )
  }

  def showSoleTraderBusinessAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = Some(id),
      incomeSourceJourney = SelfEmployment
    )
  }

  def submitSoleTraderBusiness(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = id,
      incomeSourceJourney = SelfEmployment
    )
  }

  def submitSoleTraderBusinessAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = id,
      incomeSourceJourney = SelfEmployment
    )
  }

  def showUKProperty(taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = None,
      incomeSourceJourney = UkProperty
    )
  }

  def showUKPropertyAgent(taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = None,
      incomeSourceJourney = UkProperty
    )
  }

  def submitUKProperty(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceJourney = UkProperty,
      incomeSourceId = id
    )
  }

  def submitUKPropertyAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceJourney = UkProperty,
      incomeSourceId = id
    )
  }

  def showForeignProperty(taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = None,
      incomeSourceJourney = ForeignProperty
    )
  }

  def showForeignPropertyAgent(taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = None,
      incomeSourceJourney = ForeignProperty
    )
  }

  def submitForeignProperty(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = id,
      incomeSourceJourney = ForeignProperty
    )
  }

  def submitForeignPropertyAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = id,
      incomeSourceJourney = ForeignProperty
    )
  }

  private def show(incomeSourceId: Option[String],
                   incomeSourceJourney: IncomeSourceJourney,
                   taxYear: String,
                   changeTo: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          taxYear = taxYear,
          changeTo = changeTo,
          incomeSourceJourney = incomeSourceJourney,
          incomeSourceId = incomeSourceId
        )
    }

  private def showAgent(incomeSourceId: Option[String],
                        incomeSourceJourney: IncomeSourceJourney,
                        taxYear: String,
                        changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              incomeSourceId = incomeSourceId,
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              incomeSourceJourney = incomeSourceJourney
            )
        }
  }

  private def submit(incomeSourceId: String,
                     incomeSourceJourney: IncomeSourceJourney,
                     taxYear: String,
                     changeTo: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleSubmitRequest(
          isAgent = false,
          taxYear = taxYear,
          changeTo = changeTo,
          incomeSourceId = incomeSourceId,
          incomeSourceJourney = incomeSourceJourney
        )
    }

  private def submitAgent(incomeSourceId: String,
                          incomeSourceJourney: IncomeSourceJourney,
                          taxYear: String,
                          changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              incomeSourceId = incomeSourceId,
              incomeSourceJourney = incomeSourceJourney
            )
        }
  }

  private def handleRequest(incomeSourceId: Option[String],
                            isAgent: Boolean,
                            taxYear: String,
                            changeTo: String,
                            incomeSourceJourney: IncomeSourceJourney)
                           (implicit user: MtdItUser[_], messages: Messages): Future[Result] = {

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)

    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearStartYearEndYear(taxYear)

    val maybeIncomeSourceId: Option[String] = getIncomeSourceId(incomeSourceId, incomeSourceJourney)


    (maybeTaxYearModel, newReportingMethod, maybeIncomeSourceId) match {
      case (None, _, _) =>
        Future.successful(logAndShowError(isAgent, s"[handleRequest]: Could not parse taxYear: $taxYear"))
      case (_, None, _) =>
        Future.successful(logAndShowError(isAgent, s"[handleRequest]:Could not parse reporting method: $changeTo"))
      case (_, _, None) =>
        Future.successful(logAndShowError(isAgent, s"[handleRequest]: Could not find incomeSourceId: $incomeSourceId"))
      case (Some(taxYearModel), Some(reportingMethod), Some(id)) =>
        getRedirectCalls(
          incomeSourceId = id,
          isAgent = isAgent,
          taxYear = taxYear,
          changeTo = changeTo,
          incomeSourceJourney = incomeSourceJourney
        ) match {
          case (backCall, postAction, _) =>
            Future.successful(
              Ok(
                confirmReportingMethod(
                  isAgent = isAgent,
                  backUrl = backCall.url,
                  postAction = postAction,
                  reportingMethod = reportingMethod,
                  taxYearEndYear = taxYearModel.endYear.toString,
                  form = ConfirmReportingMethodForm.form,
                  taxYearStartYear = taxYearModel.startYear.toString
                )(messages, user)
              )
            )
        }
    }
  }

  private def logAndShowError(isAgent: Boolean, errorMessage: String)(implicit user: MtdItUser[_]): Result = {
    Logger("application").error("[ConfirmReportingMethodSharedController]" + errorMessage)
    if (isAgent) {
      itvcErrorHandler.showInternalServerError()
    } else {
      itvcErrorHandlerAgent.showInternalServerError()
    }
  }

  private def handleSubmitRequest(taxYear: String,
                                  isAgent: Boolean,
                                  changeTo: String,
                                  incomeSourceId: String,
                                  incomeSourceJourney: IncomeSourceJourney)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)
    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearStartYearEndYear(taxYear)
    val redirectCalls: (Call, Call, Call) = getRedirectCalls(incomeSourceId, taxYear, isAgent, changeTo, incomeSourceJourney)

    withIncomeSourcesFS {
      (maybeTaxYearModel, newReportingMethod, redirectCalls) match {
        case (None, _, _) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse taxYear: $taxYear"))
        case (_, None, _) => Future.successful(logAndShowError(isAgent, s"[handleSubmitRequest]: Could not parse reporting method: $changeTo"))
        case (Some(taxYears), Some(reportingMethod), redirectCallsTuple) =>
          ConfirmReportingMethodForm.form.bindFromRequest().fold(
            formWithErrors => handleFormError(formWithErrors, isAgent, taxYears, reportingMethod, redirectCallsTuple),
            _ => handleUpdateSuccess(isAgent, incomeSourceId, taxYears, reportingMethod, redirectCallsTuple)
          )
      }
    }
  }

  private def handleFormError(formWithErrors: Form[ConfirmReportingMethodForm],
                              isAgent: Boolean,
                              taxYears: TaxYear,
                              reportingMethod: String,
                              redirectCallsTuple: (Call, Call, Call))(implicit messages: Messages, user: MtdItUser[_]): Future[Result] = {
    val (backCall, postAction, _) = redirectCallsTuple
    Future.successful(BadRequest(
      confirmReportingMethod(
        isAgent = isAgent,
        form = formWithErrors,
        backUrl = backCall.url,
        postAction = postAction,
        taxYearEndYear = taxYears.endYear.toString,
        reportingMethod = reportingMethod,
        taxYearStartYear = taxYears.startYear.toString
      )(messages, user)
    )
    )
  }

  private def handleUpdateSuccess(isAgent: Boolean,
                                  incomeSourceId: String,
                                  taxYears: TaxYear,
                                  reportingMethod: String,
                                  redirectCallsTuple: (Call, Call, Call))(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    val latencyIndicator = reportingMethod match {
      case "annual" => true
      case "quarterly" => false
    }

    val taxYearSpecific = TaxYearSpecific(taxYear = taxYears.endYear.toString, latencyIndicator = latencyIndicator)
    println(Console.MAGENTA + taxYearSpecific + Console.WHITE)

    updateIncomeSourceService.updateTaxYearSpecific(
      nino = user.nino,
      incomeSourceId = incomeSourceId,
      taxYearSpecific = taxYearSpecific
    ).flatMap {
      case err: UpdateIncomeSourceResponseError => Future.successful(logAndShowError(isAgent, s"Failed to Update tax year specific reporting method: $err"))
      case res: UpdateIncomeSourceResponseModel =>
        Logger("application").info(s"Updated tax year specific reporting method: $res")
        Future.successful(Redirect(redirectCallsTuple._3))
    }.recover {
      case ex: Exception => logAndShowError(isAgent, s"[handleUpdateSuccess]: Error updating reporting method: ${ex.getMessage}")
    }
  }

  private def getReportingMethod(reportingMethod: String): Option[String] = {
    Set("annual", "quarterly")
      .find(_ == reportingMethod.toLowerCase)
  }

  private def getIncomeSourceId(maybeSoleTraderBusinessId: Option[String],
                                incomeSourceJourney: IncomeSourceJourney)
                               (implicit user: MtdItUser[_]): Option[String] = {
    incomeSourceJourney match {
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
                               incomeSourceJourney: IncomeSourceJourney): (Call, Call, Call) = {

    (isAgent, incomeSourceJourney) match {
      case (false, UkProperty) =>
        (
          manageIncomeSourceDetailsController.showUkProperty,
          confirmReportingMethodSharedController.submitUKProperty(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear),
          manageObligationsController.showUKProperty(changeTo = changeTo, taxYear = taxYear)
        )
      case (false, ForeignProperty) =>
        (
          manageIncomeSourceDetailsController.showForeignProperty,
          confirmReportingMethodSharedController.submitForeignProperty(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear),
          manageObligationsController.showForeignProperty(changeTo = changeTo, taxYear = taxYear)
        )
      case (false, SelfEmployment) =>
        (
          manageIncomeSourceDetailsController.showSoleTraderBusiness(id = incomeSourceId),
          confirmReportingMethodSharedController.submitSoleTraderBusiness(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear),
          manageObligationsController.showSelfEmployment(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear)
        )
      case (true, UkProperty) =>
        (
          manageIncomeSourceDetailsController.showUkPropertyAgent,
          confirmReportingMethodSharedController.submitUKPropertyAgent(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear),
          manageObligationsController.showAgentUKProperty(changeTo = changeTo, taxYear = taxYear)
        )
      case (true, ForeignProperty) =>
        (
          manageIncomeSourceDetailsController.showForeignPropertyAgent,
          confirmReportingMethodSharedController.submitForeignPropertyAgent(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear),
          manageObligationsController.showAgentForeignProperty(changeTo = changeTo, taxYear = taxYear)
        )
      case (true, SelfEmployment) =>
        (
          manageIncomeSourceDetailsController.showSoleTraderBusinessAgent(id = incomeSourceId),
          confirmReportingMethodSharedController.submitSoleTraderBusinessAgent(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear),
          manageObligationsController.showAgentSelfEmployment(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear)
        )
    }
  }

  private lazy val manageObligationsController = controllers.incomeSources.manage.routes
    .ManageObligationsController

  private lazy val confirmReportingMethodSharedController = controllers.incomeSources.manage.routes
    .ConfirmReportingMethodSharedController

  private lazy val manageIncomeSourceDetailsController = controllers.incomeSources.manage.routes
    .ManageIncomeSourceDetailsController


}
