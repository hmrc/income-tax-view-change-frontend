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

package connectors

import javax.inject.{Inject, Singleton}

import config.FrontendAppConfig
import models._
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class BusinessReportDeadlinesConnector @Inject()(val http: HttpClient,
                                                 val config: FrontendAppConfig) extends RawResponseReads {

  lazy val getReportDeadlineDataUrl: (String, String) => String = (nino, selfEmploymentId) =>
    s"${config.saApiService}/ni/$nino/self-employments/$selfEmploymentId/obligations"

  def getBusinessReportDeadlineData(nino: String, selfEmploymentId: String)(implicit headerCarrier: HeaderCarrier): Future[ReportDeadlinesResponseModel] = {

    val url = getReportDeadlineDataUrl(nino, selfEmploymentId)
    Logger.debug(s"[BusinessReportDeadlinesConnector][getBusinessReportDeadlineData] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[BusinessReportDeadlinesConnector][getBusinessReportDeadlineData] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[ReportDeadlinesModel].fold(
              invalid => {
                Logger.warn(s"[BusinessReportDeadlinesConnector][getBusinessReportDeadlineData] - Json Validation Error. Parsing Report Deadlines Data Response")
                ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")
              },
              valid => valid
            )
          case _ =>
            Logger.debug(s"[BusinessReportDeadlinesConnector][getBusinessReportDeadlineData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(
              s"[BusinessReportDeadlinesConnector][getBusinessReportDeadlineData] - Status: [${response.status}] Returned from business report deadlines call")
            ReportDeadlinesErrorModel(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn(s"[BusinessReportDeadlinesConnector][getBusinessReportDeadlineData] - Unexpected future failed error")
        ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }

}
