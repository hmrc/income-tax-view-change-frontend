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

package hub.controllers

import hub.auth.AuthActionsWithTriggeredMigrationCheck
import common.auth.MtdItUser
import common.config.*
import common.config.featureswitch.*
import common.models.admin.*
import common.models.core.Nino
import common.services.{DateServiceInterface, ITSAStatusService}
import financials.models.*
import financials.services.*
import hub.services.{NextUpdatesService, PenaltyDetailsService}
import hub.utils.HomePageUtils
import obligations.services.reportingObligations.optOut.OptOutService
import obligations.services.reportingObligations.signUp.SignUpService
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: hub.views.html.HomeView,
                               val newHomeRecentActivityView: hub.views.html.newHomePage.NewHomeRecentActivityView,
                               val newHomeOverviewView: hub.views.html.newHomePage.NewHomeOverviewView,
                               val newHomeHelpView: hub.views.html.newHomePage.NewHomeHelpView,
                               val primaryAgentHomeView: hub.views.html.agent.PrimaryAgentHomeView,
                               val supportingAgentHomeView: hub.views.html.agent.SupportingAgentHomeView,
                               val authActions: AuthActionsWithTriggeredMigrationCheck,
                               val nextUpdatesService: NextUpdatesService,
                               val financialDetailsService: FinancialDetailsService,
                               val dateService: DateServiceInterface,
                               val whatYouOweService: WhatYouOweService,
                               val ITSAStatusService: ITSAStatusService,
                               val penaltyDetailsService: PenaltyDetailsService,
                               val creditService: CreditService,
                               val signUpService: SignUpService,
                               val optOutService: OptOutService)
                              (implicit
                               val ec: ExecutionContext,
                               val itvcErrorHandler: ItvcErrorHandler,
                               val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                               mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching with HomePageUtils {

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividualWithIncomeSources().async {
    implicit user =>
      handleShowRequest(origin)
  }

  def showAgent(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClientWithIncomeSources().async {
    implicit mtdItUser =>
      handleShowRequest(origin)
  }

  def handleShowRequest(origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
      handleYourTasks(origin, user.isAgent)
  }

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem] = {
    case x if x.remainingToPayByChargeOrInterestWhenChargeIsPaid => x
  }


  private def handleYourTasks(@unused origin: Option[String] = None, isAgent: Boolean)
                             (implicit  @unused user: MtdItUser[_]): Future[Result] = {
    if(isAgent){
      Future.successful(Redirect(hub.controllers.newHomePage.routes.HandleYourTasksController.showAgent()))
    }else {
     Future.successful(Redirect(hub.controllers.newHomePage.routes.HandleYourTasksController.show()))
    }
  }

  def handleOverview(origin: Option[String] = None, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user => {
      for {
        ctaViewModel <- whatYouOweService.claimToAdjustViewModel(Nino(user.nino))
        credits <- creditService.getAllCredits
        unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails()
        chargeItem = getChargeList(unpaidCharges, isEnabled(PenaltiesAndAppeals))
      }
      yield {
        Ok(newHomeOverviewView(origin, user.isSupportingAgent, dateService.getCurrentTaxYear,
          yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent), overviewUrl(origin, isAgent),
          helpUrl(origin, isAgent), unpaidCharges.isEmpty, credits.availableCreditInAccount, ctaViewModel, chargeItem,
          isEnabled(PenaltiesAndAppeals), isEnabled(RecentActivity), isEnabled(CreditsRefundsRepay), isEnabled(MortgageEvidence),
          isEnabled(BusinessDetailsFrontend), isEnabled(ObligationsFrontend)))
      }
    }
  }

  def handleHelp(origin: Option[String] = None, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      Future.successful(Ok(newHomeHelpView(origin, yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent), overviewUrl(origin, isAgent), helpUrl(origin, isAgent), isEnabled(RecentActivity))))
  }

  private def getChargeList(unpaidCharges: List[FinancialDetailsResponseModel], penaltiesEnabled: Boolean): List[ChargeItem] = {

    val chargesList =
      unpaidCharges.collect {
        case fdm: FinancialDetailsModel => fdm
      }
    whatYouOweService.getFilteredChargesList(
      financialDetailsList = chargesList,
      isPenaltiesEnabled = penaltiesEnabled,
      remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot = mainChargeIsNotPaidFilter)
  }

}
