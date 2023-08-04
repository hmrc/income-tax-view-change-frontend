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
import models.incomeSourceDetails.TaxYear
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

  def showSoleTraderBusiness(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = Some(id),
      incomeSourceType = SoleTraderBusiness
    )
  }

  def showSoleTraderBusinessAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = Some(id),
      incomeSourceType = SoleTraderBusiness
    )
  }

  def submitSoleTraderBusiness(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = id,
      incomeSourceType = SoleTraderBusiness
    )
  }

  def submitSoleTraderBusinessAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = id,
      incomeSourceType = SoleTraderBusiness
    )
  }

  def showUKProperty(taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = None,
      incomeSourceType = UKProperty
    )
  }

  def showUKPropertyAgent(taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = None,
      incomeSourceType = UKProperty
    )
  }

  def submitUKProperty(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceType = UKProperty,
      incomeSourceId = id
    )
  }

  def submitUKPropertyAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceType = UKProperty,
      incomeSourceId = id
    )
  }

  def showForeignProperty(taxYear: String, changeTo: String): Action[AnyContent] = {
    show(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = None,
      incomeSourceType = ForeignProperty
    )
  }

  def showForeignPropertyAgent(taxYear: String, changeTo: String): Action[AnyContent] = {
    showAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = None,
      incomeSourceType = ForeignProperty
    )
  }

  def submitForeignProperty(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submit(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = id,
      incomeSourceType = ForeignProperty
    )
  }

  def submitForeignPropertyAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = {
    submitAgent(
      taxYear = taxYear,
      changeTo = changeTo,
      incomeSourceId = id,
      incomeSourceType = ForeignProperty
    )
  }

  private def show(incomeSourceId: Option[String],
                   incomeSourceType: IncomeSourceType,
                   taxYear: String,
                   changeTo: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          taxYear = taxYear,
          changeTo = changeTo,
          itvcErrorHandler = itvcErrorHandler,
          incomeSourceType = incomeSourceType,
          soleTraderBusinessId = incomeSourceId
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
              soleTraderBusinessId = incomeSourceId,
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              incomeSourceType = incomeSourceType,
              itvcErrorHandler = itvcErrorHandlerAgent
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
          isAgent = false,
          taxYear = taxYear,
          changeTo = changeTo,
          incomeSourceId = incomeSourceId,
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
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              incomeSourceId = incomeSourceId,
              incomeSourceType = incomeSourceType,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  private def handleRequest(soleTraderBusinessId: Option[String],
                            isAgent: Boolean,
                            taxYear: String,
                            changeTo: String,
                            incomeSourceType: IncomeSourceType,
                            itvcErrorHandler: ShowInternalServerError)
                           (implicit user: MtdItUser[_]): Future[Result] = {

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)

    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearStartYearEndYear(taxYear)

    val maybeIncomeSourceId: Option[String] = getIncomeSourceId(soleTraderBusinessId, incomeSourceType)

    Future(
      (isEnabled(IncomeSources), maybeTaxYearModel, newReportingMethod, maybeIncomeSourceId) match {
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
        case (_, Some(taxYearModel), Some(reportingMethod), Some(id)) =>
          getRedirectCalls(
            incomeSourceId = id,
            isAgent = isAgent,
            taxYear = taxYear,
            changeTo = changeTo,
            incomeSourceType = incomeSourceType
          ) match {
            case (backCall, postAction, _) =>
              Ok(
                confirmReportingMethod(
                  isAgent = isAgent,
                  backUrl = backCall.url,
                  postAction = postAction,
                  reportingMethod = reportingMethod,
                  taxYearEndYear = taxYearModel.endYear.toString,
                  form = ConfirmReportingMethodForm.form,
                  taxYearStartYear = taxYearModel.startYear.toString
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

  private def handleSubmitRequest(taxYear: String,
                                  isAgent: Boolean,
                                  changeTo: String,
                                  incomeSourceId: String,
                                  incomeSourceType: IncomeSourceType,
                                  itvcErrorHandler: ShowInternalServerError)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    val newReportingMethod: Option[String] = getReportingMethod(changeTo)

    val maybeTaxYearModel: Option[TaxYear] = TaxYear.getTaxYearStartYearEndYear(taxYear)

    val redirectCalls: (Call, Call, Call) = getRedirectCalls(incomeSourceId, taxYear, isAgent, changeTo, incomeSourceType)

    (isEnabled(IncomeSources), maybeTaxYearModel, newReportingMethod, redirectCalls) match {
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
                  isAgent = isAgent,
                  form = formWithErrors,
                  backUrl = backCall.url,
                  postAction = postAction,
                  taxYearEndYear = taxYears.endYear.toString,
                  reportingMethod = reportingMethod,
                  taxYearStartYear = taxYears.startYear.toString
                )
              )
            ),
          _ =>
            updateIncomeSourceService.updateTaxYearSpecific(
              nino = user.nino,
              incomeSourceId = incomeSourceId,
              taxYearSpecific = List(
                TaxYearSpecific(
                  taxYear = taxYears.endYear.toString,
                  latencyIndicator = reportingMethod match {
                    case "annual" => true
                    case "quarterly" => false
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

  private def getRedirectCalls(incomeSourceId: String,
                               taxYear: String,
                               isAgent: Boolean,
                               changeTo: String,
                               incomeSourceType: IncomeSourceType): (Call, Call, Call) = {

    (isAgent, incomeSourceType) match {
      case (false, UKProperty) =>
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
      case (false, SoleTraderBusiness) =>
        (
          manageIncomeSourceDetailsController.showSoleTraderBusiness(id = incomeSourceId),
          confirmReportingMethodSharedController.submitSoleTraderBusiness(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear),
          manageObligationsController.showSelfEmployment(id = incomeSourceId, changeTo = changeTo, taxYear = taxYear)
        )
      case (true, UKProperty) =>
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
      case (true, SoleTraderBusiness) =>
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

  private sealed trait IncomeSourceType
  private case object UKProperty extends IncomeSourceType
  private case object ForeignProperty extends IncomeSourceType
  private case object SoleTraderBusiness extends IncomeSourceType
}
