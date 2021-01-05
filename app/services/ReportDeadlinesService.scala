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
import models.reportDeadlines._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportDeadlinesService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector) {

  def getNextDeadlineDueDate(incomeSourceResponse: IncomeSourceDetailsModel)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_]): Future[LocalDate] = {
    getReportDeadlines().map {
      case deadlines: ObligationsModel if !deadlines.obligations.forall(_.obligations.isEmpty) =>
        deadlines.obligations.flatMap(_.obligations.map(_.due)).sortWith(_ isBefore _).head
      case error: ReportDeadlinesErrorModel => throw new Exception(s"${error.message}")
      case _ =>
        Logger.error("Unexpected Exception getting next deadline due")
        throw new Exception(s"Unexpected Exception getting next deadline due")
    }
  }

  def getReportDeadlines(previous: Boolean = false)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {
    if (previous) {
      Logger.debug(s"[ReportDeadlinesService][getReportDeadlines] - Requesting previous Report Deadlines for nino: ${mtdUser.nino}")
      incomeTaxViewChangeConnector.getPreviousObligations()
    } else {
      Logger.debug(s"[ReportDeadlinesService][getReportDeadlines] - Requesting current Report Deadlines for nino: ${mtdUser.nino}")
      incomeTaxViewChangeConnector.getReportDeadlines()
    }
  }
}
