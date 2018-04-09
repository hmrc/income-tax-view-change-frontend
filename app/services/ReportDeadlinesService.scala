/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import connectors._
import models.reportDeadlines.ReportDeadlinesResponseModel
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ReportDeadlinesService @Inject()(val reportDeadlinesConnector: ReportDeadlinesConnector) {

  def getReportDeadlines(incomeSourceId: String)(implicit hc: HeaderCarrier): Future[ReportDeadlinesResponseModel] = {
    Logger.debug(s"[ReportDeadlinesService][getReportDeadlines] - Requesting Report Deadlines for incomeSourceID: $incomeSourceId")
    reportDeadlinesConnector.getReportDeadlines(incomeSourceId)
  }
}
