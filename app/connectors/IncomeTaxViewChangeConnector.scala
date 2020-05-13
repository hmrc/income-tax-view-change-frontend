/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.AuditingService
import audit.models._
import auth.MtdItUser
import config.FrontendAppConfig
import javax.inject.Inject
import models.core.{Nino, NinoResponse, NinoResponseError}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.reportDeadlines.{ObligationsModel, ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxViewChangeConnectorImpl @Inject()(val http: HttpClient,
                                                  val auditingService: AuditingService,
                                                  val config: FrontendAppConfig
                                                )(implicit val ec: ExecutionContext) extends IncomeTaxViewChangeConnector

trait IncomeTaxViewChangeConnector extends RawResponseReads {

  val http: HttpClient
  val auditingService: AuditingService
  val config: FrontendAppConfig
  implicit val ec: ExecutionContext

  def getIncomeSourcesUrl(mtditid: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/income-sources/$mtditid"
  }

  def getNinoLookupUrl(mtdRef: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/nino-lookup/$mtdRef"
  }

  def getReportDeadlinesUrl(nino: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/$nino/report-deadlines"
  }

  def getPreviousObligationsUrl(nino: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/$nino/fulfilled-report-deadlines"
  }

  def getIncomeSources(mtditid: String, nino: String)(implicit headerCarrier: HeaderCarrier): Future[IncomeSourceDetailsResponse] = {

    val url = getIncomeSourcesUrl(mtditid)
    Logger.debug(s"[IncomeTaxViewChangeConnector][getIncomeSources] - GET $url")

    auditingService.audit(IncomeSourceDetailsRequestAuditModel(mtditid, nino))

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[IncomeSourceDetailsModel].fold(
            invalid => {
              Logger.error(s"[IncomeTaxViewChangeConnector][getIncomeSources] $invalid")
              IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
            },
            valid => {
              auditingService.extendedAudit(IncomeSourceDetailsResponseAuditModel(
                mtditid,
                nino,
                valid.businesses.map(_.incomeSourceId),
                valid.property.map(_.incomeSourceId)
              ))
              valid
            }
          )
        case status =>
          if(status >= 500) {
            Logger.error(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger.warn(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          IncomeSourceDetailsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[IncomeTaxViewChangeConnector][getIncomeSources] - Unexpected future failed error, ${ex.getMessage}")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getNino(mtdRef: String)(implicit headerCarrier: HeaderCarrier): Future[NinoResponse] = {

    val url = getNinoLookupUrl(mtdRef)
    Logger.debug(s"[IncomeTaxViewChangeConnector][getNino] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[Nino].fold(
            invalid => {
              Logger.error(s"[IncomeTaxViewChangeConnector][getNino] - Json Validation Error - $invalid")
              NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
            },
            valid => valid
          )
        case status =>
          if(status >= 500) {
            Logger.error(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger.warn(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          NinoResponseError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[IncomeTaxViewChangeConnector][getNino] - Unexpected future failed error, ${ex.getMessage}")
        NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getReportDeadlines()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {

    val url = getReportDeadlinesUrl(mtdUser.nino)
    Logger.debug(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - GET $url")

    auditingService.audit(ReportDeadlinesRequestAuditModel(mtdUser.mtditid, mtdUser.nino))

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger.error(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - Json Validation Error: $invalid")
              ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(ReportDeadlinesResponseAuditModel(mtdUser.mtditid, mtdUser.nino, data.identification, data.obligations))
              }
              valid
            }
          )
        case status =>
          if(status >= 500) {
            Logger.error(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger.warn(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          ReportDeadlinesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - Unexpected future failed error, ${ex.getMessage}")
        ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getPreviousObligations()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {

    val url = getPreviousObligationsUrl(mtdUser.nino)
    Logger.debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - GET $url")

    auditingService.audit(ReportDeadlinesRequestAuditModel(mtdUser.mtditid, mtdUser.nino))

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger.error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Json Validation Error: $invalid")
              ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(ReportDeadlinesResponseAuditModel(mtdUser.mtditid, mtdUser.nino, data.identification, data.obligations))
              }
              valid
            }
          )
        case status =>
          if(status >= 500) {
            Logger.error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger.warn(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          }
          ReportDeadlinesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Unexpected failure, ${ex.getMessage}", ex)
        ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

}
