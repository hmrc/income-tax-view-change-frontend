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
import config.featureswitch.{FeatureSwitching, IncomeSources, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.add.AddUKPropertyReportingMethodForm
import models.incomeSourceDetails.LatencyDetails
import models.incomeSourceDetails.viewmodels.UKPropertyReportingMethodViewModel
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
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
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private def annualQuarterlyToBoolean(method: Option[String]): Option[Boolean] = method match {
    case Some("A") => Some(true)
    case Some("Q") => Some(false)
    case _ => None
  }
  private def getUKPropertyReportingMethodDetails(incomeSourceId: String)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UKPropertyReportingMethodViewModel]] = {
    val latencyDetails: Option[LatencyDetails] = user.incomeSources.properties.filter(_.isUkProperty).find(_.incomeSourceId.getOrElse("").equals(incomeSourceId)).flatMap(_.latencyDetails)
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
                Future.successful(Some(UKPropertyReportingMethodViewModel(Some(taxYear1), Some(taxYear1LatencyIndicator), Some(taxYear2), Some(taxYear2LatencyIndicator))))
            }
        }
      case None =>
        Logger("application").info(s"[UKPropertyReportingMethodController][getUKPropertyReportingMethodDetails] latency details not available")
        Future.successful(None)
    }

  }
  private def handleRequest(isAgent: Boolean, id: String)
                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submit(id)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyAddedController.showAgent(id) else
      controllers.incomeSources.add.routes.UKPropertyAddedController.show(id)

    if (incomeSourcesEnabled) {
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

    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting UKPropertyReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  private def handleSubmitRequest(isAgent: Boolean, id: String)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) routes.UKPropertyAddedController.showAgent(id) else routes.UKPropertyAddedController.show(id)
    val redirectErrorUrl: Call = if (isAgent) routes.UKPropertyReportingMethodErrorController.showAgent() else routes.UKPropertyReportingMethodErrorController.show()
    val submitUrl: Call = if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.submit(id)

    if (incomeSourcesEnabled) {
      AddUKPropertyReportingMethodForm.form.bindFromRequest().fold(
        hasErrors => {
          val updatedForm = AddUKPropertyReportingMethodForm.updateErrorMessagesWithValues(hasErrors)
          getUKPropertyReportingMethodDetails(id).map {
            case Some(viewModel) =>
              BadRequest(view(
                addUKPropertyReportingMethodForm = updatedForm,
                ukPropertyReportingViewModel = viewModel,
                postAction = submitUrl,
                isAgent = isAgent))
            case None => Redirect(redirectUrl)
          }
        },
        valid => {
          val taxYear1ReportingMethod = valid.taxYear1ReportingMethod
          val taxYear2ReportingMethod = valid.taxYear2ReportingMethod
          val newTaxYear1ReportingMethod = valid.newTaxYear1ReportingMethod
          val newTaxYear2ReportingMethod = valid.newTaxYear2ReportingMethod

          if (taxYear1ReportingMethod != newTaxYear1ReportingMethod || taxYear2ReportingMethod != newTaxYear2ReportingMethod) {
            val taxYearSpecific1 = newTaxYear1ReportingMethod match {
              case Some(s) => Some(TaxYearSpecific(valid.taxYear1.get, annualQuarterlyToBoolean(Some(s)).get))
              case _ => None
            }
            val taxYearSpecific2 = newTaxYear2ReportingMethod match {
              case Some(s) => Some(TaxYearSpecific(valid.taxYear2.get, annualQuarterlyToBoolean(Some(s)).get))
              case _ => None
            }
            updateIncomeSourceService.updateTaxYearSpecific(user.nino, id, List(taxYearSpecific1, taxYearSpecific2).flatten).map {
              case res: UpdateIncomeSourceResponseModel =>
                Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updated tax year specific reporting method : $res")
                Redirect(redirectUrl)
              case err: UpdateIncomeSourceResponseError =>
                Logger("application").error(s"${if (isAgent) "[Agent]"}" + s" Failed to Updated tax year specific reporting method : $err")
                Redirect(redirectErrorUrl)
            }
          } else {
            Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updating the tax year specific reporting method not required.")
            Future(Redirect(redirectUrl))
          }
        })
    } else {
      Future.successful(Ok(customNotFoundErrorView()))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"Error getting UKPropertyReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
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