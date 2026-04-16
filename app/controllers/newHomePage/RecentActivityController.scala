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

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.admin.RecentActivity
import models.financialDetails.Payment
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.obligations.ObligationsModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.newHomePage.RecentActivityService
import services.{DateServiceInterface, FinancialDetailsService, ITSAStatusService, PaymentHistoryService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecentActivityController @Inject()(val newHomeRecentActivityView: views.html.newHomePage.NewHomeRecentActivityView,
                                         val authActions: AuthActions,
                                         recentActivityService: RecentActivityService,
                                         paymentHistoryService: PaymentHistoryService,
                                         financialDetailsService: FinancialDetailsService,
                                         val ITSAStatusService: ITSAStatusService,
                                         val dateService: DateServiceInterface)
                                        (implicit val ec: ExecutionContext,
                                         mcc: MessagesControllerComponents,
                                         val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def show(isAgent: Boolean, origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      if (isEnabled(RecentActivity)) {
        handleShowRequest(origin)
      } else {
        if (isAgent) {
          Future.successful(Redirect(controllers.routes.HandleYourTasksController.showAgent()))
        } else {
          Future.successful(Redirect(controllers.routes.HandleYourTasksController.show()))
        }
      }
  }

  def handleShowRequest(origin: Option[String] = None)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    val currentTaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

    for {
      fulfilledObligations <- recentActivityService.getFulfilledObligations().map {
        case obligations: ObligationsModel => obligations
        case _ => ObligationsModel(Nil)
      }
      currentItsaStatus <- getCurrentITSAStatus(currentTaxYear)
      recentSubmissionActivities = recentActivityService.getRecentSubmissionActivity(fulfilledObligations, currentItsaStatus)

      payments <- paymentHistoryService.getPaymentHistory()
      financialDetails <- financialDetailsService.getAllFinancialDetails()
      recentPaymentActivity = recentActivityService.getRecentPaymentActivity(payments.getOrElse(List.empty[Payment]), financialDetails.map { (_, fdm) => fdm })
      
      recentActivityViewModel = recentActivityService.recentActivityCards(recentSubmissionActivities, recentPaymentActivity)
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

  def yourTasksUrl(origin: Option[String] = None, isAgent: Boolean): String = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show(origin).url

  def recentActivityUrl(origin: Option[String] = None, isAgent: Boolean): String = routes.RecentActivityController.show(isAgent, origin).url

  def overviewUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleOverview(origin, isAgent).url

  def helpUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleHelp(origin, isAgent).url
}
