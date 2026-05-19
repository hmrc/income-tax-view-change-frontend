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

package controllers.newHomePage

import com.google.inject.{Inject, Singleton}
import common.auth.{AuthActions, MtdItUser}
import common.config.FrontendAppConfig
import common.config.featureswitch.FeatureSwitching
import common.services.{DateServiceInterface, ITSAStatusService}
import models.admin.{PaymentHistoryRefunds, RecentActivity}
import models.financialDetails.Payment
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import obligations.models.ObligationsModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.newHomePage.RecentActivityService
import services.{PaymentHistoryService, WhatYouOweService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.HomePageUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecentActivityController @Inject()(val newHomeRecentActivityView: views.html.newHomePage.NewHomeRecentActivityView,
                                         val authActions: AuthActions,
                                         recentActivityService: RecentActivityService,
                                         paymentHistoryService: PaymentHistoryService,
                                         val ITSAStatusService: ITSAStatusService,
                                         val dateService: DateServiceInterface,
                                         val whatYouOweService: WhatYouOweService)
                                        (implicit val ec: ExecutionContext,
                                         mcc: MessagesControllerComponents,
                                         val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching with HomePageUtils {

  def show(isAgent: Boolean, origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      (isEnabled(RecentActivity), !user.isSupportingAgent, user.isAgent) match {
        case (true, true, _) => handleShowRequest(origin)
        case (true, false, _) => Future.successful(Redirect(hub.controllers.routes.HomeController.handleOverview(origin, isAgent)))
        case (false, _, true) => Future.successful(Redirect(controllers.newHomePage.routes.HandleYourTasksController.showAgent()))
        case (false, _, false) => Future.successful(Redirect(controllers.newHomePage.routes.HandleYourTasksController.show()))
      }
  }
  
  def handleShowRequest(origin: Option[String] = None)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    val currentTaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

    for {
      fulfilledObligations <- recentActivityService.getFulfilledObligations().map {
        case obligations: ObligationsModel => obligations
        case _ => ObligationsModel(Nil)
      }

      repaymentHistoryData <- paymentHistoryService.getRepaymentHistory(isEnabled(PaymentHistoryRefunds)).map {
        case Right(repaymentHistory) => models.repaymentHistory.RepaymentHistoryModel(repaymentHistory)
        case Left(value) => models.repaymentHistory.RepaymentHistoryModel(Nil)
      }

      currentItsaStatus <- getCurrentITSAStatus(currentTaxYear)
      recentSubmissionActivities = recentActivityService.getRecentSubmissionActivity(fulfilledObligations, currentItsaStatus)
      payments <- paymentHistoryService.getPaymentHistory().map(_.getOrElse(List.empty[Payment]))
      recentPayment = recentActivityService.getRecentPaymentActivity(payments)
      recentRefunds = recentActivityService.getRecentRefundActivity(repaymentHistoryData, dateService)
      recentActivityViewModel = recentActivityService.recentActivityCards(recentSubmissionActivities, recentPayment, recentRefunds)
    } yield {
      Ok(newHomeRecentActivityView(
        origin,
        user.isAgent,
        yourTasksUrl(origin, user.isAgent),
        recentActivityUrl(origin, user.isAgent),
        overviewUrl(origin, user.isAgent),
        helpUrl(origin, user.isAgent),
        appConfig.itvcRebrand,
        recentActivityViewModel)
      )
    }
  }

  private def getCurrentITSAStatus(currentTaxYear: TaxYear)(
    implicit hc: HeaderCarrier,
    user: MtdItUser[_]
  ): Future[ITSAStatus.ITSAStatus] = {
    ITSAStatusService
      .getITSAStatusDetail(currentTaxYear, false, false)
      .map { statusDetailList =>
        statusDetailList
          .flatMap(_.itsaStatusDetails)
          .flatMap(_.map(_.status))
          .headOption
          .getOrElse(ITSAStatus.NoStatus)
      }
  }

}
