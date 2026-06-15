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
import common.models.core.IncomeSourceId.mkIncomeSourceId
import common.models.incomeSourceDetails.{QuarterTypeCalendar, QuarterTypeStandard, TaxYear}
import models.incomeSourceDetails.viewmodels.*
import obligations.connectors.ObligationsConnector
import obligations.models.*
import obligations.services.NextUpdatesService.{QuarterlyUpdatesCountForTaxYear, noQuarterlyUpdates}
import play.api.Logger
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

  def getDueDates(openObligations: Option[Future[ObligationsResponseModel]] = None)(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[Either[Exception, Seq[LocalDate]]] = {
    openObligations.getOrElse(getOpenObligations()).map {
      case deadlines: ObligationsModel if !deadlines.obligations.forall(_.obligations.isEmpty) =>
        Right(deadlines.obligations.flatMap(_.obligations.map(_.due)))
      case ObligationsModel(obligations) if obligations.isEmpty || obligations.forall(_.obligations.isEmpty) =>
        Right(Seq.empty)
      case error: ObligationsErrorModel =>
        Left(new Exception(s"${error.message}"))
      case _ =>
        Left(new Exception("Unexpected Exception getting next deadline due and Overdue Obligations"))
    }
  }

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

    NextUpdatesViewModel(remainingDeadlines, missedDeadlines)
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


  def getObligationDates(id: String)
                        (implicit user: MtdItUser[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[DatesModel]] = {
    getOpenObligations() map {
      case ObligationsErrorModel(code, message) =>
        Logger("application").error(
          s"Error: $message, code $code")
        Seq.empty
      case model: ObligationsModel =>
        Seq(model.obligations.flatMap(nextUpdatesModel => nextUpdatesModel.currentCrystDeadlines) map {
          source =>
            DatesModel(source.start, source.end, source.due, source.periodKey, isFinalDec = true, obligationType = source.obligationType)
        },
          model.obligations
            .filter(nextUpdatesModel => mkIncomeSourceId(nextUpdatesModel.identification) == mkIncomeSourceId(id))
            .flatMap(obligation => obligation.obligations.map(obligation =>
              DatesModel(obligation.start, obligation.end, obligation.due, obligation.periodKey, isFinalDec = false, obligationType = obligation.obligationType)))
        ).flatten
    }
  }

  def getObligationsViewModel(id: String, showPreviousTaxYears: Boolean)
                             (implicit user: MtdItUser[_], ec: ExecutionContext, hc: HeaderCarrier): Future[ObligationsViewModel] = {
    val processingRes =
      for {
        datesList <- getObligationDates(id)
      } yield {
        val (finalDeclarationDates, otherObligationDates) = datesList.partition(datesModel => datesModel.isFinalDec)

        val quarterlyDates: Seq[DatesModel] =
          otherObligationDates
            .filter(datesModel => datesModel.obligationType == "Quarterly")
            .sortBy(_.inboundCorrespondenceFrom)

        val obligationsGroupedByYear =
          quarterlyDates
            .groupBy(datesModel => dateService.getAccountingPeriodEndDate(datesModel.inboundCorrespondenceTo).getYear)

        val sortedObligationsByYear =
          obligationsGroupedByYear
            .toSeq
            .sortBy(_._1)
            .map(_._2.distinct.sortBy(_.periodKey))

        val finalDecDates: Seq[DatesModel] = finalDeclarationDates.distinct.sortBy(_.inboundCorrespondenceFrom)

        ObligationsViewModel(sortedObligationsByYear, finalDecDates, dateService.getCurrentTaxYearEnd, showPrevTaxYears = showPreviousTaxYears)
      }

    processingRes
  }

  def getNextDueDates(openObligations: Option[Future[ObligationsResponseModel]] = None)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[(Option[LocalDate], Option[LocalDate])] = {
    openObligations.getOrElse(getOpenObligations()).map {
      case ObligationsModel(obligations) =>
        val openObligations = obligations.flatMap(_.obligations)

        val nextQuarterlyDueDate = openObligations
          .filter(_.obligationType == "Quarterly")
          .map(_.due)
          .filter(dueDate => !dueDate.isBefore(dateService.getCurrentDate))
          .sorted
          .headOption

        val nextCrystallisationDueDate = openObligations
          .filter(_.obligationType == "Crystallisation")
          .map(_.due)
          .filter(dueDate => !dueDate.isBefore(dateService.getCurrentDate))
          .sorted
          .headOption

        val fallbackNextTaxReturnDate: LocalDate = LocalDate.of(dateService.getCurrentTaxYear.endYear + 1, 1, 31)

        val nextTaxReturnDate: Option[LocalDate] = nextCrystallisationDueDate match {
          case Some(date) => Some(date)
          case None =>
            Logger("application").info("[getNextDueDates] No upcoming crystallisation obligation found - falling back to static next tax return due date")
            Some(fallbackNextTaxReturnDate)
        }

        (nextQuarterlyDueDate, nextTaxReturnDate)

      case error: ObligationsErrorModel =>
        Logger("application").warn(s"[getNextDueDates] Failed to fetch obligations: ${error.message}")
        (None, Some(LocalDate.of(dateService.getCurrentTaxYear.endYear + 1, 1, 31)))
    }
  }
}


