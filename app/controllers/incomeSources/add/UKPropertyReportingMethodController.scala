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

package controllers.incomeSources.add

import audit.AuditingService
import audit.models.IncomeSourceReportingMethodAuditModel
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.UkProperty
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.AddUKPropertyReportingMethodForm
import models.incomeSourceDetails.{AddIncomeSourceData, LatencyDetails}
import models.incomeSourceDetails.viewmodels.UKPropertyReportingMethodViewModel
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.UKPropertyReportingMethod

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UKPropertyReportingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                    val authorisedFunctions: FrontendAuthorisedFunctions,
                                                    val checkSessionTimeout: SessionTimeoutPredicate,
                                                    val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                    val retrieveBtaNavBar: NavBarPredicate,
                                                    val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                    val view: UKPropertyReportingMethod,
                                                    val updateIncomeSourceService: UpdateIncomeSourceService,
                                                    val itsaStatusService: ITSAStatusService,
                                                    val dateService: DateService,
                                                    val calculationListService: CalculationListService,
                                                    val auditingService: AuditingService,
                                                    val customNotFoundErrorView: CustomNotFoundError,
                                                    val sessionService: SessionService)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                                   )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  private def annualQuarterlyToBoolean(method: Option[String]): Boolean = method match {
    case Some("A") => true
    case _ => false
  }

  private def getUKPropertyReportingMethodDetails(incomeSourceId: String)
                                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext)
  : Future[Option[UKPropertyReportingMethodViewModel]] = {
    val latencyDetails: Option[LatencyDetails] = user.incomeSources.properties
      .filter(_.isUkProperty).find(_.incomeSourceId.equals(incomeSourceId)).flatMap(_.latencyDetails)
    latencyDetails match {
      case Some(latencyValue) =>
        val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
        latencyValue match {
          case LatencyDetails(_, _, _, taxYear2, _) if taxYear2.toInt < currentTaxYearEnd => Future.successful(None)
          case LatencyDetails(_, taxYear1, taxYear1LatencyIndicator, taxYear2, taxYear2LatencyIndicator) =>
            calculationListService.isTaxYearCrystallised(taxYear1.toInt).flatMap {
              case Some(true) =>
                Future.successful(Some(UKPropertyReportingMethodViewModel(None, None, Some(taxYear2), Some(taxYear2LatencyIndicator))))
              case _ =>
                Future.successful(Some(UKPropertyReportingMethodViewModel(Some(taxYear1),
                  Some(taxYear1LatencyIndicator), Some(taxYear2), Some(taxYear2LatencyIndicator))))
            }
        }
      case None =>
        Logger("application").info(s"[UKPropertyReportingMethodController][getUKPropertyReportingMethodDetails]: Latency details not available")
        Future.successful(None)
    }

  }

  private def handleRequest(isAgent: Boolean, id: String)
                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submit(id)
    val cannotGoBackRedirect = if (isAgent) controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(UkProperty) else
      controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(UkProperty)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(id, UkProperty) else
      controllers.incomeSources.add.routes.IncomeSourceAddedController.show(id, UkProperty)

    withIncomeSourcesFS {
      sessionService.getMongoKeyTyped[Boolean](AddIncomeSourceData.hasBeenAddedField, JourneyType(Add, UkProperty)).flatMap {
        case Left(ex) => Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting hasBeenAdded field from session: ${ex.getMessage}")
          Future.successful(errorHandler.showInternalServerError())
        case Right(hasBeenAdded) => hasBeenAdded match {
          case Some(true) => Future.successful(Redirect(cannotGoBackRedirect))
          case _ =>
            itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap {
              case true =>
                getUKPropertyReportingMethodDetails(id).map {
                  case Some(viewModel) =>
                    Ok(view(
                      addUKPropertyReportingMethodForm = AddUKPropertyReportingMethodForm.form,
                      ukPropertyReportingViewModel = viewModel,
                      postAction = postAction,
                      isAgent = isAgent)(user, messages))
                  case None =>
                    Redirect(redirectUrl)
                }

              case false => Future.successful(Redirect(redirectUrl))
            }
        }
      }.recover {
        case ex: Exception =>
          Logger("application").error(
            s"[UKPropertyReportingMethodController][handleRequest]: Error getting UKPropertyReportingMethodController page: ${ex.getMessage}")
          errorHandler.showInternalServerError()
      }
    }
  }

  private def handleSubmitRequest(isAgent: Boolean, id: String)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    withIncomeSourcesFS {
      AddUKPropertyReportingMethodForm.form.bindFromRequest().fold(
        hasErrors => handleFormErrors(hasErrors, id, isAgent),
        valid => handleFormData(valid, id, isAgent)
      ).recover {
        case ex: Exception =>
          Logger("application").error(s"[UKPropertyReportingMethodController][handleSubmitRequest]:" +
            s"Error getting UKPropertyReportingMethodController page: ${ex.getMessage}")
          errorHandler.showInternalServerError()
      }
    }
  }

  private def handleFormErrors(errors: Form[AddUKPropertyReportingMethodForm], id: String, isAgent: Boolean)
                              (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    val updatedForm = AddUKPropertyReportingMethodForm.updateErrorMessagesWithValues(errors)
    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceNotAddedController.showAgent(UkProperty) else
      routes.IncomeSourceNotAddedController.show(UkProperty)
    val submitUrl: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submit(id)

    getUKPropertyReportingMethodDetails(id).map {
      case Some(viewModel) =>
        BadRequest(view(
          addUKPropertyReportingMethodForm = updatedForm,
          ukPropertyReportingViewModel = viewModel,
          postAction = submitUrl,
          isAgent = isAgent))
      case None => Redirect(redirectErrorUrl)
    }
  }

  private def handleFormData(form: AddUKPropertyReportingMethodForm, id: String, isAgent: Boolean)
                            (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val redirectUrl: Call = if (isAgent) routes.IncomeSourceAddedController.showAgent(id, UkProperty) else routes.IncomeSourceAddedController.show(id, UkProperty)
    val reportingMethodNeedsUpdating = form.taxYear1ReportingMethod != form.newTaxYear1ReportingMethod ||
      form.taxYear2ReportingMethod != form.newTaxYear2ReportingMethod

    if (reportingMethodNeedsUpdating) {
      val newReportingMethods: Seq[TaxYearSpecific] = Seq(
        getSelectedReportingMethodValues(form.taxYear1ReportingMethod, form.newTaxYear1ReportingMethod, form.taxYear1),
        getSelectedReportingMethodValues(form.taxYear2ReportingMethod, form.newTaxYear2ReportingMethod, form.taxYear2)
      ).flatten

      updateReportingMethod(isAgent, id, newReportingMethods)

    } else {
      Future.successful(Redirect(redirectUrl))
    }
  }

  private def getSelectedReportingMethodValues(existingMethod: Option[String], newMethod: Option[String], taxYear: Option[String]): Option[TaxYearSpecific] = {
    (existingMethod, newMethod, taxYear) match {
      case (Some(existing), Some(newMethod), Some(taxYear)) if existing != newMethod =>
        Some(TaxYearSpecific(taxYear, annualQuarterlyToBoolean(Some(newMethod))))
      case _ =>
        None
    }
  }

  private def formatReportingMethodPeriod(latencyIndicator: Boolean): String = {
    latencyIndicator match {
      case true => "Annually"
      case false => "Quarterly"
    }
  }

  private def addAudit(isSuccessful: Boolean, newReportingMethods: Seq[TaxYearSpecific])(implicit user: MtdItUser[_]): Unit = {
    for (taxYear <- newReportingMethods) {
      auditingService
        .extendedAudit(
          IncomeSourceReportingMethodAuditModel(
            isSuccessful = isSuccessful,
            journeyType = UkProperty.journeyType,
            operationType = "ADD",
            reportingMethodChangeTo = formatReportingMethodPeriod(taxYear.latencyIndicator),
            taxYear = (taxYear.taxYear.toInt - 1).toString + "-" + taxYear.taxYear,
            businessName = "UK property"
          )
        )
    }
  }

  private def updateReportingMethod(isAgent: Boolean, id: String, newReportingMethods: Seq[TaxYearSpecific])
                                   (implicit user: MtdItUser[_]): Future[Result] = {

    val redirectUrl: Call = if (isAgent) routes.IncomeSourceAddedController.showAgent(id, UkProperty) else routes.IncomeSourceAddedController.show(id, UkProperty)
    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(id = id, incomeSourceType = UkProperty) else
      routes.IncomeSourceReportingMethodNotSavedController.show(id = id, incomeSourceType = UkProperty)

    for {
      results <- Future.sequence(newReportingMethods.map(taxYearSpecific =>
        updateIncomeSourceService.updateTaxYearSpecific(user.nino, id, taxYearSpecific))
      )
    } yield {
      val errors = results.collect {
        case error: UpdateIncomeSourceResponseError => error
      }
      val success = results.collect {
        case success: UpdateIncomeSourceResponseModel => success
      }
      (errors, success) match {
        case (es: Seq[UpdateIncomeSourceResponseError], _) if es.isEmpty =>
          Logger("application").info(s"[BusinessReportingMethodController][updateReportingMethod]: " +
            s"Updated tax year specific reporting method for all supplied tax years")
          addAudit(true, newReportingMethods)(user)
          Redirect(redirectUrl)
        case (es: Seq[UpdateIncomeSourceResponseError], ss: UpdateIncomeSourceResponseModel) =>
          for (success <- ss) {
            Logger("application").info(s"[BusinessReportingMethodController][updateReportingMethod]: " +
              s"Updated tax year specific reporting method for $success")
          }
          for (error <- es) {
            Logger("application").error(s"[BusinessReportingMethodController][updateReportingMethod]: " +
              s"Error updating specific reporting method: $error")
          }
          addAudit(false, newReportingMethods)(user)
          Redirect(redirectErrorUrl)
        case _ =>
          Logger("application").error(s"[BusinessReportingMethodController][updateReportingMethod]: " +
            s"Error updating tax year specific reporting method")
          addAudit(false, newReportingMethods)(user)
          Redirect(redirectErrorUrl)
      }
    }
  }

  def show(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user => handleRequest(isAgent = false, id = id)
  }

  def showAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(isAgent = true, id = id)
        }
  }

  def submit(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false, id = id)
  }

  def submitAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser => handleSubmitRequest(isAgent = true, id = id)
        }
  }

}