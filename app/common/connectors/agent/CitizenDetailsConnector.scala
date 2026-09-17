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

package common.connectors.agent

import common.config.FrontendAppConfig
import common.connectors.RawResponseReads
import common.models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel, CitizenDetailsResponseModel}
import play.api.Logging
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CitizenDetailsConnector @Inject()(val http: HttpClientV2,
                                        val config: FrontendAppConfig
                                       )(implicit ec: ExecutionContext) extends RawResponseReads with Logging {

  private[connectors] lazy val getCitizenDetailsBySaUtrUrl: String => String = saUtr => s"${config.citizenDetailsUrl}/citizen-details/sautr/$saUtr"

  private def updateHeaderCarrier(request: RequestBuilder, utr: String): RequestBuilder = if (config.hasEnabledTestOnlyRoutes) {
    request.setHeader(HeaderNames.trueClientIp -> s"ITVC-$utr")
  } else {
    request
  }

  def getCitizenDetailsBySaUtr(saUtr: String)(implicit headerCarrier: HeaderCarrier): Future[CitizenDetailsResponseModel] = {

    val url = getCitizenDetailsBySaUtrUrl(saUtr)

    logger.debug(s"GET $url")

    updateHeaderCarrier(http.get(url"$url"), saUtr).execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          logger.debug(s"[getCitizenDetailsBySaUtr] RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[CitizenDetailsModel].fold(
            invalid => {
              logger.error(s"Json Validation Error. Parsing Citizen Details Response. Invalid=$invalid")
              CitizenDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Citizen Details response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            logger.error(s"[getCitizenDetailsBySaUtr] RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            logger.warn(s"[getCitizenDetailsBySaUtr] RESPONSE status: ${response.status}, body: ${response.body}")
          }
          CitizenDetailsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        logger.error(s"Unexpected future failed error, ${ex.getMessage}")
        CitizenDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed, ${ex.getMessage}")
    }
  }
}
