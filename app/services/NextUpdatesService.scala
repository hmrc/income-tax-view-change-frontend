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
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels._
import models.nextUpdates._
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesService @Inject()(val obligationsConnector: ObligationsConnector)(implicit ec: ExecutionContext, val dateService: DateServiceInterface) {


  def getNextDeadlineDueDateAndOverDueObligations(implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_], isTimeMachineEnabled: Boolean): Future[(LocalDate, Seq[LocalDate])] = {
    getNextUpdates().map {
      case deadlines: ObligationsModel if !deadlines.obligations.forall(_.obligations.isEmpty) =>
        val latestDeadline = deadlines.obligations.flatMap(_.obligations.map(_.due)).sortWith(_ isBefore _).head
        val overdueObligations = deadlines.obligations.flatMap(_.obligations.map(_.due)).filter(_.isBefore(dateService.getCurrentDate(isTimeMachineEnabled)))
        (latestDeadline, overdueObligations)
      case error: NextUpdatesErrorModel => throw new Exception(s"${error.message}")
      case _ =>
        Logger("application").error("Unexpected Exception getting next deadline due and Overdue Obligations")
        throw new Exception(s"Unexpected Exception getting next deadline due and Overdue Obligations")
    }
  }

  def getObligationDueDates(implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_], isTimeMachineEnabled: Boolean): Future[Either[(LocalDate, Boolean), Int]] = {
    getNextUpdates().map {

      case deadlines: ObligationsModel if deadlines.obligations.forall(_.obligations.nonEmpty) =>
        val dueDates = deadlines.obligations.flatMap(_.obligations.map(_.due)).sortWith(_ isBefore _)
        val overdueDates = dueDates.filter(_ isBefore dateService.getCurrentDate(isTimeMachineEnabled))
        val nextDueDates = dueDates.diff(overdueDates)
        val overdueDatesCount = overdueDates.size

        overdueDatesCount match {
          case 0 => Left(nextDueDates.head -> false)
          case 1 => Left(overdueDates.head -> true)
          case _ => Right(overdueDatesCount)
        }
      case _ =>
        throw new InternalServerException(s"Unexpected Exception getting obligation due dates")
    }
  }

  def getNextUpdates(previous: Boolean = false)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {
    if (previous) {
      Logger("application").debug(s"[NextUpdatesService][getNextUpdates] - Requesting previous Next Updates for nino: ${mtdUser.nino}")
      obligationsConnector.getPreviousObligations()
    } else {
      Logger("application").debug(s"[NextUpdatesService][getNextUpdates] - Requesting current Next Updates for nino: ${mtdUser.nino}")
      obligationsConnector.getNextUpdates()
    }
  }


  private def obligationFilter(fromDate: LocalDate, toDate: LocalDate, obligationsModel: ObligationsModel): Seq[NextUpdatesModel] = {
    obligationsModel.obligations map {
      nextUpdates =>
        nextUpdates.copy(obligations = nextUpdates.obligations.filterNot {
          nextUpdate => nextUpdate.start.isBefore(fromDate) || nextUpdate.end.isAfter(toDate)
        })
    }
  }

  def getNextUpdates(fromDate: LocalDate, toDate: LocalDate)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {

    for {
      previousObligations <- obligationsConnector.getPreviousObligations(fromDate, toDate)
      openObligations <- obligationsConnector.getNextUpdates()
    } yield {
      (previousObligations, openObligations) match {
        case (ObligationsModel(previous), open: ObligationsModel) =>
          ObligationsModel( (previous ++ obligationFilter(fromDate, toDate, open) ).filter(_.obligations.nonEmpty))
        case (error: NextUpdatesErrorModel, open: ObligationsModel) if error.code == 404 =>
          ObligationsModel( obligationFilter(fromDate, toDate, open).filter(_.obligations.nonEmpty) )
        case (error: NextUpdatesErrorModel, _) => error
        case (_, error: NextUpdatesErrorModel) => error
        case (_, _) => NextUpdatesErrorModel(500, "[NextUpdatesService][getNextUpdates] Invalid response from connector")
      }
    }
  }

  def getObligationDates(id: String)
                        (implicit user: MtdItUser[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[DatesModel]] = {
    getNextUpdates() map {
      case NextUpdatesErrorModel(code, message) =>
        Logger("application").error(
          s"[IncomeSourceAddedController][handleRequest] - Error: $message, code $code")
        Seq.empty
      case NextUpdateModel(start, end, due, obligationType, _, periodKey) =>
        Seq(DatesModel(start,
          end,
          due,
          periodKey,
          isFinalDec = false,
          obligationType = obligationType))
      case model: ObligationsModel =>
        Seq(model.obligations.flatMap(nextUpdatesModel => nextUpdatesModel.currentCrystDeadlines) map {
          source =>
            DatesModel(source.start, source.end, source.due, source.periodKey, isFinalDec = true, obligationType = source.obligationType)
        },
          model.obligations
            .filter(nextUpdatesModel => mkIncomeSourceId(nextUpdatesModel.identification) == mkIncomeSourceId(id))
            .flatMap(obligation => obligation.obligations.map(nextUpdateModel =>
              DatesModel(nextUpdateModel.start, nextUpdateModel.end, nextUpdateModel.due, nextUpdateModel.periodKey, isFinalDec = false, obligationType = nextUpdateModel.obligationType)))
        ).flatten
    }
  }

  def getObligationsViewModel(id: String, showPreviousTaxYears: Boolean)
                             (implicit user: MtdItUser[_], ec: ExecutionContext, hc: HeaderCarrier): Future[ObligationsViewModel] = {
    val processingRes = for {
      datesList <- getObligationDates(id)
    } yield {
      val (finalDeclarationDates, otherObligationDates) = datesList.partition(datesModel => datesModel.isFinalDec)

      val quarterlyDates: Seq[DatesModel] = otherObligationDates.filter(datesModel => datesModel.obligationType == "Quarterly" )
        .sortBy(_.inboundCorrespondenceFrom)

      val obligationsGroupedByYear = quarterlyDates.groupBy(datesModel => dateService.getAccountingPeriodEndDate(datesModel.inboundCorrespondenceTo).getYear)
      val sortedObligationsByYear = obligationsGroupedByYear.toSeq.sortBy(_._1).map(_._2.distinct.sortBy(_.periodKey))

      val eopsDates: Seq[DatesModel] = otherObligationDates.filter(datesModel => datesModel.periodKey.contains("EOPS")).distinct.sortBy(_.inboundCorrespondenceFrom)

      val finalDecDates: Seq[DatesModel] = finalDeclarationDates.distinct.sortBy(_.inboundCorrespondenceFrom)

      ObligationsViewModel(sortedObligationsByYear, eopsDates, finalDecDates, dateService.getCurrentTaxYearEnd(), showPrevTaxYears = showPreviousTaxYears)
    }
    processingRes
  }
}
