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

package hub.services.newHomePage

import com.google.inject.Inject
import common.auth.MtdItUser
import common.models.incomeSourceDetails.TaxYear
import common.services.DateServiceInterface
import models.financialDetails.*
import common.models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, Voluntary}
import hub.models.newHomePage.{RecentActivityCard, RecentActivityPaymentModel, RecentActivitySubmissionsModel, RecentActivityViewModel, RecentRefundModel}
import models.repaymentHistory.RepaymentHistoryModel
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Singleton
import financials.controllers.routes as financialsRoutes
import shared.connectors.ObligationsConnector
import shared.models.{ObligationsModel, SingleObligationModel}

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
    val mostRecentQuarterly: Option[SingleObligationModel] = if (currentItsaStatus != Voluntary && currentItsaStatus != Mandated) None else recentQuarterlySubmissions.maxByOption(_.dateReceived)

    RecentActivitySubmissionsModel(mostRecentAnnual, mostRecentQuarterly)
  }

  def getRecentPaymentActivity(payments: List[Payment]): Option[RecentActivityPaymentModel] = {
    val today = dateService.getCurrentDate
    val recentActivityDate = today.minusDays(90)
    payments
      .filter(_.dueDate.exists(date => !date.isBefore(recentActivityDate) && !date.isAfter(today)))
      .maxByOption(_.dueDate)
      .flatMap {
        case Payment(_, _, Some(amount), _, _, _, _, _, Some(dueDate), _, _, _, _, _) => Some(RecentActivityPaymentModel(amount, dueDate))
        case _ => None
      }
  }

  def getRecentRefundActivity(repaymentHistoryModel: RepaymentHistoryModel, dateService: DateServiceInterface): Option[RecentRefundModel] = {
    val today = dateService.getCurrentDate
    val recentActivityDate = today.minusDays(90)

    val mostRecentRefundWithin90days = repaymentHistoryModel.repaymentsViewerDetails
      .flatMap { refund =>
        refund.estimatedRepaymentDate.collect {
          case date if !date.isBefore(recentActivityDate) && !date.isAfter(today) => refund
        }
      }
      .maxByOption(_.estimatedRepaymentDate)

    mostRecentRefundWithin90days.map(RecentRefundModel(_))
  }

  def recentActivityCards(recentSubmissionActivity: RecentActivitySubmissionsModel, recentPayment: Option[RecentActivityPaymentModel], recentRefundModel: Option[RecentRefundModel])(implicit mtdUser: MtdItUser[_]): RecentActivityViewModel = {
    val submissionsCards = getRecentSubmissionsCards(recentSubmissionActivity.mostRecentAnnualSubmission, recentSubmissionActivity.mostRecentQuarterlySubmission)
    val paymentCard = getRecentPaymentCard(recentPayment)
    val refundCard = getRecentRefundCard(recentRefundModel).toList
    RecentActivityViewModel(submissionsCards ++ paymentCard ++ refundCard)
  }

  private def getRecentPaymentCard(recentPayment: Option[RecentActivityPaymentModel])(implicit mtdUser: MtdItUser[_]) = {
    recentPayment.map { payment =>
      RecentActivityCard(
        linkContentText = "new.home.recentActivity.payments.link.text",
        linkUrl = if (mtdUser.isAgent) financialsRoutes.PaymentHistoryController.showAgent().url else financialsRoutes.PaymentHistoryController.show().url,
        contentText = "new.home.recentActivity.payments.content.text",
        dateContentText = "new.home.recentActivity.payments.date.content.text",
        cardDate = payment.dateOfPayment,
        cardTaxYear = Some(payment.taxYear),
        cardAmount = Some(payment.amount)
      )
    }
  }

  private def getRecentSubmissionsCards(recentAnnualSubmission: Option[SingleObligationModel], recentQuarterlySubmission: Option[SingleObligationModel])(implicit mtdUser: MtdItUser[_]) = {

    def getTaxYearSummaryUrl(taxYear: Int) = mtdUser.isAgent match {
      case true => returns.controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url
      case false => returns.controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear).url
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
          linkUrl = getTaxYearSummaryUrl(getTaxYearIncludingCalendar(obligation.start)),
          contentText = "new.home.recentActivity.submissions.quarterly.content.text",
          dateContentText = "new.home.recentActivity.submissions.quarterly.date.content.text",
          cardDate = date
        )
      }
    }

    List(recentAnnualCard, recentQuarterlyCard).flatten
  }

  private def getTaxYearIncludingCalendar(date: LocalDate): Int = {
    if (date.getMonthValue == 4 && date.getDayOfMonth == 1) {
      date.getYear + 1
    } else {
      TaxYear.getTaxYear(date).endYear
    }
  }

  private def getRecentRefundCard(recentRefundModel: Option[RecentRefundModel])(implicit mtdItUser: MtdItUser[_]): Option[RecentActivityCard] = {

    val paymentCreditRefundUrl = if (mtdItUser.isAgent) {
      financialsRoutes.PaymentHistoryController.showAgent().url
    } else {
      financialsRoutes.PaymentHistoryController.show().url
    }

    recentRefundModel.map { refund =>
      RecentActivityCard(
        linkContentText = "new.home.recentActivity.recentRefund.link.text",
        linkUrl = paymentCreditRefundUrl,
        contentText = "new.home.recentActivity.recentRefund.content.text",
        dateContentText = "new.home.recentActivity.recentRefund.date.content.text",
        cardDate = refund.recentRefund.estimatedRepaymentDate.get,
        cardAmount = refund.recentRefund.totalRepaymentAmount
      )
    }
  }
}
