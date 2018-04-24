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
import audit.AuditingService
import audit.models.{ReportDeadlinesRequestAuditModel, ReportDeadlinesResponseAuditModel}
import auth.{MtdItUser, MtdItUserWithNino}
import config.FrontendAppConfig
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class ReportDeadlinesConnector @Inject()(val http: HttpClient,
                                         val config: FrontendAppConfig,
                                         val auditingService: AuditingService
                                        ) extends RawResponseReads {

  private[connectors] lazy val getReportDeadlinesUrl: String => String = incomeSourceID =>
    s"${config.itvcProtectedService}/income-tax-view-change/income-source/$incomeSourceID/report-deadlines"

  def getReportDeadlines(incomeSourceID: String)(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {

    val url = getReportDeadlinesUrl(incomeSourceID)
    Logger.debug(s"[ReportDeadlinesConnector][getReportDeadlines] - GET $url")

    //Audit Report Deadlines Request
    auditingService.audit(ReportDeadlinesRequestAuditModel(mtdUser.mtditid, mtdUser.nino, incomeSourceID))

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[ReportDeadlinesConnector][getReportDeadlines] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[ReportDeadlinesModel].fold(
              invalid => {
                Logger.warn("[ReportDeadlinesConnector][getReportDeadlines] - Json Validation Error. Parsing Report Deadlines Data Response")
                Logger.debug(s"[ReportDeadlinesConnector][getReportDeadlines] - Json Validation Error: $invalid")
                ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")
              },
              valid => {
                //Audit Report Deadlines Response
                auditingService.extendedAudit(ReportDeadlinesResponseAuditModel(mtdUser.mtditid, mtdUser.nino, incomeSourceID, valid.obligations))
                valid
              }
            )
          case _ =>
            Logger.debug(s"[ReportDeadlinesConnector][getReportDeadlines] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[ReportDeadlinesConnector][getReportDeadlines] - Status: [${response.status}] Returned from business report deadlines call")
            ReportDeadlinesErrorModel(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn("[ReportDeadlinesConnector][getReportDeadlines] - Unexpected future failed error")
        ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error")
    }
  }
}
