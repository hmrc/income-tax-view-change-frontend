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
import cats.implicits.catsSyntaxOptionId
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{AfterSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import models.core.IncomeSourceId
import models.incomeSourceDetails.AddIncomeSourceData.{addIncomeSourceDataLens, journeyIsCompleteLens}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.add.IncomeSourceAddedObligations
import controllers.manageBusinesses.add.routes._

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

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(isAgent = false, incomeSourceType)
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(isAgent = true, incomeSourceType)
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withSessionData(JourneyType(Add, incomeSourceType), AfterSubmissionPage) { sessionData =>
      (for {
        incomeSourceIdModel <- sessionData.addIncomeSourceData.flatMap(_.incomeSourceId.map(IncomeSourceId(_)))
        (startDate, businessName) <- incomeSourceDetailsService.getIncomeSourceFromUser(incomeSourceType, incomeSourceIdModel)
      } yield {
        handleSuccess(
          isAgent = isAgent,
          businessName = businessName,
          incomeSourceType = incomeSourceType,
          incomeSourceId = incomeSourceIdModel,
          showPreviousTaxYears = startDate.isBefore(dateService.getCurrentTaxYearStart)
        )
      }) getOrElse {
        Logger("application").error(
          s"${if (isAgent) "[Agent]" else ""}" + s"could not find incomeSource for IncomeSourceType: $incomeSourceType")
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

  def handleSuccess(incomeSourceId: IncomeSourceId, incomeSourceType: IncomeSourceType, businessName: Option[String],
                    showPreviousTaxYears: Boolean, isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withSessionData(JourneyType(Add, incomeSourceType), AfterSubmissionPage) { sessionData =>

      val uiJourneySessionData: UIJourneySessionData =
        addIncomeSourceDataLens.replace(
          sessionData.addIncomeSourceData
            .map(journeyIsCompleteLens.replace(true.some)).some
            .getOrElse(AddIncomeSourceData().some)
        )(sessionData)

      sessionService.setMongoData(uiJourneySessionData).flatMap { _ =>
        nextUpdatesService
          .getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears)
          .map { viewModel =>
            Ok(obligationsView(viewModel, isAgent, incomeSourceType, businessName))
          }
      }
    }
  }

  private def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val redirectUrl = (if (isAgent) AddIncomeSourceController.showAgent() else AddIncomeSourceController.show()).url
    Future.successful(Redirect(redirectUrl))
  }

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def submit: Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def agentSubmit: Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleSubmitRequest(isAgent = true)
  }
}
