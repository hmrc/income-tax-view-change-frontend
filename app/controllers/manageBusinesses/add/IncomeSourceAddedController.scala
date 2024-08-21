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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{Add, JourneyType}
import exceptions.MissingFieldException
import models.core.IncomeSourceId
import models.incomeSourceDetails.{AddIncomeSourceData, ChosenReportingMethod, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.add.IncomeSourceAddedObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val obligationsView: IncomeSourceAddedObligations,
                                            nextUpdatesService: NextUpdatesService,
                                            auth: AuthenticatorPredicate)
                                           (implicit val appConfig: FrontendAppConfig,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            implicit override val mcc: MessagesControllerComponents,
                                            implicit val sessionService: SessionService,
                                            val ec: ExecutionContext,
                                            dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(isAgent = false, incomeSourceType)
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(isAgent = true, incomeSourceType)
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.getMongo(JourneyType(Add, incomeSourceType).toString).flatMap {
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
                errorHandler(isAgent).showInternalServerError()
              }
            }
          }) getOrElse {
            Logger("application").error(
              s"${if (isAgent) "[Agent]" else ""}" + s"could not find incomeSource for IncomeSourceType: $incomeSourceType")
            Future.successful {
              errorHandler(isAgent).showInternalServerError()
            }
          }
        case _ =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix + s"Unable to retrieve Mongo session data for $incomeSourceType")
          Future.successful {
            errorHandler(isAgent).showInternalServerError()
          }
      }
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
          s"Error getting IncomeSourceAdded page: - ${ex.getMessage} - ${ex.getCause}, IncomeSourceType: $incomeSourceType")
        errorHandler(isAgent).showInternalServerError()
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
                           )(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
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
            errorHandler(isAgent).showInternalServerError()
          }
      }
    }
  }
}
