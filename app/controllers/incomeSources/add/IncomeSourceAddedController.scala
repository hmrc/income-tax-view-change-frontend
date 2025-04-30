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

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.{AfterSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import models.core.IncomeSourceId
import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import models.incomeSourceDetails.{AddIncomeSourceData, IncomeSourceFromUser, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyChecker
import views.html.incomeSources.add.IncomeSourceAddedObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedController @Inject()(val authActions: AuthActions,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val obligationsView: IncomeSourceAddedObligations,
                                            nextUpdatesService: NextUpdatesService,
                                            val sessionService: SessionService,
                                            dateService: DateServiceInterface)
                                           (implicit val appConfig: FrontendAppConfig,
                                            val mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyChecker {

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
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), AfterSubmissionPage) { sessionData =>
      (for {
        incomeSourceIdModel: IncomeSourceId <- sessionData.addIncomeSourceData.flatMap(_.incomeSourceId.map(IncomeSourceId(_)))
        incomeSourceFromUser: IncomeSourceFromUser <- incomeSourceDetailsService.getIncomeSource(incomeSourceType, incomeSourceIdModel, user.incomeSources)
      } yield {
        handleSuccess(
          isAgent = isAgent,
          businessName = incomeSourceFromUser.businessName,
          incomeSourceType = incomeSourceType,
          incomeSourceId = incomeSourceIdModel,
          showPreviousTaxYears = incomeSourceFromUser.startDate.isBefore(dateService.getCurrentTaxYearStart)
        )
      }) getOrElse {
        Logger("application").error(
          s"${if (isAgent) "[Agent]" else ""}" + s"could not find incomeSource for IncomeSourceType: $incomeSourceType")
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

  def handleSuccess(incomeSourceId: IncomeSourceId, incomeSourceType: IncomeSourceType, businessName: Option[String],
                    showPreviousTaxYears: Boolean, isAgent: Boolean)(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {

    sessionService.getMongo(IncomeSourceJourneyType(Add, incomeSourceType)).flatMap {
      case Right(Some(sessionData)) =>
        val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
        val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(journeyIsComplete = Some(true))
        val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))
        sessionService.setMongoData(uiJourneySessionData).flatMap { _ =>
          incomeSourceType match {
            case SelfEmployment =>
              nextUpdatesService.getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears).map { viewModel: ObligationsViewModel =>
                Ok(obligationsView(businessName = businessName, sources = viewModel, isAgent = isAgent, incomeSourceType = SelfEmployment))
              }
            case _ => nextUpdatesService.getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears).map { viewModel =>
              Ok(obligationsView(viewModel, isAgent = isAgent, incomeSourceType = incomeSourceType))
            }
          }
        }
      case _ =>
        val agentPrefix = if (isAgent) "[Agent]" else ""
        Logger("application").error(agentPrefix +
          s"Unable to retrieve Mongo session data for $incomeSourceType")
        Future.successful {
          errorHandler.showInternalServerError()
        }
    }
  }


  private def handleSubmitRequest(isAgent: Boolean): Future[Result] = {
    val redirectUrl = if (isAgent) routes.AddIncomeSourceController.showAgent().url else routes.AddIncomeSourceController.show().url
    Future.successful(Redirect(redirectUrl))
  }

  def submit: Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleSubmitRequest(false)
  }

  def agentSubmit: Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit user =>
      handleSubmitRequest(true)
  }
}
