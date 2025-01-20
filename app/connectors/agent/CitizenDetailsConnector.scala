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

package connectors.agent

import config.FrontendAppConfig
import connectors.RawResponseReads
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel, CitizenDetailsResponseModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CitizenDetailsConnector @Inject()(val http: HttpClientV2,
                                        val config: FrontendAppConfig
                                       )(implicit ec: ExecutionContext) extends RawResponseReads {

  private[connectors] lazy val getCitizenDetailsBySaUtrUrl: String => String = saUtr => s"${config.citizenDetailsUrl}/citizen-details/sautr/$saUtr"

  private def updateHeaderCarrier(request: RequestBuilder): RequestBuilder = if (config.hasEnabledTestOnlyRoutes) {
    request.setHeader(HeaderNames.trueClientIp -> "ITVC")
  } else {
    request
  }

  def getCitizenDetailsBySaUtr(saUtr: String)(implicit headerCarrier: HeaderCarrier): Future[CitizenDetailsResponseModel] = {

    val url = getCitizenDetailsBySaUtrUrl(saUtr)

    Logger("application").debug(s"GET $url")

    updateHeaderCarrier(
      http.get(url"$url")
    ).transform(_.withRequestTimeout(Duration(10, SECONDS))).execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[CitizenDetailsModel].fold(
            invalid => {
              Logger("application").error(s"Json Validation Error. Parsing Citizen Details Response. Invalid=$invalid")
              CitizenDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Citizen Details response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").error(s"RESPONSE status: ${response.status}, URL: ${url} body: ${response.body}")
          }
          CitizenDetailsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"Unexpected future failed error, ${ex.getMessage}")
        CitizenDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed, ${ex.getMessage}")
    }
  }
}
