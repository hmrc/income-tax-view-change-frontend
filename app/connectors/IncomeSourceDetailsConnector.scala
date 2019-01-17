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

import javax.inject.{Inject, Singleton}

import audit.AuditingService
import audit.models.{IncomeSourceDetailsRequestAuditModel, IncomeSourceDetailsResponseAuditModel}
import config.FrontendAppConfig
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.http.Status.OK
import play.api.http.{HeaderNames, Status}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class IncomeSourceDetailsConnector @Inject()(val http: HttpClient,
                                             val config: FrontendAppConfig,
                                             val auditingService: AuditingService
                                            ) extends RawResponseReads {

  private[connectors] lazy val getIncomeSourcesUrl: String => String = mtditid =>
    s"${config.itvcProtectedService}/income-tax-view-change/income-sources/$mtditid"

  def getIncomeSources(mtditid: String, nino: String)(implicit headerCarrier: HeaderCarrier): Future[IncomeSourceDetailsResponse] = {

    val url = getIncomeSourcesUrl(mtditid)
    Logger.debug(s"[IncomeSourceDetailsConnector][getIncomeSources] - GET $url")

    auditingService.audit(IncomeSourceDetailsRequestAuditModel(mtditid, nino))

    http.GET[HttpResponse](url) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[IncomeSourceDetailsConnector][getIncomeSources] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[IncomeSourceDetailsModel].fold(
              invalid => {
                Logger.warn(s"[IncomeSourceDetailsConnector][getIncomeSources] - Json Validation Error. Parsing Latest Calc Response")
                Logger.debug(s"[IncomeSourceDetailsConnector][getIncomeSources] $invalid")
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
            Logger.debug(s"[IncomeSourceDetailsConnector][getIncomeSources] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[IncomeSourceDetailsConnector][getIncomeSources] - Response status: [${response.status}] from Latest Calc call")
            IncomeSourceDetailsError(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn(s"[IncomeSourceDetailsConnector][getIncomeSources] - Unexpected future failed error")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }

  }

}
