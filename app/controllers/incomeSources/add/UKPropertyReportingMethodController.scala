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
import cats.data.OptionT
import cats.implicits._
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.add.AddUKPropertyReportingMethodForm
import models.incomeSourceDetails.LatencyDetails
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
                                                    val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                    val retrieveNino: NinoPredicate,
                                                    val view: UKPropertyReportingMethod,
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

  private def getUKPropertyReportingMethodDetails(incomeSourceId: String)
                                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext)
  : Future[Option[UKPropertyReportingMethodViewModel]] = {
    val latencyDetails: Option[LatencyDetails] = user.incomeSources.properties
      .filter(_.isUkProperty).find(_.incomeSourceId.equals(incomeSourceId)).flatMap(_.latencyDetails)
    latencyDetails match {
      case Some(x) =>
        val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
        x match {
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
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyAddedController.showAgent(id) else
      controllers.incomeSources.add.routes.UKPropertyAddedController.show(id)

    withIncomeSourcesFS {
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
    }.recover {
      case ex: Exception =>
        Logger("application").error(
          s"[UKPropertyReportingMethodController][handleRequest]: Error getting UKPropertyReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
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
    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceNotAddedController.showUKPropertyAgent() else
      routes.IncomeSourceNotAddedController.showUKProperty()
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
    val redirectUrl: Call = if (isAgent) routes.UKPropertyAddedController.showAgent(id) else routes.UKPropertyAddedController.show(id)
    val reportingMethodNeedsUpdating = form.taxYear1ReportingMethod != form.newTaxYear1ReportingMethod ||
      form.taxYear2ReportingMethod != form.newTaxYear2ReportingMethod

    if (reportingMethodNeedsUpdating) {
      val taxYearSpecific1Opt = for {
        newReportingMethod <- form.newTaxYear1ReportingMethod
        taxYear <- form.taxYear1
      } yield TaxYearSpecific(taxYear, annualQuarterlyToBoolean(Some(newReportingMethod)))

      val taxYearSpecific2Opt = for {
        newReportingMethod <- form.newTaxYear2ReportingMethod
        taxYear <- form.taxYear2
      } yield TaxYearSpecific(taxYear, annualQuarterlyToBoolean(Some(newReportingMethod)))

      updateReportingMethod(isAgent, id, taxYearSpecific1Opt, taxYearSpecific2Opt)

    } else {
      Future.successful(Redirect(redirectUrl))
    }
  }

  private def updateReportingMethod(isAgent: Boolean, id: String, taxYearSpecific1Opt: Option[TaxYearSpecific], taxYearSpecific2Opt: Option[TaxYearSpecific])
                                   (implicit user: MtdItUser[_]): Future[Result] = {

    val redirectUrl: Call = if (isAgent) routes.UKPropertyAddedController.showAgent(id) else routes.UKPropertyAddedController.show(id)
    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showUKPropertyAgent() else
      routes.IncomeSourceReportingMethodNotSavedController.showUKProperty()

    val updateResults = for {
      result1 <- OptionT.fromOption[Future](taxYearSpecific1Opt)
        .semiflatMap(taxYearSpecific => updateIncomeSourceService.updateTaxYearSpecific(user.nino, id, taxYearSpecific))
      result2 <- OptionT.fromOption[Future](taxYearSpecific2Opt)
        .semiflatMap(taxYearSpecific => updateIncomeSourceService.updateTaxYearSpecific(user.nino, id, taxYearSpecific))
    } yield (result1, result2)

    updateResults.value.flatMap {
      case Some((result1: UpdateIncomeSourceResponseModel, result2: UpdateIncomeSourceResponseModel)) =>
        Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]:" +
          s"Updated tax year specific reporting method : $result1 and $result2")
        Future.successful(Redirect(redirectUrl))
      case Some((_: UpdateIncomeSourceResponseError, _: UpdateIncomeSourceResponseModel)) =>
        //TODO: redirect to custom error page based on one failure response & one success response
        Future.successful(Redirect(redirectErrorUrl))
      case Some((_: UpdateIncomeSourceResponseError, _: UpdateIncomeSourceResponseError)) =>
        Future.successful(Redirect(redirectErrorUrl))
      case _ =>
        Logger("application").error("[UKPropertyReportingMethodController][updateReportingMethod]:" +
          "Unexpected response when updating tax year specific reporting methods")
        Future.successful(Redirect(redirectErrorUrl))
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"[UKPropertyReportingMethodController][updateReportingMethod]:" +
          s"Error updating tax year specific reporting method: ${ex.getMessage}")
        Redirect(redirectErrorUrl)
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