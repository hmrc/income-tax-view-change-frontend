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

package services.newHomePage

import auth.MtdItUser
import com.google.inject.Inject
import connectors.ObligationsConnector
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, Voluntary}
import models.newHomePage.{RecentActivityCard, RecentActivitySubmissionsModel, RecentActivityViewModel, RecentRefundModel}
import models.obligations.{ObligationsModel, SingleObligationModel}
import models.repaymentHistory.RepaymentHistoryModel
import services.DateServiceInterface
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Singleton

@Singleton
class RecentActivityService @Inject()(obligationsConnector: ObligationsConnector,
                                      dateService: DateServiceInterface) {

  def getFulfilledObligations()(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]) = {
    obligationsConnector.getFulfilledObligations()
  }

  def getRecentSubmissionActivity(fulfilledObligations: ObligationsModel, currentItsaStatus: ITSAStatus): RecentActivitySubmissionsModel = {
    val today = dateService.getCurrentDate
    val recentActivityDate = today.minusDays(90)

    val obligationsReceivedWithin90Days = fulfilledObligations.obligations.flatMap(_.obligations)
      .filter { obligation =>
        obligation.dateReceived.exists { dateReceived =>
          !dateReceived.isBefore(recentActivityDate) && !dateReceived.isAfter(today)
        }
      }

    val (recentAnnualSubmissions, recentQuarterlySubmissions) = obligationsReceivedWithin90Days.partition(_.obligationType == "Crystallisation")

    val mostRecentAnnual: Option[SingleObligationModel] = recentAnnualSubmissions.maxByOption(_.dateReceived)
    val mostRecentQuarterly: Option[SingleObligationModel] = if(currentItsaStatus != Voluntary && currentItsaStatus != Mandated) None else recentQuarterlySubmissions.maxByOption(_.dateReceived)

    RecentActivitySubmissionsModel(mostRecentAnnual, mostRecentQuarterly)
  }

  def getRecentRefundActivity(repaymentHistoryModel: RepaymentHistoryModel, dateService: DateServiceInterface): RecentRefundModel = {
    val today = dateService.getCurrentDate
    val recentActivityDate = today.minusDays(90)
    
    val mostRecentRefundWithin90days = repaymentHistoryModel.repaymentsViewerDetails
      .filter { refund =>
        refund.estimatedRepaymentDate.exists { date =>
          !date.isBefore(recentActivityDate) && !date.isAfter(today)
        }
      }.maxByOption(_.estimatedRepaymentDate)

    RecentRefundModel(mostRecentRefundWithin90days)
  }

  def recentActivityCards(recentSubmissionActivity: RecentActivitySubmissionsModel, recentRefundModel: RecentRefundModel)(implicit mtdUser: MtdItUser[_]): RecentActivityViewModel = {
    if(mtdUser.isSupportingAgent) {
      RecentActivityViewModel(Seq.empty)
    } else {
      val submissionsCards = getRecentSubmissionsCards(recentSubmissionActivity.mostRecentAnnualSubmission, recentSubmissionActivity.mostRecentQuarterlySubmission)
      val refundCard = getRecentRefundCard(recentRefundModel).toList
      RecentActivityViewModel(submissionsCards ++ refundCard)
    }
  }

  private def getRecentSubmissionsCards(recentAnnualSubmission: Option[SingleObligationModel], recentQuarterlySubmission: Option[SingleObligationModel])(implicit mtdUser: MtdItUser[_]) = {

    def getTaxYearSummaryUrl(taxYear: Int) = mtdUser.isAgent match {
      case true => controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url
      case false => controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear).url
    }

    val recentAnnualCard: Option[RecentActivityCard] = recentAnnualSubmission.flatMap { obligation =>
      obligation.dateReceived.map { date =>
        RecentActivityCard(
          linkContentText = "new.home.recentActivity.submissions.annual.link.text",
          linkUrl = getTaxYearSummaryUrl(TaxYear.getTaxYear(obligation.start).endYear),
          contentText = "new.home.recentActivity.submissions.annual.content.text",
          dateContentText = "new.home.recentActivity.submissions.annual.date.content.text",
          cardDate = date,
          cardTaxYear = Some(TaxYear.getTaxYear(obligation.start))
        )
      }
    }

    val recentQuarterlyCard: Option[RecentActivityCard] = recentQuarterlySubmission.flatMap { obligation =>
      obligation.dateReceived.map { date =>
        RecentActivityCard(
          linkContentText = "new.home.recentActivity.submissions.quarterly.link.text",
          linkUrl = getTaxYearSummaryUrl(TaxYear.getTaxYear(obligation.start).endYear),
          contentText = "new.home.recentActivity.submissions.quarterly.content.text",
          dateContentText = "new.home.recentActivity.submissions.quarterly.date.content.text",
          cardDate = date
        )
      }
    }

    List(recentAnnualCard, recentQuarterlyCard).flatten
  }

  private def getRecentRefundCard(recentRefundModel: RecentRefundModel)(implicit mtdItUser: MtdItUser[_]): Option[RecentActivityCard] = {

    val paymentCreditRefundUrl = if (mtdItUser.isAgent) {
      controllers.routes.PaymentHistoryController.showAgent().url
    } else {
      controllers.routes.PaymentHistoryController.show().url
    }

    val recentRefundCard = recentRefundModel.recentRefund.flatMap { refund =>
      refund.estimatedRepaymentDate.map { date =>
        RecentActivityCard(
          linkContentText = "new.home.recentActivity.recentRefund.link.text",
          linkUrl = paymentCreditRefundUrl,
          contentText = "new.home.recentActivity.recentRefund.content.text",
          dateContentText = "new.home.recentActivity.recentRefund.date.content.text",
          cardDate = date,
          cardAmount = refund.totalRepaymentAmount
        )
      }
    }

    recentRefundCard
  }
}
