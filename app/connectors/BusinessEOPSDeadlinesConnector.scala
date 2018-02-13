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
class BusinessEOPSDeadlinesConnector @Inject()(val http: HttpClient, val config: FrontendAppConfig) extends RawResponseReads {

  private[connectors] def getBusinessEOPSDeadlineUrl(nino: String, selfEmploymentId: String): String =
    s"${config.saApiService}/ni/$nino/self-employments/$selfEmploymentId/end-of-period-statements/obligations"

  def getBusinessEOPSDeadline(nino: String, selfEmploymentId: String)(implicit headerCarrier: HeaderCarrier): Future[ReportDeadlinesResponseModel] = {

    val url = getBusinessEOPSDeadlineUrl(nino, selfEmploymentId)
    Logger.debug(s"[BusinessEOPSDeadlinesConnector][getBusinessEOPSDeadline] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[BusinessEOPSDeadlinesConnector][getBusinessEOPSDeadline] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[ReportDeadlinesModel].fold(
              invalid => {
                Logger.warn(s"[BusinessEOPSDeadlinesConnector][getBusinessEOPSDeadline] - Json Validation Error. Parsing SE Business EOPS Deadlines Response")
                ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing SE Business EOPS Deadlines Response")
              },
              valid => valid
            )
          case _ =>
            Logger.debug(s"[BusinessEOPSDeadlinesConnector][getBusinessEOPSDeadline] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(
              s"[BusinessEOPSDeadlinesConnector][getBusinessEOPSDeadline] - Response status: [${response.status}] returned from SE Business EOPS Deadlines call")
            ReportDeadlinesErrorModel(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn(s"[BusinessEOPSDeadlinesConnector][getBusinessEOPSDeadline] - Unexpected future failed error")
        ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }
}
