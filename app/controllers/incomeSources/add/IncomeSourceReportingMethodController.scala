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
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.IncomeSourceReportingMethodForm
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels.IncomeSourceReportingMethodViewModel
import models.incomeSourceDetails.{AddIncomeSourceData, LatencyDetails, LatencyYear}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.IncomeSourceReportingMethod

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceReportingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                      val authorisedFunctions: FrontendAuthorisedFunctions,
                                                      val checkSessionTimeout: SessionTimeoutPredicate,
                                                      val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                      val retrieveBtaNavBar: NavBarPredicate,
                                                      val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                      val updateIncomeSourceService: UpdateIncomeSourceService,
                                                      val itsaStatusService: ITSAStatusService,
                                                      val dateService: DateService,
                                                      val calculationListService: CalculationListService,
                                                      val auditingService: AuditingService,
                                                      val view: IncomeSourceReportingMethod,
                                                      val sessionService: SessionService)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext,
                                                      val itvcErrorHandler: ItvcErrorHandler,
                                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                                     ) extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils with JourneyChecker {

  lazy val backUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    (isAgent, incomeSourceType.equals(SelfEmployment)) match {
      case (false, true) => routes.AddBusinessAddressController.show(false).url
      case (true, true) => routes.AddBusinessAddressController.showAgent(false).url
      case (_, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange = false, incomeSourceType).url
    }

  lazy val errorRedirectUrl: (Boolean, IncomeSourceType, String) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
    if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(id, incomeSourceType).url
    else routes.IncomeSourceReportingMethodNotSavedController.show(id, incomeSourceType).url

  lazy val redirectUrl: (Boolean, IncomeSourceType, String) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
    if (isAgent) routes.IncomeSourceAddedController.showAgent(id, incomeSourceType).url
    else routes.IncomeSourceAddedController.show(id, incomeSourceType).url

  lazy val submitUrl: (Boolean, IncomeSourceType, String) => Call = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
    routes.IncomeSourceReportingMethodController.submit(isAgent, incomeSourceType, id)


  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String): Action[AnyContent] = authenticatedAction(isAgent) {
    implicit user =>
      val incomeSourceId = mkIncomeSourceId(id)
      handleRequest(isAgent = isAgent, incomeSourceType, id = incomeSourceId)
  }

  private def authenticatedAction(isAgent: Boolean)
                                 (authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
        implicit user =>
          authenticatedCodeBlock(user)
      }
  }

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: IncomeSourceId)
                   (implicit user: MtdItUser[_]): Future[Result] = withCustomSession(JourneyType(Add, incomeSourceType)) {
    val cannotGoBackRedirect = if (isAgent) controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(incomeSourceType) else
      controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(incomeSourceType)
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    sessionService.getMongoKeyTyped[Boolean](AddIncomeSourceData.hasBeenAddedField, JourneyType(Add, incomeSourceType)).flatMap {
      case Left(ex) => Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error getting hasBeenAdded field from session: ${ex.getMessage}")
        Future.successful(errorHandler.showInternalServerError())
      case Right(hasBeenAdded) => hasBeenAdded match {
        case Some(true) => Future.successful(Redirect(cannotGoBackRedirect))
        case _ =>
          itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap {
            case true =>
              getViewModel(incomeSourceType, id).map {
                case Some(viewModel) =>
                  Ok(view(
                    incomeSourceReportingMethodForm = IncomeSourceReportingMethodForm.form,
                    incomeSourceReportingViewModel = viewModel,
                    postAction = submitUrl(isAgent, incomeSourceType, id.value),
                    isAgent = isAgent))
                case None =>
                  Redirect(redirectUrl(isAgent, incomeSourceType, id.value))
              }
            case false =>
              Future.successful {
                Redirect(redirectUrl(isAgent, incomeSourceType, id.value))
              }
          }
      }
    }.recover {
      case ex: Exception =>
        val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application").error(
          s"[UKPropertyReportingMethodController][handleRequest]:" +
            s"Unable to display IncomeSourceReportingMethod page for $incomeSourceType: ${ex.getMessage} ${ex.getCause}")
        errorHandler.showInternalServerError()
    }
  }


  private def getLatencyDetails(incomeSourceType: IncomeSourceType, incomeSourceId: String)(implicit user: MtdItUser[_]): Option[LatencyDetails] = {
    user.incomeSources.getLatencyDetails(incomeSourceType, incomeSourceId)
  }

  private def getViewModel(incomeSourceType: IncomeSourceType, incomeSourceId: IncomeSourceId)
                          (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[IncomeSourceReportingMethodViewModel]] = {
    val currentTaxYear = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
    val latencyDetails = getLatencyDetails(incomeSourceType, incomeSourceId.value)


    latencyDetails match {
      case Some(LatencyDetails(_, _, _, taxYear2, _)) if taxYear2.toInt < currentTaxYear =>
        Future.successful(None)
      case Some(LatencyDetails(_, taxYear1, taxYear1LatencyIndicator, taxYear2, taxYear2LatencyIndicator)) =>
        calculationListService.isTaxYearCrystallised(taxYear1.toInt).flatMap {
          case Some(true) =>
            Future.successful {
              Some(IncomeSourceReportingMethodViewModel(latencyYear1 = None, latencyYear2 = Some(LatencyYear(taxYear2, taxYear2LatencyIndicator))))
            }
          case _ =>
            Future.successful {
              Some(IncomeSourceReportingMethodViewModel(latencyYear1 = Some(LatencyYear(taxYear1, taxYear1LatencyIndicator)),
                latencyYear2 = Some(LatencyYear(taxYear2, taxYear2LatencyIndicator))))
            }
        }
      case _ =>
        Logger("application").info(s"[IncomeSourceReportingMethodController][getUKPropertyReportingMethodDetails]: Latency details not available")
        Future.successful(None)
    }
  }

  def submit(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String): Action[AnyContent] = authenticatedAction(isAgent) {
    implicit user =>
      val incomeSourceId = mkIncomeSourceId(id)
      handleSubmit(isAgent, incomeSourceType, incomeSourceId)
  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: IncomeSourceId)
                          (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {

    IncomeSourceReportingMethodForm.form.bindFromRequest().fold(
      invalid => handleInvalidForm(invalid, incomeSourceType, id, isAgent),
      valid => handleValidForm(valid, incomeSourceType, id, isAgent)
    ).recover {
      case ex: Exception =>
        val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application").error(s"[IncomeSourceReportingMethodController][handleSubmit]:" +
          s"Unable to handle IncomeSourceReportingMethodController submit request for $incomeSourceType: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  private def handleInvalidForm(form: Form[IncomeSourceReportingMethodForm], incomeSourceType: IncomeSourceType, id: IncomeSourceId, isAgent: Boolean)
                               (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    val updatedForm = IncomeSourceReportingMethodForm.updateErrorMessagesWithValues(form)

    getViewModel(incomeSourceType, id).map {
      case Some(viewModel) =>
        BadRequest(view(
          incomeSourceReportingMethodForm = updatedForm,
          incomeSourceReportingViewModel = viewModel,
          postAction = submitUrl(isAgent, incomeSourceType, id.value),
          isAgent = isAgent))
      case None =>
        Redirect(errorRedirectUrl(isAgent, incomeSourceType, id.value))
    }
  }

  private def handleValidForm(form: IncomeSourceReportingMethodForm, incomeSourceType: IncomeSourceType, id: IncomeSourceId, isAgent: Boolean)
                             (implicit user: MtdItUser[_]): Future[Result] = {

    val latencyDetails = getLatencyDetails(incomeSourceType, id.value)
    val isAnnual: String => Boolean = (reportingMethod: String) => reportingMethod.equals("A")

    val latencyIndicators =
      (for {
        selectedReportingMethodTaxYear1 <- form.newTaxYear1ReportingMethod
        latencyDetails <- latencyDetails
        if selectedReportingMethodTaxYear1 != latencyDetails.latencyIndicator1
      } yield {
        Some(TaxYearSpecific(latencyDetails.taxYear1, isAnnual(selectedReportingMethodTaxYear1)))
      }) ++ (for {
        selectedReportingMethodTaxYear2 <- form.newTaxYear2ReportingMethod
        latencyDetails <- latencyDetails
        if selectedReportingMethodTaxYear2 != latencyDetails.latencyIndicator2
      } yield {
        Some(TaxYearSpecific(latencyDetails.taxYear2, isAnnual(selectedReportingMethodTaxYear2)))
      })

    val filteredIndicators = latencyIndicators.flatten.toSeq

    updateReportingMethod(isAgent, id, incomeSourceType, filteredIndicators)
  }

  private def updateReportingMethod(isAgent: Boolean, id: IncomeSourceId, incomeSourceType: IncomeSourceType, newReportingMethods: Seq[TaxYearSpecific])
                                   (implicit user: MtdItUser[_]): Future[Result] = {
    val results = newReportingMethods.foldLeft(Future.successful(Seq.empty[UpdateIncomeSourceResponse])) { (prevFutRes, taxYearSpec) =>
      prevFutRes.flatMap { prevRes =>
        updateIncomeSourceService.updateTaxYearSpecific(user.nino, id.value, taxYearSpec).map { currRes =>
          val isSuccessful = currRes.isInstanceOf[UpdateIncomeSourceResponseModel]
          sendAuditEvent(isSuccessful, taxYearSpec, incomeSourceType, id.value)
          prevRes :+ currRes
        }
      }
    }
    handleUpdateResults(isAgent, incomeSourceType, id, results)
  }.recover {
    case ex: Exception =>
      Logger("application").error(s"[IncomeSourceReportingMethodController][updateReportingMethod]: ${ex.getMessage}")
      Redirect(errorRedirectUrl(isAgent, incomeSourceType, id.value))
  }

  private def sendAuditEvent(isSuccessful: Boolean, newReportingMethod: TaxYearSpecific, incomeSourceType: IncomeSourceType, id: String)
                            (implicit user: MtdItUser[_]): Unit = {
    val businessName: String = user.incomeSources.getIncomeSourceBusinessName(incomeSourceType, Some(id)).getOrElse("Unknown")
    val reportingMethod = if (newReportingMethod.latencyIndicator) "Annually" else "Quarterly"

    auditingService.extendedAudit(
      IncomeSourceReportingMethodAuditModel(
        isSuccessful = isSuccessful,
        journeyType = incomeSourceType.journeyType,
        operationType = "ADD",
        reportingMethodChangeTo = reportingMethod,
        taxYear = (newReportingMethod.taxYear.toInt - 1).toString + "-" + newReportingMethod.taxYear,
        businessName = businessName
      )
    )
  }

  private def handleUpdateResults(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: IncomeSourceId,
                                  updateResults: Future[Seq[UpdateIncomeSourceResponse]]): Future[Result] = {

    updateResults.map { results =>
      val successCount = results.count(_.isInstanceOf[UpdateIncomeSourceResponseModel])
      val errorCount = results.count(_.isInstanceOf[UpdateIncomeSourceResponseError])
      val prefix = s"[IncomeSourceReportingMethodController][handleUpdateResults]: "

      if (successCount == results.length) {
        Logger("application").info(prefix + s"Successfully updated all new selected reporting methods for $incomeSourceType")
        Redirect(redirectUrl(isAgent, incomeSourceType, id.value))
      } else if (errorCount == results.length) {
        Logger("application").info(prefix + s"Unable to update all new selected reporting methods for $incomeSourceType")
        Redirect(errorRedirectUrl(isAgent, incomeSourceType, id.value))
      } else {
        Logger("application").info(prefix + s"Successfully updated one new selected reporting method for $incomeSourceType, the other one failed")
        Redirect(errorRedirectUrl(isAgent, incomeSourceType, id.value))
      }
    }
  }
}
