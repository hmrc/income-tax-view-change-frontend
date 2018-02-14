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

import connectors.{BusinessEOPSDeadlinesConnector, BusinessReportDeadlinesConnector, PropertyEOPSDeadlinesConnector, PropertyReportDeadlineDataConnector}
import models._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ReportDeadlinesService @Inject()(val businessReportDeadlinesConnector: BusinessReportDeadlinesConnector,
                                       val propertyReportDeadlineDataConnector: PropertyReportDeadlineDataConnector,
                                       val businessEOPSDeadlinesConnector: BusinessEOPSDeadlinesConnector,
                                       val propertyEOPSDeadlinesConnector: PropertyEOPSDeadlinesConnector
                                      ) {

  def getBusinessReportDeadlines(nino: String, selfEmploymentId: String)(implicit hc: HeaderCarrier): Future[ReportDeadlinesResponseModel] = {
    Logger.debug(
      s"[ReportDeadlinesService][getBusinessReportDeadlineData] - Requesting Business Obligation details for NINO: $nino, selfEmploymentId: $selfEmploymentId")
    for {
      quarterlyObs <- businessReportDeadlinesConnector.getBusinessReportDeadlineData(nino, selfEmploymentId)
      eopsObs <- businessEOPSDeadlinesConnector.getBusinessEOPSDeadline(nino, selfEmploymentId)
    } yield handleReportDeadlines(quarterlyObs, eopsObs)
  }

  def getPropertyReportDeadlines(nino: String)(implicit hc: HeaderCarrier): Future[ReportDeadlinesResponseModel] = {
    Logger.debug(s"[ReportDeadlinesService][getPropertyReportDeadlineData] - Requesting Property Obligation details for NINO: $nino")
    for {
      quarterlyObs <- propertyReportDeadlineDataConnector.getPropertyReportDeadlineData(nino)
      eopsObs <- propertyEOPSDeadlinesConnector.getPropertyEOPSDeadline(nino)
    } yield handleReportDeadlines(quarterlyObs, eopsObs)
  }

  private def handleReportDeadlines(quarterlyObs: ReportDeadlinesResponseModel, eopsObs: ReportDeadlinesResponseModel): ReportDeadlinesResponseModel = {
    (quarterlyObs, eopsObs) match {
      case (qObs: ReportDeadlinesModel, eObs: ReportDeadlinesModel) =>
        Logger.debug(s"[ReportDeadlinesService][handleReportDeadlines] - Quarterly and EOPS Deadlines received")
        ReportDeadlinesModel(qObs.obligations ++ eObs.obligations)
      case (qObs: ReportDeadlinesModel, _) =>
        Logger.debug(s"[ReportDeadlinesService][handleReportDeadlines] - ReportDeadlinesErrorModel received for EOPS; returning only Quarterly Obligations")
        qObs
      case (qObsError: ReportDeadlinesErrorModel, _) =>
        Logger.debug(s"[ReportDeadlinesService][handleReportDeadlines] - ReportDeadlinesErrorModel received for Quarterly obligations")
        qObsError
    }
  }
}
