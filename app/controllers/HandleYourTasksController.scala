/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import auth.MtdItUser
import auth.authV2.AuthActions
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import controllers.agent.sessionUtils.SessionKeys
import models.admin.*
import models.financialDetails.*
import models.newHomePage.HandleYourTasksViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.optIn.OptInService
import services.optout.OptOutService
import services.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.HandleYourTasksView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HandleYourTasksController @Inject()(val authActions: AuthActions,
                                val handleYourTasksView: HandleYourTasksView,
                                val optInService: OptInService,
                                val optOutService: OptOutService,
                                val ITSAStatusService: ITSAStatusService,
                                val whatYouOweService: WhatYouOweService,
                                val dateService: DateServiceInterface,
                                val financialDetailsService: FinancialDetailsService)
                               (implicit val ec: ExecutionContext,
                                mcc: MessagesControllerComponents,
                                val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {


  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleShowRequest(origin)
  }

  def showAgent(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient().async {
    implicit mtdItUser =>
      handleShowRequest(origin)
  }

  def handleShowRequest(origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    handleYourTasks(origin, user.isAgent())
  }

  private def handleYourTasks(origin: Option[String] = None, isAgent: Boolean)
                             (implicit user: MtdItUser[_]): Future[Result] = {
    for {
      unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails()
      _ <- optInService.updateJourneyStatusInSessionData(journeyComplete = false)
      _ <- optOutService.updateJourneyStatusInSessionData(journeyComplete = false)
      mandation <- ITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandated)
      chargeItemList = getChargeList(unpaidCharges, isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals))
    } yield {

      val creditsRefundsRepayEnabled = isEnabled(CreditsRefundsRepay)
      val mandationStatus =
        if (mandation) SessionKeys.mandationStatus -> "on"
        else SessionKeys.mandationStatus -> "off"

      val homeViewModel = HandleYourTasksViewModel(chargeItemList, unpaidCharges, creditsRefundsRepayEnabled)

      Ok(handleYourTasksView(origin, isAgent,
        yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent),
        overviewUrl(origin, isAgent), helpUrl(origin, isAgent), homeViewModel)).addingToSession(mandationStatus)

    }
  }

  private def getChargeList(unpaidCharges: List[FinancialDetailsResponseModel], isFilterOutCodedPoasEnabled: Boolean, penaltiesEnabled: Boolean): List[ChargeItem] = {

    val chargesList =
      unpaidCharges.collect {
        case fdm: FinancialDetailsModel => fdm
      }
    whatYouOweService.getFilteredChargesList(
      financialDetailsList = chargesList,
      isFilterCodedOutPoasEnabled = isFilterOutCodedPoasEnabled,
      isPenaltiesEnabled = penaltiesEnabled,
      remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot = mainChargeIsNotPaidFilter)
  }

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem] = {
    case x if x.remainingToPayByChargeOrInterestWhenChargeIsPaid => x
  }

  def yourTasksUrl(origin: Option[String] = None, isAgent: Boolean): String = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show(origin).url

  def recentActivityUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleRecentActivity(origin, isAgent).url

  def overviewUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleOverview(origin, isAgent).url

  def helpUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleHelp(origin, isAgent).url

}
