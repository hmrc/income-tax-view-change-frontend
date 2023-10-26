/*
 * Copyright 2023 HM Revenue & Customs
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
import audit.models.NextUpdatesResponseAuditModel
import auth.MtdItUser
import config.FrontendAppConfig
import models.nextUpdates.{NextUpdatesErrorModel, NextUpdatesResponseModel, ObligationsModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ObligationsConnector @Inject()(val http: HttpClient,
                                     val auditingService: AuditingService,
                                     val appConfig: FrontendAppConfig
                                           )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getReportDeadlinesUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/report-deadlines"
  }

  def getPreviousObligationsUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/fulfilled-report-deadlines"
  }

  def getPreviousObligationsUrl(fromDate: LocalDate, toDate: LocalDate, nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/fulfilled-report-deadlines/from/$fromDate/to/$toDate"
  }

  def getNextUpdates()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {

    val url = getReportDeadlinesUrl(mtdUser.nino)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getNextUpdates] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getNextUpdates] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getNextUpdates] - Json Validation Error: $invalid")
              NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(NextUpdatesResponseAuditModel(mtdUser, data.identification, data.obligations))
              }
              valid
            }
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getNextUpdates] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getNextUpdates] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          NextUpdatesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getNextUpdates] - Unexpected future failed error, ${ex.getMessage}")
        NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getPreviousObligations()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {

    val url = getPreviousObligationsUrl(mtdUser.nino)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Json Validation Error: $invalid")
              NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(NextUpdatesResponseAuditModel(mtdUser, data.identification, data.obligations))
              }
              valid
            }
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          }
          NextUpdatesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Unexpected failure, ${ex.getMessage}", ex)
        NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getPreviousObligations(fromDate: LocalDate, toDate: LocalDate)
                            (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {

    val url = getPreviousObligationsUrl(fromDate, toDate, mtdUser.nino)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Json Validation Error: $invalid")
              NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(NextUpdatesResponseAuditModel(mtdUser, data.identification, data.obligations))
              }
              valid
            }
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          }
          NextUpdatesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Unexpected failure, ${ex.getMessage}", ex)
        NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

}