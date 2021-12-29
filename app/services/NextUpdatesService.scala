/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import auth.MtdItUser
import connectors._
import javax.inject.{Inject, Singleton}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.nextUpdates._
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector)(implicit ec: ExecutionContext) {


  // todo check if parameter is needed
  def getNextDeadlineDueDateAndOverDueObligations()
                                                 (implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_]): Future[(LocalDate, Seq[LocalDate])] = {
    getNextUpdates().map {
      case deadlines: ObligationsModel if !deadlines.obligations.forall(_.obligations.isEmpty) =>
        val latestDeadline = deadlines.obligations.flatMap(_.obligations.map(_.due)).sortWith(_ isBefore _).head
        val overdueObligations = deadlines.obligations.flatMap(_.obligations.map(_.due)).filter(_.isBefore(LocalDate.now()))
        (latestDeadline, overdueObligations)
      case error: NextUpdatesErrorModel => throw new Exception(s"${error.message}")
      case _ =>
        Logger("application").error("Unexpected Exception getting next deadline due and Overdue Obligations")
        throw new Exception(s"Unexpected Exception getting next deadline due and Overdue Obligations")
    }
  }

  def getObligationDueDates()
                           (implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_]): Future[Either[(LocalDate, Boolean), Int]] = {
    getNextUpdates().map {

      case deadlines: ObligationsModel if deadlines.obligations.forall(_.obligations.nonEmpty) => {
        val dueDates = deadlines.obligations.flatMap(_.obligations.map(_.due)).sortWith(_ isBefore _)
        val overdueDates = dueDates.filter(_ isBefore LocalDate.now)
        val nextDueDates = dueDates.diff(overdueDates)
        val overdueDatesCount = overdueDates.size

        overdueDatesCount match {
          case 0 => Left(nextDueDates.head -> false)
          case 1 => Left(overdueDates.head -> true)
          case _ => Right(overdueDatesCount)
        }
      }
      case _ =>
        throw new InternalServerException(s"Unexpected Exception getting obligation due dates")
    }
  }

  def getNextUpdates(previous: Boolean = false)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {
    if (previous) {
      Logger("application").debug(s"[NextUpdatesService][getNextUpdates] - Requesting previous Next Updates for nino: ${mtdUser.nino}")
      incomeTaxViewChangeConnector.getPreviousObligations()
    } else {
      Logger("application").debug(s"[NextUpdatesService][getNextUpdates] - Requesting current Next Updates for nino: ${mtdUser.nino}")
      incomeTaxViewChangeConnector.getNextUpdates()
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
      previousObligations <- incomeTaxViewChangeConnector.getPreviousObligations(fromDate, toDate)
      openObligations <- incomeTaxViewChangeConnector.getNextUpdates()
    } yield {
      (previousObligations, openObligations) match {
        case (ObligationsModel(previous), open: ObligationsModel) =>
          ObligationsModel((previous ++ obligationFilter(fromDate, toDate, open)).filter(_.obligations.nonEmpty))
        case (error: NextUpdatesErrorModel, open: ObligationsModel) if error.code == 404 =>
          ObligationsModel(obligationFilter(fromDate, toDate, open).filter(_.obligations.nonEmpty))
        case (error: NextUpdatesErrorModel, _) => error
        case (_, error: NextUpdatesErrorModel) => error
      }
    }
  }
}
