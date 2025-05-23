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
import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.{AfterSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.incomeSources.add.IncomeSourceReportingMethodForm
import models.core.IncomeSourceId
import models.incomeSourceDetails.viewmodels.IncomeSourceReportingMethodViewModel
import models.incomeSourceDetails.{AddIncomeSourceData, LatencyDetails, LatencyYear, UIJourneySessionData}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyChecker
import views.html.incomeSources.add.IncomeSourceReportingMethod

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceReportingMethodController @Inject()(val authActions: AuthActions,
                                                      val updateIncomeSourceService: UpdateIncomeSourceService,
                                                      val itsaStatusService: ITSAStatusService,
                                                      val dateService: DateService,
                                                      val calculationListService: CalculationListService,
                                                      val auditingService: AuditingService,
                                                      val view: IncomeSourceReportingMethod,
                                                      val sessionService: SessionService,
                                                      val itvcErrorHandler: ItvcErrorHandler,
                                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext
                                                     ) extends FrontendController(mcc) with I18nSupport with JourneyChecker {

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  lazy val backUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    (isAgent, incomeSourceType.equals(SelfEmployment)) match {
      case (false, true) => routes.AddBusinessAddressController.show(false).url
      case (true, true) => routes.AddBusinessAddressController.showAgent(false).url
      case (_, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange = false, incomeSourceType).url
    }

  lazy val errorRedirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType).url
    else routes.IncomeSourceReportingMethodNotSavedController.show(incomeSourceType).url

  lazy val redirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    if (isAgent) routes.IncomeSourceAddedController.showAgent(incomeSourceType).url
    else routes.IncomeSourceAddedController.show(incomeSourceType).url

  lazy val submitUrl: (Boolean, IncomeSourceType) => Call = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    routes.IncomeSourceReportingMethodController.submit(isAgent, incomeSourceType)


  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      handleRequest(isAgent = isAgent, incomeSourceType)
  }

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = AfterSubmissionPage) { sessionData =>

      sessionData.addIncomeSourceData.flatMap(_.incomeSourceId) match {
        case Some(id) => handleIncomeSourceIdRetrievalSuccess(incomeSourceType, id, sessionData, isAgent = isAgent)
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
            s"Unable to retrieve incomeSourceId from session data for for $incomeSourceType")
          Future.successful {
            errorHandler(isAgent).showInternalServerError()
          }
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(
          "" +
            s"Unable to display IncomeSourceReportingMethod page for $incomeSourceType: ${ex.getMessage} ${ex.getCause}")
        errorHandler(isAgent).showInternalServerError()
    }
  }


  private def handleIncomeSourceIdRetrievalSuccess(incomeSourceType: IncomeSourceType, id: String, sessionData: UIJourneySessionData, isAgent: Boolean)
                                                  (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    updateIncomeSourceAsAdded(sessionData).flatMap {
      case false => Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error retrieving data from session, IncomeSourceType: $incomeSourceType")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
      case true =>
        itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear().flatMap {
          case true =>
            getViewModel(incomeSourceType, IncomeSourceId(id)).map {
              case Some(viewModel) =>
                Ok(view(
                  incomeSourceReportingMethodForm = IncomeSourceReportingMethodForm.form,
                  incomeSourceReportingViewModel = viewModel,
                  postAction = submitUrl(isAgent, incomeSourceType),
                  isAgent = isAgent))
              case None =>
                Redirect(redirectUrl(isAgent, incomeSourceType))
            }
          case false =>
            Future.successful {
              Redirect(redirectUrl(isAgent, incomeSourceType))
            }
        }
    }
  }

  private def updateIncomeSourceAsAdded(sessionData: UIJourneySessionData): Future[Boolean] = {
    val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
    val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(incomeSourceAdded = Some(true))
    val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

    sessionService.setMongoData(uiJourneySessionData)
  }


  private def getLatencyDetails(incomeSourceType: IncomeSourceType, incomeSourceId: String)(implicit user: MtdItUser[_]): Option[LatencyDetails] = {
    user.incomeSources.getLatencyDetails(incomeSourceType, incomeSourceId)
  }

  private def getViewModel(incomeSourceType: IncomeSourceType, incomeSourceId: IncomeSourceId)
                          (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[IncomeSourceReportingMethodViewModel]] = {
    val currentTaxYear = dateService.getCurrentTaxYearEnd
    val latencyDetails = getLatencyDetails(incomeSourceType, incomeSourceId.value)


    latencyDetails match {
      case Some(LatencyDetails(_, _, _, taxYear2, _)) if taxYear2.toInt < currentTaxYear =>
        Future.successful(None)
      case Some(LatencyDetails(_, taxYear1, taxYear1LatencyIndicator, taxYear2, taxYear2LatencyIndicator)) =>
        calculationListService.determineTaxYearCrystallised(taxYear1.toInt).flatMap {
          case true =>
            Future.successful {
              Some(IncomeSourceReportingMethodViewModel(latencyYear1 = None, latencyYear2 = Some(LatencyYear(taxYear2, taxYear2LatencyIndicator))))
            }
          case false =>
            Future.successful {
              Some(IncomeSourceReportingMethodViewModel(latencyYear1 = Some(LatencyYear(taxYear1, taxYear1LatencyIndicator)),
                latencyYear2 = Some(LatencyYear(taxYear2, taxYear2LatencyIndicator))))
            }
        }
      case _ =>
        Logger("application").info("Latency details not available")
        Future.successful(None)
    }
  }

  def submit(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      handleSubmit(isAgent, incomeSourceType)
  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]):
  Future[Result] = {
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Add, incomeSourceType), AfterSubmissionPage) { sessionData =>
      sessionData.addIncomeSourceData.flatMap(_.incomeSourceId) match {
        case Some(id) => IncomeSourceReportingMethodForm.form.bindFromRequest().fold(
          invalid => handleInvalidForm(invalid, incomeSourceType, IncomeSourceId(id), isAgent),
          valid => handleValidForm(valid, incomeSourceType, IncomeSourceId(id), isAgent))
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
            s"Could not find an incomeSourceId in session data for $incomeSourceType")
          Future.successful {
            errorHandler(isAgent).showInternalServerError()
          }
      }
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
          postAction = submitUrl(isAgent, incomeSourceType),
          isAgent = isAgent))
      case None =>
        Redirect(errorRedirectUrl(isAgent, incomeSourceType))
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
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      Redirect(errorRedirectUrl(isAgent, incomeSourceType))
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
      val prefix = ""

      if (successCount == results.length) {
        Logger("application").info(prefix + s"Successfully updated all new selected reporting methods for $incomeSourceType")
        Redirect(redirectUrl(isAgent, incomeSourceType))
      } else if (errorCount == results.length) {
        Logger("application").info(prefix + s"Unable to update all new selected reporting methods for $incomeSourceType")
        Redirect(errorRedirectUrl(isAgent, incomeSourceType))
      } else {
        Logger("application").info(prefix + s"Successfully updated one new selected reporting method for $incomeSourceType, the other one failed")
        Redirect(errorRedirectUrl(isAgent, incomeSourceType))
      }
    }
  }
}
