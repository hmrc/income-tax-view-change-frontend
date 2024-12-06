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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import models.core.IncomeSourceId
import models.incomeSourceDetails.{AddIncomeSourceData, ChosenReportingMethod, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.IncomeSourceAddedObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedController @Inject()(val authActions: AuthActions,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val obligationsView: IncomeSourceAddedObligations,
                                            nextUpdatesService: NextUpdatesService,
                                            val sessionService: SessionService,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            dateService: DateServiceInterface)
                                           (implicit val appConfig: FrontendAppConfig,
                                            val mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(isAgent = false, incomeSourceType)(implicitly, itvcErrorHandler)
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleRequest(isAgent = true, incomeSourceType)(implicitly, itvcErrorHandlerAgent)
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                           (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.getMongo(IncomeSourceJourneyType(Add, incomeSourceType)).flatMap {
        case Right(Some(sessionData)) =>
          (for {
            incomeSourceIdModel <- sessionData.addIncomeSourceData.flatMap(_.incomeSourceId.map(IncomeSourceId(_)))
            (startDate, businessName) <- incomeSourceDetailsService.getIncomeSourceFromUser(incomeSourceType, incomeSourceIdModel)
          } yield {
            val reportingMethod: ChosenReportingMethod = getReportingMethod(sessionData.addIncomeSourceData)
            if (reportingMethod != ChosenReportingMethod.Unknown) {
              handleSuccess(
                isAgent = isAgent,
                businessName = businessName,
                incomeSourceType = incomeSourceType,
                incomeSourceId = incomeSourceIdModel,
                showPreviousTaxYears = startDate.isBefore(dateService.getCurrentTaxYearStart),
                sessionData = sessionData,
                reportingMethod = reportingMethod
              )
            } else {
              Logger("application").error(
                s"${if (isAgent) "[Agent]" else ""}" + s"retrieved an unknown case for chosen reporting method: $reportingMethod")
              Future.successful {
                errorHandler.showInternalServerError()
              }
            }
          }) getOrElse {
            Logger("application").error(
              s"${if (isAgent) "[Agent]" else ""}" + s"could not find incomeSource for IncomeSourceType: $incomeSourceType")
            Future.successful {
              errorHandler.showInternalServerError()
            }
          }
        case _ =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix + s"Unable to retrieve Mongo session data for $incomeSourceType")
          Future.successful {
            errorHandler.showInternalServerError()
          }
      }
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
          s"Error getting IncomeSourceAdded page: - ${ex.getMessage} - ${ex.getCause}, IncomeSourceType: $incomeSourceType")
        errorHandler.showInternalServerError()
    }
  }

  private def getReportingMethod(maybeAddIncomeSourceData: Option[AddIncomeSourceData]): ChosenReportingMethod = {
    val (reportingMethodTaxYear1, reportingMethodTaxYear2) = (
      maybeAddIncomeSourceData.flatMap(_.reportingMethodTaxYear1).orElse(None),
      maybeAddIncomeSourceData.flatMap(_.reportingMethodTaxYear2).orElse(None)
    )
    (reportingMethodTaxYear1, reportingMethodTaxYear2) match {
      case (Some("A"), Some("A")) | (None, Some("A")) => ChosenReportingMethod.Annual
      case (Some("Q"), Some("Q")) | (None, Some("Q")) => ChosenReportingMethod.Quarterly
      case (Some("A"), Some("Q")) | (Some("Q"), Some("A")) => ChosenReportingMethod.Hybrid
      case (None, None) => ChosenReportingMethod.DefaultAnnual
      case _ => ChosenReportingMethod.Unknown
    }
  }

  private def handleSuccess(incomeSourceId: IncomeSourceId, incomeSourceType: IncomeSourceType, businessName: Option[String],
                            showPreviousTaxYears: Boolean, isAgent: Boolean, sessionData: UIJourneySessionData, reportingMethod: ChosenReportingMethod
                           )(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
    val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(journeyIsComplete = Some(true))
    val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))
    sessionService.setMongoData(uiJourneySessionData).flatMap { _ =>
      uiJourneySessionData.addIncomeSourceData.flatMap(_.dateStarted) match {
        case Some(dateStarted) =>
          nextUpdatesService.getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears) map { viewModel =>
            val taxYearEndOfBusinessStartDate = dateService.getAccountingPeriodEndDate(dateStarted)
            val isBusinessHistoric = taxYearEndOfBusinessStartDate.getYear < viewModel.currentTaxYear - 1
            Ok(obligationsView(
              businessName = businessName,
              sources = viewModel,
              isAgent = isAgent,
              incomeSourceType = incomeSourceType,
              currentDate = dateService.getCurrentDate,
              isBusinessHistoric = isBusinessHistoric,
              reportingMethod = reportingMethod
            ))
          }
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix + s"Unable to retrieve Mongo session data for $incomeSourceType")
          Future.successful {
            errorHandler.showInternalServerError()
          }
      }
    }
  }
}
