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
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, ITSAStatusService, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.add.IncomeSourceAddedObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val obligationsView: IncomeSourceAddedObligations,
                                            val itsaStatusService: ITSAStatusService,
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
            itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap { _ =>
              val (reportingMethodTaxYear1, reportingMethodTaxYear2) = (
                sessionData.addIncomeSourceData.flatMap(_.reportingMethodTaxYear1).orElse(Some("A")),
                sessionData.addIncomeSourceData.flatMap(_.reportingMethodTaxYear2).orElse(Some("A"))
              )
              val isHybridReporting = reportingMethodTaxYear1 != reportingMethodTaxYear2
              handleSuccess(
                isAgent = isAgent,
                businessName = businessName,
                incomeSourceType = incomeSourceType,
                incomeSourceId = incomeSourceIdModel,
                showPreviousTaxYears = startDate.isBefore(dateService.getCurrentTaxYearStart),
                sessionData = sessionData,
                isHybridReporting = isHybridReporting
              )
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

  private def handleSuccess(incomeSourceId: IncomeSourceId, incomeSourceType: IncomeSourceType, businessName: Option[String],
                            showPreviousTaxYears: Boolean, isAgent: Boolean, sessionData: UIJourneySessionData, isHybridReporting: Boolean
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
            try {
              Ok(obligationsView(
                businessName = businessName,
                sources = viewModel,
                isAgent = isAgent,
                incomeSourceType = incomeSourceType,
                currentDate = dateService.getCurrentDate,
                isBusinessHistoric = isBusinessHistoric,
                isHybridReporting = isHybridReporting
              ))
            } catch {
              case error: MissingFieldException =>
                Logger("application").error(s"Missing field: ${error.getMessage}")
                errorHandler(isAgent).showInternalServerError()
            }
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


  private def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val redirectUrl = if (isAgent) routes.AddIncomeSourceController.showAgent().url else routes.AddIncomeSourceController.show().url
    Future.successful(Redirect(redirectUrl))
  }

  def submit: Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def agentSubmit: Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleSubmitRequest(isAgent = true)
  }
}
