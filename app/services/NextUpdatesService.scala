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

package services

import auth.MtdItUser
import connectors._
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels._
import models.incomeSourceDetails.{QuarterTypeCalendar, QuarterTypeStandard, TaxYear}
import models.obligations._
import play.api.Logger
import services.NextUpdatesService.{QuarterlyUpdatesCountForTaxYear, noQuarterlyUpdates}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

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

  def getDueDates()(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[Either[Exception, Seq[LocalDate]]] = {
    getOpenObligations().map {
      case deadlines: ObligationsModel if !deadlines.obligations.forall(_.obligations.isEmpty) =>
        Right(deadlines.obligations.flatMap(_.obligations.map(_.due)))
      case ObligationsModel(obligations) if obligations.isEmpty || obligations.forall(_.obligations.isEmpty) => Right(Seq.empty)
      case error: ObligationsErrorModel => Left(new Exception(s"${error.message}"))
      case _ =>
        Left(new Exception("Unexpected Exception getting next deadline due and Overdue Obligations"))
    }
  }

  def getObligationDueDates(implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_]): Future[Either[(LocalDate, Boolean), Int]] = {
    getOpenObligations().map {

      case deadlines: ObligationsModel if deadlines.obligations.forall(_.obligations.nonEmpty) =>
        val dueDates = deadlines.obligations.flatMap(_.obligations.map(_.due)).sortWith(_ isBefore _)
        val overdueDates = dueDates.filter(_ isBefore dateService.getCurrentDate)
        val nextDueDates = dueDates.diff(overdueDates)
        val overdueDatesCount = overdueDates.size

        overdueDatesCount match {
          case 0 => Left(nextDueDates.head -> false)
          case 1 => Left(overdueDates.head -> true)
          case _ => Right(overdueDatesCount)
        }
      case _ =>
        throw new InternalServerException("Unexpected Exception getting obligation due dates")
    }
  }


  def getNextUpdatesViewModel(obligationsModel: ObligationsModel)(implicit user: MtdItUser[_]): NextUpdatesViewModel = {
    val allDeadlines = obligationsModel.obligationsByDate.map { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
      if (obligations.headOption.map(_.obligation.obligationType).contains("Quarterly")) {
        val obligationsByType = obligationsModel.groupByQuarterPeriod(obligations)
        DeadlineViewModel(QuarterlyObligation, standardAndCalendar = true, date, obligationsByType.getOrElse(Some(QuarterTypeStandard), Seq.empty), obligationsByType.getOrElse(Some(QuarterTypeCalendar), Seq.empty))
      }
      else DeadlineViewModel(EopsObligation, standardAndCalendar = false, date, obligations, Seq.empty)
    }.filter(deadline => deadline.obligationType != EopsObligation)

    NextUpdatesViewModel(allDeadlines, dateService.getCurrentTaxYear)
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
      case error: ObligationsErrorModel => error
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
}


