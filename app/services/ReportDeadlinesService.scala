/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.{BusinessReportDeadlinesConnector, PropertyReportDeadlineDataConnector}
import models._
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ReportDeadlinesService @Inject()(val businessReportDeadlinesConnector: BusinessReportDeadlinesConnector,
                                       val propertyReportDeadlineDataConnector: PropertyReportDeadlineDataConnector
                                      ) {

  def getBusinessReportDeadlines(nino: String, selfEmploymentId: String)
                                (implicit hc: HeaderCarrier): Future[ObligationsResponseModel] = {
    Logger.debug(
      s"[ReportDeadlinesService][getBusinessReportDeadlineData] - Requesting Business Obligation details from connector for user with NINO: $nino")
    businessReportDeadlinesConnector.getBusinessReportDeadlineData(nino, selfEmploymentId)
  }

  def getPropertyReportDeadlines(nino: String)
                                (implicit hc: HeaderCarrier): Future[ObligationsResponseModel] = {
    Logger.debug (
      s"[ReportDeadlinesService][getPropertyReportDeadlineData] - Requesting Property Obligation details from connectors for user with NINO: $nino")
    propertyReportDeadlineDataConnector.getPropertyReportDeadlineData (nino)
  }
}
