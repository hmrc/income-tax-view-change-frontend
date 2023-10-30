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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.SelfEmployment
import forms.incomeSources.add.AddBusinessReportingMethodForm
import models.incomeSourceDetails.LatencyDetails
import models.incomeSourceDetails.viewmodels.BusinessReportingMethodViewModel
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.BusinessReportingMethod

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessReportingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                  val authorisedFunctions: FrontendAuthorisedFunctions,
                                                  val checkSessionTimeout: SessionTimeoutPredicate,
                                                  val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                  val retrieveBtaNavBar: NavBarPredicate,
                                                  val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                  val retrieveNino: NinoPredicate,
                                                  val view: BusinessReportingMethod,
                                                  val updateIncomeSourceService: UpdateIncomeSourceService,
                                                  val itsaStatusService: ITSAStatusService,
                                                  val dateService: DateService,
                                                  val calculationListService: CalculationListService,
                                                  val customNotFoundErrorView: CustomNotFoundError)
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

  private def getBusinessReportingMethodDetails(incomeSourceId: String)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessReportingMethodViewModel]] = {
    val latencyDetails: Option[LatencyDetails] = user.incomeSources.businesses.find(_.incomeSourceId.equals(incomeSourceId)).flatMap(_.latencyDetails)
    latencyDetails match {
      case Some(latencyValue) =>
        val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
        latencyValue match {
          case LatencyDetails(_, _, _, taxYear2, _) if taxYear2.toInt < currentTaxYearEnd => Future.successful(None)
          case LatencyDetails(_, taxYear1, taxYear1LatencyIndicator, taxYear2, taxYear2LatencyIndicator) =>
            calculationListService.isTaxYearCrystallised(taxYear1.toInt, isEnabled(TimeMachineAddYear)).flatMap {
              case Some(true) =>
                Future.successful(Some(BusinessReportingMethodViewModel(None, None, Some(taxYear2), Some(taxYear2LatencyIndicator))))
              case _ =>
                Future.successful(Some(BusinessReportingMethodViewModel(Some(taxYear1), Some(taxYear1LatencyIndicator), Some(taxYear2), Some(taxYear2LatencyIndicator))))
            }
        }
      case None =>
        Logger("application").info(s"[BusinessReportingMethodService][getBusinessReportingMethodDetails]: Latency details not available")
        Future.successful(None)
    }

  }

  private def handleRequest(isAgent: Boolean, id: String)
                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.BusinessReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.BusinessReportingMethodController.submit(id)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(id, SelfEmployment) else
      controllers.incomeSources.add.routes.IncomeSourceAddedController.show(id, SelfEmployment)

    withIncomeSourcesFS {
      itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap {
        case true =>
          getBusinessReportingMethodDetails(id).map {
            case Some(viewModel) =>
              Ok(view(
                addBusinessReportingMethodForm = AddBusinessReportingMethodForm.form,
                businessReportingViewModel = viewModel,
                postAction = postAction,
                isAgent = isAgent)(user, messages))
            case None =>
              Redirect(redirectUrl)
          }

        case false => Future.successful(Redirect(redirectUrl))
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  private def handleSubmitRequest(isAgent: Boolean, id: String)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      AddBusinessReportingMethodForm.form.bindFromRequest().fold(
        formWithErrors => handleFormErrors(formWithErrors, id, isAgent),
        valid => handleFormData(valid, id, isAgent))
    }
  }

  private def handleFormErrors(formWithErrors: Form[AddBusinessReportingMethodForm], id: String, isAgent: Boolean)
                              (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(id = id, incomeSourceType = SelfEmployment) else
      routes.IncomeSourceReportingMethodNotSavedController.show(id = id, incomeSourceType = SelfEmployment)
    val submitUrl: Call = if (isAgent) controllers.incomeSources.add.routes.BusinessReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.BusinessReportingMethodController.submit(id)

    val updatedForm = AddBusinessReportingMethodForm.updateErrorMessagesWithValues(formWithErrors)
    getBusinessReportingMethodDetails(id).map {
      case Some(viewModel) =>
        BadRequest(view(
          addBusinessReportingMethodForm = updatedForm,
          businessReportingViewModel = viewModel,
          postAction = submitUrl,
          isAgent = isAgent))
      case None => Redirect(redirectErrorUrl)
    }
  }

  private def handleFormData(form: AddBusinessReportingMethodForm, id: String, isAgent: Boolean)
                            (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val redirectUrl: Call = if (isAgent) routes.IncomeSourceAddedController.showAgent(id, SelfEmployment) else routes.IncomeSourceAddedController.show(id, SelfEmployment)
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

  private def updateReportingMethod(isAgent: Boolean, id: String, newReportingMethods: Seq[TaxYearSpecific])
                                   (implicit user: MtdItUser[_]): Future[Result] = {
    val redirectUrl: Call = if (isAgent) routes.IncomeSourceAddedController.showAgent(id, SelfEmployment) else
      routes.IncomeSourceAddedController.show(id = id, incomeSourceType = SelfEmployment)
    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(id = id, SelfEmployment) else
      routes.IncomeSourceReportingMethodNotSavedController.show(id = id, incomeSourceType = SelfEmployment)

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
          Redirect(redirectErrorUrl)
        case _ =>
          Logger("application").error(s"[BusinessReportingMethodController][updateReportingMethod]: " +
            s"Error updating tax year specific reporting method")
          Redirect(redirectErrorUrl)
      }
    }
  }

  def show(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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

  def submit(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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
