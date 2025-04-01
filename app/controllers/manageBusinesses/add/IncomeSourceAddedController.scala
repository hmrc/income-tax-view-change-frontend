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
import models.incomeSourceDetails._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.IncomeSourceAddedObligations

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedController @Inject()(authActions: AuthActions,
                                            incomeSourceDetailsService: IncomeSourceDetailsService,
                                            obligationsView: IncomeSourceAddedObligations,
                                            nextUpdatesService: NextUpdatesService,
                                            val sessionService: SessionService,
                                            itvcErrorHandler: ItvcErrorHandler,
                                            itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            dateService: DateServiceInterface)
                                           (implicit val appConfig: FrontendAppConfig, mcc: MessagesControllerComponents, val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  val logger: Logger = Logger("application")

  private def handleRequest(
                             isAgent: Boolean,
                             incomeSourceType: IncomeSourceType
                           )(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {

    val errorView = Future(errorHandler.showInternalServerError())
    val agentPrefix = if (isAgent) "[Agent]" else ""

    withIncomeSourcesFS {
      sessionService.getMongo(IncomeSourceJourneyType(Add, incomeSourceType)).flatMap {

        case Right(Some(sessionData)) =>

          val result: Option[Future[Result]] =
            for {
              incomeSourceId: IncomeSourceId <- sessionData.addIncomeSourceData.flatMap(_.incomeSourceId.map(id => IncomeSourceId(id)))
              (startDate: LocalDate, businessName: Option[String]) <- incomeSourceDetailsService.getIncomeSourceFromUser(incomeSourceType, incomeSourceId)
              showPreviousTaxYears = startDate.isBefore(dateService.getCurrentTaxYearStart)
              reportingMethod: ChosenReportingMethod = incomeSourceDetailsService.getReportingMethod(sessionData.addIncomeSourceData)
            } yield {
              reportingMethod match {
                case ChosenReportingMethod.Unknown =>
                  logger.error(s"retrieved an unknown case for chosen reporting method: $reportingMethod")
                  errorView
                case _ =>
                  handleSuccess(
                    isAgent = isAgent,
                    businessName = businessName,
                    incomeSourceType = incomeSourceType,
                    incomeSourceId = incomeSourceId,
                    showPreviousTaxYears = showPreviousTaxYears,
                    sessionData = sessionData
                  )
              }
            }
          result.getOrElse {
            logger.error(agentPrefix + s"could not find incomeSource for IncomeSourceType: $incomeSourceType")
            errorView
          }
        case _ =>
          logger.error(agentPrefix + s"Unable to retrieve Mongo session data for $incomeSourceType")
          errorView
      }
    }
  }

  private def handleSuccess(
                             incomeSourceId: IncomeSourceId,
                             incomeSourceType: IncomeSourceType,
                             businessName: Option[String],
                             showPreviousTaxYears: Boolean,
                             isAgent: Boolean,
                             sessionData: UIJourneySessionData
                           )(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {

    lazy val showErrorView = errorHandler.showInternalServerError()
    val originalAddIncomeSourceSessionData: AddIncomeSourceData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
    val updatedAddIncomeSourceSessionData: AddIncomeSourceData = originalAddIncomeSourceSessionData.copy(journeyIsComplete = Some(true))
    val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

    val incomeSourceBeingAddedLatencyDetails: Option[LatencyDetails] =
      incomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType)

    val reportingMethodTaxYear1 =
      incomeSourceBeingAddedLatencyDetails.map(_.latencyIndicator1)

    val reportingMethodTaxYear2 =
      incomeSourceBeingAddedLatencyDetails.map(_.latencyIndicator2)

    for {
      _ <- sessionService.setMongoData(uiJourneySessionData)
      viewModel <- nextUpdatesService.getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears)
      dateStarted <- Future(uiJourneySessionData.addIncomeSourceData.flatMap(_.dateStarted))
    } yield {
      dateStarted match {
        case Some(dateStarted) =>
          val taxYearEndOfBusinessStartDate = dateService.getAccountingPeriodEndDate(dateStarted)
          val isBusinessHistoric = taxYearEndOfBusinessStartDate.getYear < viewModel.currentTaxYear - 1
          Ok(
            obligationsView(
              sources = viewModel,
              isAgent = isAgent,
              businessName = businessName,
              incomeSourceType = incomeSourceType,
              currentDate = dateService.getCurrentDate,
              isBusinessHistoric = isBusinessHistoric,
              reportingMethod = viewModel.reportingMethod(reportingMethodTaxYear1, reportingMethodTaxYear2)
            )
          )
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          logger.error(agentPrefix + s"Unable to retrieve Mongo session data for $incomeSourceType")
          showErrorView
      }
    }
  }

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividual.async { implicit mtdItUser =>
      handleRequest(isAgent = false, incomeSourceType)(mtdItUser, itvcErrorHandler)
    }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDAgentWithConfirmedClient.async { implicit mtdItUser =>
      handleRequest(isAgent = true, incomeSourceType)(mtdItUser, itvcErrorHandlerAgent)
    }

}
