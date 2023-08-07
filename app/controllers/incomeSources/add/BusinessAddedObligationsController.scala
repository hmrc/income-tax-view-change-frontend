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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.incomeSourceDetails.BusinessDetailsModel
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesErrorModel, ObligationsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.BusinessAddedObligations

import java.time.LocalDate
import java.time.Month.APRIL
import javax.inject.Inject
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

class BusinessAddedObligationsController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   retrieveNino: NinoPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val obligationsView: BusinessAddedObligations,
                                                   nextUpdatesService: NextUpdatesService)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  private def getBusinessNameAndStartDate(incomeSourceId: String)(implicit user: MtdItUser[_]): Option[(String, LocalDate)] = {
    user.incomeSources.businesses
      .find(_.incomeSourceId.equals(incomeSourceId))
      .flatMap { addedBusiness =>
        for {
          businessName <- addedBusiness.tradingName
          startDate <- addedBusiness.tradingStartDate
        } yield (businessName, startDate)
      }
  }

  private def getBackUrl(incomeSourceId: String, isAgent: Boolean): String = {
    val baseRoute = if (isAgent) controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent _ else
      controllers.incomeSources.add.routes.BusinessReportingMethodController.show _
    baseRoute(incomeSourceId).url
  }

  private def handleRequest(isAgent: Boolean, incomeSourceId: String)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      val businessNameAndStartDate = getBusinessNameAndStartDate(incomeSourceId)

      businessNameAndStartDate match {
        case Some((businessName, startDate)) =>
          val showPreviousTaxYears: Boolean = startDate.isBefore(dateService.getCurrentTaxYearStart())
          nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
            val backUrl = getBackUrl(incomeSourceId, isAgent)
            if (isAgent) Ok(obligationsView(businessName, viewModel,
              controllers.incomeSources.add.routes.BusinessAddedObligationsController.agentSubmit(), backUrl, isAgent = true))
            else Ok(obligationsView(businessName, viewModel,
              controllers.incomeSources.add.routes.BusinessAddedObligationsController.submit(), backUrl, isAgent = false))
          }
        case None =>
          val errorMessage = s"Unable to find incomeSource by id: $incomeSourceId"
          Logger("application").error(errorMessage)
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Future.successful(errorHandler.showInternalServerError())
      }
    }
  }

  private def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val redirectUrl = if (isAgent) routes.AddIncomeSourceController.showAgent().url else routes.AddIncomeSourceController.show().url
    Future.successful(Redirect(redirectUrl))
  }

  def show(incomeSourceId: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false, incomeSourceId)
  }

  def showAgent(incomeSourceId: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(isAgent = true, incomeSourceId)
        }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }
}
