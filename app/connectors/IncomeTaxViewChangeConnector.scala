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

  def getEstimatedTaxLiabilityUrl(nino: String, year: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/estimated-tax-liability/$nino/$year/it"
  }

  def getNinoLookupUrl(mtdRef: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/nino-lookup/$mtdRef"
  }

  def getReportDeadlinesUrl(incomeSourceID: String, nino: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/$nino/income-source/$incomeSourceID/report-deadlines"
  }

  def getLatestCalculation(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[CalculationResponseModel] = {
    val url = getLatestCalculationUrl(nino, taxYear.toString)
    Logger.debug(s"[CalculationDataConnector][getLatestCalculation] - GET $url")

    http.GET[HttpResponse](url)(httpReads, hc.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[CalculationDataConnector][getLatestCalculation] - Response status: ${response.status}, json: ${response.json}")
          response.json.validate[CalculationModel].fold(
            invalid => {
              Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Json validation error parsing calculation model response. Invalid=$invalid")
              CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Json validation error parsing calculation model response")
            },
            valid => valid
          )
        case _ =>
          Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Response status: ${response.status}, json: ${response.body}")
          Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Response status: [${response.status}] returned from Latest Calculation call")
          CalculationErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Unexpected future failed error, ${ex.getMessage}")
        CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      case _ =>
        Logger.error(s"[CalculationDataConnector][getLatestCalculation] - Unexpected future failed error")
        CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error")
    }
  }

  def getIncomeSources(mtditid: String, nino: String)(implicit headerCarrier: HeaderCarrier): Future[IncomeSourceDetailsResponse] = {

    val url = getIncomeSourcesUrl(mtditid)
    Logger.debug(s"[IncomeSourceDetailsConnector][getIncomeSources] - GET $url")

    auditingService.audit(IncomeSourceDetailsRequestAuditModel(mtditid, nino))

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[IncomeSourceDetailsConnector][getIncomeSources] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[IncomeSourceDetailsModel].fold(
            invalid => {
              Logger.error(s"[IncomeSourceDetailsConnector][getIncomeSources] - Json Validation Error. Parsing Latest Calc Response")
              Logger.error(s"[IncomeSourceDetailsConnector][getIncomeSources] $invalid")
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
        case _ =>
          Logger.error(s"[IncomeSourceDetailsConnector][getIncomeSources] - RESPONSE status: ${response.status}, body: ${response.body}")
          Logger.error(s"[IncomeSourceDetailsConnector][getIncomeSources] - Response status: [${response.status}] from Latest Calc call")
          IncomeSourceDetailsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[IncomeSourceDetailsConnector][getIncomeSources] - Unexpected future failed error, ${ex.getMessage}")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      case _ =>
        Logger.error("[IncomeSourceDetailsConnector][getIncomeSources] - Unexpected future failed error")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }

  def getLastEstimatedTax(nino: String, year: Int)(implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {

    val url = getEstimatedTaxLiabilityUrl(nino, year.toString)
    Logger.debug(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[LastTaxCalculation].fold(
            invalid => {
              Logger.error(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - Json Validation Error. Parsing Latest Calc Response")
              LastTaxCalculationError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Latest Calc Response")
            },
            valid => valid
          )
        case NOT_FOUND =>
          Logger.debug(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - No Data Found response")
          NoLastTaxCalculation
        case _ =>
          Logger.error(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - RESPONSE status: ${response.status}, body: ${response.body}")
          Logger.error(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - Response status: [${response.status}] from Latest Calc call")
          LastTaxCalculationError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - Unexpected future failed error, ${ex.getMessage}")
        LastTaxCalculationError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      case _ =>
        Logger.error(s"[LastEstimatedTaxCalculationConnector][getLastEstimatedTax] - Unexpected future failed error")
        LastTaxCalculationError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }

  def getNino(mtdRef: String)(implicit headerCarrier: HeaderCarrier): Future[NinoResponse] = {

    val url = getNinoLookupUrl(mtdRef)
    Logger.debug(s"[NinoLookupConnector][getNino] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[NinoLookupConnector][getNino] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[Nino].fold(
            invalid => {
              Logger.error(s"[NinoLookupConnector][getNino] - Json Validation Error. Parsing Nino Response")
              NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
            },
            valid => valid
          )
        case _ =>
          Logger.error(s"[NinoLookupConnector][getNino] - RESPONSE status: ${response.status}, body: ${response.body}")
          Logger.error(s"[NinoLookupConnector][getNino] - Response status: [${response.status}] from Get Nino call")
          NinoResponseError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[NinoLookupConnector][getNino] - Unexpected future failed error, ${ex.getMessage}")
        NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      case _ =>
        Logger.error(s"[NinoLookupConnector][getNino] - Unexpected future failed error")
        NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }

  def getReportDeadlines(incomeSourceID: String)(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {

    val url = getReportDeadlinesUrl(incomeSourceID, mtdUser.nino)
    Logger.debug(s"[ReportDeadlinesConnector][getReportDeadlines] - GET $url")

    //Audit Report Deadlines Request
    auditingService.audit(ReportDeadlinesRequestAuditModel(mtdUser.mtditid, mtdUser.nino, incomeSourceID))

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[ReportDeadlinesConnector][getReportDeadlines] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[ReportDeadlinesModel].fold(
            invalid => {
              Logger.error("[ReportDeadlinesConnector][getReportDeadlines] - Json Validation Error. Parsing Report Deadlines Data Response")
              Logger.error(s"[ReportDeadlinesConnector][getReportDeadlines] - Json Validation Error: $invalid")
              ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")
            },
            valid => {
              //Audit Report Deadlines Response
              auditingService.extendedAudit(ReportDeadlinesResponseAuditModel(mtdUser.mtditid, mtdUser.nino, incomeSourceID, valid.obligations))
              valid
            }
          )
        case _ =>
          Logger.error(s"[ReportDeadlinesConnector][getReportDeadlines] - RESPONSE status: ${response.status}, body: ${response.body}")
          Logger.error(s"[ReportDeadlinesConnector][getReportDeadlines] - Status: [${response.status}] Returned from business report deadlines call")
          ReportDeadlinesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger.error(s"[ReportDeadlinesConnector][getReportDeadlines] - Unexpected future failed error, ${ex.getMessage}")
        ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      case _ =>
        Logger.error("[ReportDeadlinesConnector][getReportDeadlines] - Unexpected future failed error")
        ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error")
    }
  }

}
