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

package obligations.services

import common.auth.MtdItUser
import common.services.DateServiceInterface
import common.models.incomeSourceDetails.{QuarterTypeCalendar, QuarterTypeStandard, TaxYear}
import common.models.obligations.{ObligationWithIncomeType, ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}
import obligations.models.*
import obligations.services.NextUpdatesService.{QuarterlyUpdatesCountForTaxYear, noQuarterlyUpdates}
import play.api.Logger
import shared.connectors.ObligationsConnector
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object NextUpdatesService {

  case class QuarterlyUpdatesCountForTaxYear(taxYear: TaxYear, count: Int)

  private val noQuarterlyUpdates = 0
}

@Singleton
class NextUpdatesService @Inject()(
                                    val obligationsConnector: ObligationsConnector
                                  )(implicit ec: ExecutionContext, val dateService: DateServiceInterface) {

  def getNextUpdatesViewModel(obligationsModel: ObligationsModel)(implicit user: MtdItUser[_]): NextUpdatesViewModel = {
    val allDeadlines =
      obligationsModel.obligationsByDate.flatMap { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
        if (obligations.headOption.exists(_.obligation.obligationType == "Quarterly")) {
          val obligationsByType = obligationsModel.groupByQuarterPeriod(obligations)
          Some(
            DeadlineViewModel(QuarterlyObligation,
              standardAndCalendar = true,
              date,
              obligationsByType.getOrElse(Some(QuarterTypeStandard), Seq.empty),
              obligationsByType.getOrElse(Some(QuarterTypeCalendar), Seq.empty))
          )
        } else None
      }.filterNot(deadline => deadline.standardQuarters.isEmpty && deadline.calendarQuarters.isEmpty)

    val (missedDeadlines, remainingDeadlines) = allDeadlines.partition(_.deadline.isBefore(dateService.getCurrentDate))

    // ToDo will be replaced with a feature switch check in the future
    val isFinancialsEnabled = false 
    NextUpdatesViewModel(remainingDeadlines, missedDeadlines, isFinancialsEnabled)
  }

  def getOpenObligations()(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {
    Logger("application").debug(s"Requesting current Next Updates for nino: ${mtdUser.nino}")
    obligationsConnector.getOpenObligations()
  }

  def getAllObligationsWithinDateRange(fromDate: LocalDate, toDate: LocalDate)(
    implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {
    obligationsConnector.getAllObligationsDateRange(fromDate, toDate).map {
      case obligationsResponse: ObligationsModel =>
        ObligationsModel(obligationsResponse.obligations.filter(_.obligations.nonEmpty))
      case error: ObligationsErrorModel =>
        error
    }
  }

  def getQuarterlyUpdatesCounts(queryTaxYear: TaxYear)
                               (implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[QuarterlyUpdatesCountForTaxYear] = {
    getAllObligationsWithinDateRange(queryTaxYear.toFinancialYearStart, queryTaxYear.toFinancialYearEnd).map {
      case obligationsModel: ObligationsModel =>
        QuarterlyUpdatesCountForTaxYear(queryTaxYear, obligationsModel.quarterlyUpdatesCounts)
      case _ => QuarterlyUpdatesCountForTaxYear(queryTaxYear, noQuarterlyUpdates)
    }
  }
}


