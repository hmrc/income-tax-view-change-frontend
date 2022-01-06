/*
 * Copyright 2022 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel, CitizenDetailsResponseModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CitizenDetailsConnector @Inject()(val http: HttpClient,
                                        val config: FrontendAppConfig
                                       )(implicit ec: ExecutionContext) extends RawResponseReads {

  private[connectors] lazy val getCitizenDetailsBySaUtrUrl: String => String = saUtr => s"${config.citizenDetailsUrl}/citizen-details/sautr/$saUtr"

  def updatedHeaderCarrier(hc: HeaderCarrier): HeaderCarrier = if(config.hasEnabledTestOnlyRoutes) {
    hc.copy(trueClientIp = Some("ITVC"))
  } else {
    hc
  }

  def getCitizenDetailsBySaUtr(saUtr: String)(implicit headerCarrier: HeaderCarrier):Future[CitizenDetailsResponseModel] = {

    val url = getCitizenDetailsBySaUtrUrl(saUtr)

    Logger("application").debug(s"[CitizenDetailsConnector][getCitizenDetailsBySaUtr] - GET $url")

    http.GET[HttpResponse](url)(implicitly, updatedHeaderCarrier(headerCarrier), implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[CitizenDetailsConnector][getCitizenDetailsBySaUtr] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[CitizenDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[CitizenDetailsConnector][getCitizenDetailsBySaUtr] - Json Validation Error. Parsing Citizen Details Response. Invalid=$invalid")
              CitizenDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Citizen Details response")
            },
            valid => valid
          )
        case status =>
          if(status >= 500) {
            Logger("application").error(s"[CitizenDetailsConnector][getCitizenDetailsBySaUtr] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[CitizenDetailsConnector][getCitizenDetailsBySaUtr] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          CitizenDetailsErrorModel(response.status, response.body)
        }
    } recover {
      case ex =>
        Logger("application").error(s"[CitizenDetailsConnector][getCitizenDetailsBySaUtr] - Unexpected future failed error, ${ex.getMessage}")
        CitizenDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed, ${ex.getMessage}")
    }
  }
}
