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
import models.financialDetails.*
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, Voluntary}
import models.newHomePage.{RecentActivityCard, RecentActivityPaymentModel, RecentActivitySubmissionsModel, RecentActivityViewModel}
import models.obligations.{ObligationsModel, SingleObligationModel}
import play.api.i18n.Messages
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
        case Payment(_, Some(amount), _, _, _, _, _, Some(dueDate), _, _, _, _, _) => Some(RecentActivityPaymentModel(amount, dueDate))
        case _ => None
      }
  }


  def recentActivityCards(recentSubmissionActivity: RecentActivitySubmissionsModel, recentPayment: Option[RecentActivityPaymentModel])(implicit mtdUser: MtdItUser[_], messages: Messages): RecentActivityViewModel = {
    if (mtdUser.isSupportingAgent) {
      RecentActivityViewModel(Seq.empty)
    } else {
      val submissionsCards = getRecentSubmissionsCards(recentSubmissionActivity.mostRecentAnnualSubmission, recentSubmissionActivity.mostRecentQuarterlySubmission)
      val paymentCard = getRecentPaymentCard(recentPayment)
      RecentActivityViewModel(submissionsCards ++ paymentCard)
    }
  }

  private def getRecentPaymentCard(recentPayment: Option[RecentActivityPaymentModel])(implicit mtdUser: MtdItUser[_], messages: Messages) = {
    recentPayment.map { payment =>
      RecentActivityCard(
        linkContentText = messages("new.home.recentActivity.payments.link.text"),
        linkUrl = if (mtdUser.isAgent) controllers.routes.PaymentHistoryController.showAgent().url else controllers.routes.PaymentHistoryController.show().url,
        contentText = messages("new.home.recentActivity.payments.content.text", payment.amount.abs),
        dateContentText = "new.home.recentActivity.payments.date.content.text",
        cardDate = payment.dateOfPayment,
        cardTaxYear = Some(payment.taxYear)
      )
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
}
