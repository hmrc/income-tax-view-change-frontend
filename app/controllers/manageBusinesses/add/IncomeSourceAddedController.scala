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
import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.IncomeSourceAddedObligationsView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedController @Inject()(
                                             authActions: AuthActions,
                                             incomeSourceDetailsService: IncomeSourceDetailsService,
                                             view: IncomeSourceAddedObligationsView,
                                             nextUpdatesService: NextUpdatesService,
                                             itvcErrorHandler: ItvcErrorHandler,
                                             itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             dateService: DateServiceInterface,
                                             val sessionService: SessionService
                                           )
                                           (implicit val appConfig: FrontendAppConfig, mcc: MessagesControllerComponents, val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  val logger: Logger = Logger("application")

  def getNextUpdatesUrl(isAgent: Boolean) =
    if (isAgent) {
      controllers.routes.NextUpdatesController.showAgent().url
    } else {
      controllers.routes.NextUpdatesController.show().url
    }


  def getManageBusinessUrl(isAgent: Boolean) =
    if (isAgent) {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
    } else {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
    }

  def getReportingFrequencyUrl(isAgent: Boolean) =
    // TODO the below link will need to change to the new entry point for the opt in/out journeys once the page is made
    if (isAgent) {
      controllers.routes.NextUpdatesController.showAgent().url
    } else {
      controllers.routes.NextUpdatesController.show().url
    }

  private def getIncomeSourceIdFromSession(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Option[IncomeSourceId]] = {

    sessionService.getMongo(IncomeSourceJourneyType(Add, incomeSourceType)).flatMap {
      case Right(Some(sessionData)) =>
        val incomeSourceId: Option[IncomeSourceId] = sessionData.addIncomeSourceData.flatMap(_.incomeSourceId.map(id => IncomeSourceId(id)))
        Future(incomeSourceId)
      case _ =>
        Future(None)
    }
  }

  private def handleRequest(
                             isAgent: Boolean,
                             incomeSourceType: IncomeSourceType
                           )(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {

    val errorView = errorHandler.showInternalServerError()
    val agentPrefix = if (isAgent) "[Agent]" else ""

    withNewIncomeSourcesFS {
      sessionService.getMongo(IncomeSourceJourneyType(Add, incomeSourceType)).flatMap {

        case Right(Some(sessionData)) =>
          lazy val result: Future[Result] = {
            for {
              incomeSourceId: Option[IncomeSourceId] <- getIncomeSourceIdFromSession(incomeSourceType)
              incomeSourceFromUser: Option[IncomeSourceFromUser] <- Future(incomeSourceId.flatMap(id => incomeSourceDetailsService.getIncomeSource(incomeSourceType, id, user.incomeSources)))
              showPreviousTaxYears: Boolean = incomeSourceFromUser.exists(_.startDate.isBefore(dateService.getCurrentTaxYearStart))
              showView: Result <-
                (incomeSourceId, incomeSourceFromUser) match {
                  case (Some(incomeSourceId), Some(incomeSource)) =>
                    handleSuccess(
                      isAgent = isAgent,
                      businessName = incomeSource.businessName,
                      incomeSourceType = incomeSourceType,
                      incomeSourceId = incomeSourceId,
                      showPreviousTaxYears = showPreviousTaxYears,
                      sessionData = sessionData
                    )
                  case _ =>
                    Future(errorView)
                }
            } yield {
              showView
            }
          }

          result
        case _ =>
          logger.error(agentPrefix + s"Unable to retrieve Mongo session data for $incomeSourceType")
          Future(errorView)
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

    val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
    val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(incomeSourceCreatedJourneyComplete = Some(true))

    val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

    val incomeSourceBeingAddedLatencyDetails: Option[LatencyDetails] =
      incomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType, user.incomeSources)

    val reportingMethodTaxYear1: Option[String] =
      incomeSourceBeingAddedLatencyDetails.map(_.latencyIndicator1)

    val reportingMethodTaxYear2: Option[String] =
      incomeSourceBeingAddedLatencyDetails.map(_.latencyIndicator2)

    for {
      _: Boolean <- sessionService.setMongoData(uiJourneySessionData)
      viewModel: ObligationsViewModel <- nextUpdatesService.getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears)
      dateStarted: Option[LocalDate] <- Future(uiJourneySessionData.addIncomeSourceData.flatMap(_.dateStarted))
    } yield {
      dateStarted match {
        case Some(dateStarted) =>
          val taxYearEndOfBusinessStartDate = dateService.getAccountingPeriodEndDate(dateStarted)
          val isBusinessHistoric = taxYearEndOfBusinessStartDate.getYear < viewModel.currentTaxYear - 1
          Ok(
            view(
              viewModel = viewModel,
              isAgent = isAgent,
              businessName = businessName,
              incomeSourceType = incomeSourceType,
              currentDate = dateService.getCurrentDate,
              isBusinessHistoric = isBusinessHistoric,
              reportingMethod = viewModel.reportingMethod(reportingMethodTaxYear1, reportingMethodTaxYear2),
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = getReportingFrequencyUrl(isAgent),
              getNextUpdatesUrl = getNextUpdatesUrl(isAgent),
              getManageBusinessUrl = getManageBusinessUrl(isAgent)
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
