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

package shared.connectors

import common.auth.MtdItUser
import common.config.FrontendAppConfig
import common.connectors.RawResponseReads
import common.models.obligations.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}
import common.services.AuditingService
import play.api.Logging
import play.api.http.Status
import play.api.http.Status.{FORBIDDEN, NOT_FOUND, OK}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ObligationsConnector @Inject()(val http: HttpClientV2,
                                     val auditingService: AuditingService,
                                     val appConfig: FrontendAppConfig
                                    )(implicit val ec: ExecutionContext) extends RawResponseReads with Logging {

  def getOpenObligationsUrl(nino: String): String = {
    s"${appConfig.incomeTaxObligationsService}/income-tax-obligations/$nino/open-obligations"
  }

  def getAllObligationsDateRangeUrl(fromDate: LocalDate, toDate: LocalDate, nino: String): String = {
    s"${appConfig.incomeTaxObligationsService}/income-tax-obligations/$nino/obligations/from/$fromDate/to/$toDate"
  }

  private def getFulfilledObligationsUrl(nino: String, fromDate: LocalDate, toDate: LocalDate): String = {
    s"${appConfig.incomeTaxObligationsService}/income-tax-obligations/$nino/fulfilled-obligations/from/$fromDate/to/$toDate"
  }

  def getOpenObligations()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {

    val url = getOpenObligationsUrl(mtdUser.nino)
    logger.debug(s"[getOpenObligations] GET $url")

    http.get(url"$url").execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          logger.debug(s"[getOpenObligations] RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              logger.error(s"[getOpenObligations] Json Validation Error: $invalid")
              ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.sendViewObligationsResponseAuditEvent(data.identification, data.obligations)
              }
              valid
            }
          )
        case NOT_FOUND | FORBIDDEN =>
          logger.warn(s"[getOpenObligations] Status: ${response.status}, body: ${response.body}")
          ObligationsModel(Seq.empty)
        case status =>
          if (status >= 500) {
            logger.error(s"[getOpenObligations] RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            logger.warn(s"[getOpenObligations] RESPONSE status: ${response.status}, body: ${response.body}")
          }
          ObligationsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        logger.error(s"[getOpenObligations] Unexpected future failed error, ${ex.getMessage}")
        ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getFulfilledObligations(fromDate: LocalDate, toDate: LocalDate)(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {

    val url = getFulfilledObligationsUrl(mtdUser.nino, fromDate, toDate)
    logger.debug(s"[getFulfilledObligations] GET $url")

    http.get(url"$url").execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          logger.debug(s"[getFulfilledObligations] RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              logger.error(s"[getFulfilledObligations] Json Validation Error: $invalid")
              ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid
            }
          )
        case NOT_FOUND | FORBIDDEN =>
          logger.warn(s"[getFulfilledObligations] Status: ${response.status}, body: ${response.body}")
          ObligationsModel(Seq.empty)
        case status =>
          if (status >= 500) {
            logger.error(s"[getFulfilledObligations] RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            logger.warn(s"[getFulfilledObligations] RESPONSE status: ${response.status}, body: ${response.body}")
          }
          ObligationsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        logger.error(s"[getFulfilledObligations] Unexpected future failed error, ${ex.getMessage}")
        ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getAllObligationsDateRange(fromDate: LocalDate, toDate: LocalDate)
                                (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {

    val url = getAllObligationsDateRangeUrl(fromDate, toDate, mtdUser.nino)
    logger.debug(s"[getAllObligationsDateRange] GET $url")

    http.get(url"$url").execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          logger.debug(s"[getAllObligationsDateRange] Status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              logger.error(s"[getAllObligationsDateRange] Json Validation Error: $invalid")
              ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.sendViewObligationsResponseAuditEvent(data.identification, data.obligations)
              }
              valid
            }
          )
        case NOT_FOUND | FORBIDDEN =>
          logger.warn(s"[getAllObligationsDateRange] Status: ${response.status}, body: ${response.body}")
          ObligationsModel(Seq.empty)
        case status =>
          if (status >= 500) {
            logger.error(s"[getAllObligationsDateRange] Status: ${response.status}, body: ${response.body}")
          } else {
            logger.warn(s"[getAllObligationsDateRange] Status: ${response.status}, body: ${response.body}")
          }
          ObligationsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        logger.error(s"[getAllObligationsDateRange] Unexpected failure, ${ex.getMessage}", ex)
        ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

}