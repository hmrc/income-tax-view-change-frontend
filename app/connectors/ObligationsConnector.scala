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
import models.obligations.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}
import play.api.Logger
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
                                    )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getOpenObligationsUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/open-obligations"
  }

  def getAllObligationsDateRangeUrl(fromDate: LocalDate, toDate: LocalDate, nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/obligations/from/$fromDate/to/$toDate"
  }

  def getOpenObligations()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {

    val url = getOpenObligationsUrl(mtdUser.nino)
    Logger("application").debug(s"GET $url")

    http.get(url"$url").execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger("application").error(s"Json Validation Error: $invalid")
              ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(NextUpdatesResponseAuditModel(mtdUser, data.identification, data.obligations))
              }
              valid
            }
          )
        case NOT_FOUND | FORBIDDEN =>
          Logger("application").warn(s"Status: ${response.status}, body: ${response.body}")
          ObligationsModel(Seq.empty)
        case status =>
          if (status >= 500) {
            Logger("application").error(s"RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"RESPONSE status: ${response.status}, body: ${response.body}")
          }
          ObligationsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"Unexpected future failed error, ${ex.getMessage}")
        ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getAllObligationsDateRange(fromDate: LocalDate, toDate: LocalDate)
                                (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {

    val url = getAllObligationsDateRangeUrl(fromDate, toDate, mtdUser.nino)
    Logger("application").debug(s"GET $url")

    http.get(url"$url").execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"Status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger("application").error(s"Json Validation Error: $invalid")
              ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(NextUpdatesResponseAuditModel(mtdUser, data.identification, data.obligations))
              }
              valid
            }
          )
        case NOT_FOUND | FORBIDDEN =>
          Logger("application").warn(s"Status: ${response.status}, body: ${response.body}")
          ObligationsModel(Seq.empty)
        case status =>
          if (status >= 500) {
            Logger("application").error(s"Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"Status: ${response.status}, body: ${response.body}")
          }
          ObligationsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"Unexpected failure, ${ex.getMessage}", ex)
        ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

}