/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject

import audit.AuditingService
import audit.models._
import auth.MtdItUser
import config.FrontendAppConfig
import models.calculation._
import models.core.{Nino, NinoResponse, NinoResponseError}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class IncomeTaxViewChangeConnectorImpl @Inject()(
                                                  val http: HttpClient,
                                                  val auditingService: AuditingService,
                                                  val config: FrontendAppConfig
                                                ) extends IncomeTaxViewChangeConnector

trait IncomeTaxViewChangeConnector extends RawResponseReads {

  val http: HttpClient
  val auditingService: AuditingService
  val config: FrontendAppConfig

  def getLatestCalculationUrl(nino: String, taxYear: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/previous-tax-calculation/$nino/$taxYear"
  }

  def getIncomeSourcesUrl(mtditid: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/income-sources/$mtditid"
  }

  def getNinoLookupUrl(mtdRef: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/nino-lookup/$mtdRef"
  }

  def getReportDeadlinesUrl(incomeSourceID: String, nino: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/$nino/income-source/$incomeSourceID/report-deadlines"
  }

  def getPreviousObligationsUrl(incomeSourceID: String, nino: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/$nino/income-source/$incomeSourceID/fulfilled-report-deadlines"
  }

  def getLatestCalculation(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[CalculationResponseModel] = {
    val url = getLatestCalculationUrl(nino, taxYear.toString)
    Logger.debug(s"[IncomeTaxViewChangeConnector][getLatestCalculation] - GET $url")

    http.GET[HttpResponse](url)(httpReads, hc.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[IncomeTaxViewChangeConnector][getLatestCalculation] - Response status: ${response.status}, json: ${response.json}")
          response.json.validate[CalculationModel].fold(
            invalid => {
              Logger.error(s"[IncomeTaxViewChangeConnector][getLatestCalculation] - Json validation error parsing calculation model response. Invalid=$invalid")
              CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Json validation error parsing calculation model response")
            },
            valid => valid
          )
        case status =>
          if(status >= 500) {
            Logger.error(s"[IncomeTaxViewChangeConnector][getLatestCalculation] - Response status: ${response.status}, json: ${response.body}")
          } else {
            Logger.warn(s"[IncomeTaxViewChangeConnector][getLatestCalculation] - Response status: ${response.status}, json: ${response.body}")
          }
          CalculationErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[IncomeTaxViewChangeConnector][getLatestCalculation] - Unexpected future failed error, ${ex.getMessage}")
        CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
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

  def getReportDeadlines(incomeSourceID: String)(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {

    val url = getReportDeadlinesUrl(incomeSourceID, mtdUser.nino)
    Logger.debug(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - GET $url")

    auditingService.audit(ReportDeadlinesRequestAuditModel(mtdUser.mtditid, mtdUser.nino, incomeSourceID))

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[ReportDeadlinesModel].fold(
            invalid => {
              Logger.error(s"[IncomeTaxViewChangeConnector][getReportDeadlines] - Json Validation Error: $invalid")
              ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")
            },
            valid => {
              auditingService.extendedAudit(ReportDeadlinesResponseAuditModel(mtdUser.mtditid, mtdUser.nino, incomeSourceID, valid.obligations))
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

  def getPreviousObligations(incomeSourceId: String)(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {

    val url = getPreviousObligationsUrl(incomeSourceId, mtdUser.nino)
    Logger.debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - GET $url")

    auditingService.audit(ReportDeadlinesRequestAuditModel(mtdUser.mtditid, mtdUser.nino, incomeSourceId))

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[ReportDeadlinesModel].fold(
            invalid => {
              Logger.error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Json Validation Error: $invalid")
              ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")
            },
            valid => {
              auditingService.extendedAudit(ReportDeadlinesResponseAuditModel(mtdUser.mtditid, mtdUser.nino, incomeSourceId, valid.obligations))
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
