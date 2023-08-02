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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.manage.ConfirmReportingMethodForm
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel, TaxYear}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.mvc.{Action, _}
import services.{IncomeSourceDetailsService, UpdateIncomeSourceService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
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
                                                       val itvcErrorHandler: ItvcErrorHandler,
                                                       val customNotFoundErrorView: CustomNotFoundError,
                                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       val confirmReportingMethod: ConfirmReportingMethod,
                                                       val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                       val retrieveBtaNavBar: NavBarPredicate)
                                                      (implicit val ec: ExecutionContext,
                                                      implicit override val mcc: MessagesControllerComponents,
                                                      val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  def showSoleTraderBusiness(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      incomeSourceId = Some(incomeSourceId),
      incomeSourceType = SoleTraderBusiness,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def showSoleTraderBusinessAgent(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      incomeSourceId = Some(incomeSourceId),
      incomeSourceType = SoleTraderBusiness,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def submitSoleTraderBusiness(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      incomeSourceId = incomeSourceId,
      incomeSourceType = SoleTraderBusiness,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def submitSoleTraderBusinessAgent(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      incomeSourceId = incomeSourceId,
      incomeSourceType = SoleTraderBusiness,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def showUKProperty(taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      incomeSourceId = None,
      incomeSourceType = UKProperty,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def showUKPropertyAgent(taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      incomeSourceId = None,
      incomeSourceType = UKProperty,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def submitUKProperty(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      incomeSourceId = incomeSourceId,
      incomeSourceType = UKProperty,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def submitUKPropertyAgent(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      incomeSourceId = incomeSourceId,
      incomeSourceType = UKProperty,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def showForeignProperty(taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      incomeSourceId = None,
      incomeSourceType = ForeignProperty,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def showForeignPropertyAgent(taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      incomeSourceId = None,
      incomeSourceType = ForeignProperty,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def submitForeignProperty(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      incomeSourceId = incomeSourceId,
      incomeSourceType = ForeignProperty,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  def submitForeignPropertyAgent(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      incomeSourceId = incomeSourceId,
      incomeSourceType = ForeignProperty,
      taxYear = taxYear,
      changeTo = changeTo
    )
  }

  private def show(incomeSourceId: Option[String],
                   incomeSourceType: IncomeSourceType,
                   taxYear: String,
                   changeTo: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        id = incomeSourceId,
        isAgent = false,
        taxYear = taxYear,
        changeTo = changeTo,
        itvcErrorHandler = itvcErrorHandler,
        incomeSourceType = incomeSourceType
      )
  }

  private def showAgent(incomeSourceId: Option[String],
                        incomeSourceType: IncomeSourceType,
                        taxYear: String,
                        changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              id = incomeSourceId,
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              itvcErrorHandler = itvcErrorHandlerAgent,
              incomeSourceType = incomeSourceType,
            )
        }
  }

  private def submit(incomeSourceId: String,
                     incomeSourceType: IncomeSourceType,
                     taxYear: String,
                     changeTo: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleSubmitRequest(
          id = incomeSourceId,
          isAgent = false,
          taxYear = taxYear,
          changeTo = changeTo,
          incomeSourceType = incomeSourceType,
          itvcErrorHandler = itvcErrorHandler
        )
    }

  private def submitAgent(incomeSourceId: String,
                          incomeSourceType: IncomeSourceType,
                          taxYear: String,
                          changeTo: String) = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              id = incomeSourceId,
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              incomeSourceType = incomeSourceType,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  private def handleRequest(id: Option[String],
                            isAgent: Boolean,
                            taxYear: String,
                            changeTo: String,
                            incomeSourceType: IncomeSourceType,
                            itvcErrorHandler: ShowInternalServerError)
                           (implicit user: MtdItUser[_]): Future[Result] = {
    Future(
      (isEnabled(IncomeSources), TaxYear.getTaxYearStartYearEndYear(taxYear), getReportingMethod(changeTo), getIncomeSourceId(id, incomeSourceType)) match {
        case (false, _, _, _) =>
          Ok(
            customNotFoundErrorView()
          )
        case (_, None, _, _) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: " +
              s"Could not parse taxYear: $taxYear")
          itvcErrorHandler.showInternalServerError()
        case (_, _, None, _) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: " +
              s"Could not parse reporting method: $changeTo")
          itvcErrorHandler.showInternalServerError()
        case (_, _, _, None) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: " +
              s"Could not find incomeSourceId")
          itvcErrorHandler.showInternalServerError()
        case (_, Some(taxYears), Some(reportingMethod), Some(id)) =>
          getRedirectCalls(
            id = id,
            incomeSourceType = incomeSourceType,
            isAgent = isAgent,
            changeTo = changeTo,
            taxYear = taxYear
          ) match {
            case (backCall, postAction, _) =>
              Ok(
                confirmReportingMethod(
                  form = ConfirmReportingMethodForm.form,
                  backUrl = backCall.url,
                  postAction = postAction,
                  isAgent = isAgent,
                  taxYearEndYear = taxYears.endYear,
                  taxYearStartYear = taxYears.startYear,
                  reportingMethod = reportingMethod
                )
              )
          }
      }
    ) recover {
      case ex: Exception =>
        Logger("application").error(s"[ConfirmReportingMethodSharedController][handleRequest]: " +
          s"Error getting confirmReportingMethod page: ${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def handleSubmitRequest(id: String,
                                  isAgent: Boolean,
                                  taxYear: String,
                                  changeTo: String,
                                  incomeSourceType: IncomeSourceType,
                                  itvcErrorHandler: ShowInternalServerError)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    (isEnabled(IncomeSources), TaxYear.getTaxYearStartYearEndYear(taxYear), getReportingMethod(changeTo), getRedirectCalls(id, incomeSourceType, isAgent, changeTo, taxYear)) match {
      case (false, _, _, _) =>
        Future(
          Ok(
            customNotFoundErrorView()
          )
        )
      case (_, None, _, _) =>
        Logger("application")
          .error(s"[ConfirmReportingMethodSharedController][handleSubmitRequest]: " +
            s"Could not parse taxYear: $taxYear")
        Future(
          itvcErrorHandler.showInternalServerError()
        )
      case (_, _, None, _) =>
        Logger("application")
          .error(s"[ConfirmReportingMethodSharedController][handleSubmitRequest]: " +
            s"Could not parse reporting method: $changeTo")
        Future(
          itvcErrorHandler.showInternalServerError()
        )
      case (_, Some(taxYears), Some(reportingMethod), (backCall, postAction, successCall)) =>
        ConfirmReportingMethodForm.form.bindFromRequest().fold(
          formWithErrors =>
            Future(
              BadRequest(
                confirmReportingMethod(
                  form = formWithErrors,
                  backUrl = backCall.url,
                  postAction = postAction,
                  isAgent = isAgent,
                  taxYearStartYear = taxYears.startYear,
                  taxYearEndYear = taxYears.endYear,
                  reportingMethod = reportingMethod
                )
              )
            ),
          _ =>
            updateIncomeSourceService.updateTaxYearSpecific(
              nino = user.nino,
              incomeSourceId = id,
              taxYearSpecific = List(
                TaxYearSpecific(
                  taxYear = taxYears.endYear,
                  latencyIndicator = reportingMethod match {
                    case "annual" => true
                    case _ => false
                  }
                )
              )
            ) flatMap {
              case err: UpdateIncomeSourceResponseError =>
                Logger("application")
                  .error(s"[ConfirmReportingMethodSharedController][handleSubmitRequest]: " +
                    s"Failed to Update tax year specific reporting method: $err")
                Future(
                  itvcErrorHandler.showInternalServerError()
                )
              case res: UpdateIncomeSourceResponseModel =>
                Logger("application")
                  .info(s"[ConfirmReportingMethodSharedController][handleSubmitRequest]: " +
                    s"Updated tax year specific reporting method : $res")
                Future.successful(
                  Redirect(
                    successCall
                  )
                )
            } recover {
              case ex: Exception =>
                Logger("application")
                  .error(s"[ConfirmReportingMethodSharedController][handleSubmitRequest]: " +
                    s"Error updating reporting method: ${ex.getMessage}")
                itvcErrorHandler.showInternalServerError()
            }
        )
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
      case SoleTraderBusiness =>
        user.incomeSources.businesses
          .find(business => !business.isCeased && business.incomeSourceId == maybeSoleTraderBusinessId)
          .flatMap(_.incomeSourceId)
      case UKProperty =>
        user.incomeSources.properties
          .find(property => property.isUkProperty && !property.isCeased)
          .flatMap(_.incomeSourceId)
      case ForeignProperty =>
        user.incomeSources.properties
          .find(property => property.isForeignProperty && !property.isCeased)
          .flatMap(_.incomeSourceId)
    }
  }

  private def getRedirectCalls(id: String,
                               incomeSourceType: IncomeSourceType,
                               isAgent: Boolean,
                               changeTo: String,
                               taxYear: String): (Call, Call, Call) = {
    (isAgent, incomeSourceType) match {
      case (false, UKProperty) =>
        (
          manageIncomeSourceDetailsController.showUkProperty,
          confirmReportingMethodSharedController.submitUKProperty(id, taxYear, changeTo),
          manageObligationsController.showUKProperty(changeTo, taxYear)
        )
      case (false, ForeignProperty) =>
        (
          manageIncomeSourceDetailsController.showForeignProperty,
          confirmReportingMethodSharedController.submitForeignProperty(id, taxYear, changeTo),
          manageObligationsController.showForeignProperty(changeTo, taxYear)
        )
      case (false, SoleTraderBusiness) =>
        (
          manageIncomeSourceDetailsController.showSoleTraderBusiness(id),
          confirmReportingMethodSharedController.submitSoleTraderBusiness(id, taxYear, changeTo),
          manageObligationsController.showSelfEmployment(id, changeTo, taxYear)
        )
      case (true, UKProperty) =>
        (
          manageIncomeSourceDetailsController.showUkPropertyAgent,
          confirmReportingMethodSharedController.submitUKPropertyAgent(id, taxYear, changeTo),
          manageObligationsController.showAgentUKProperty(changeTo, taxYear)
        )
      case (true, ForeignProperty) =>
        (
          manageIncomeSourceDetailsController.showForeignPropertyAgent,
          confirmReportingMethodSharedController.submitForeignPropertyAgent(id, taxYear, changeTo),
          manageObligationsController.showAgentForeignProperty(changeTo, taxYear)
        )
      case (true, SoleTraderBusiness) =>
        (
          manageIncomeSourceDetailsController.showSoleTraderBusinessAgent(id),
          confirmReportingMethodSharedController.submitSoleTraderBusinessAgent(id, taxYear, changeTo),
          manageObligationsController.showAgentSelfEmployment(id, changeTo, taxYear)
        )
    }
  }

  private lazy val manageObligationsController = controllers.incomeSources.manage.routes
    .ManageObligationsController

  private lazy val confirmReportingMethodSharedController = controllers.incomeSources.manage.routes
    .ConfirmReportingMethodSharedController

  private lazy val manageIncomeSourceDetailsController = controllers.incomeSources.manage.routes
    .ManageIncomeSourceDetailsController

  private sealed trait IncomeSourceType
  private case object UKProperty extends IncomeSourceType
  private case object ForeignProperty extends IncomeSourceType
  private case object SoleTraderBusiness extends IncomeSourceType
}
