/*
 * Copyright 2018 HM Revenue & Customs
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

import config.FrontendAppConfig
import models._
import play.api.Logger
import play.api.http.Status
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class NinoLookupConnector @Inject()(val http: HttpClient,
                                    val config: FrontendAppConfig) extends RawResponseReads {

  private[connectors] lazy val getNinoLookupUrl: (String) => String = (mtdRef) =>
    s"${config.itvcProtectedService}/income-tax-view-change/nino-lookup/$mtdRef"

  def getNino(mtdRef: String)(implicit headerCarrier: HeaderCarrier): Future[NinoResponse] = {

    val url = getNinoLookupUrl(mtdRef)
    Logger.debug(s"[NinoLookupConnector][getNino] - GET $url")

    http.GET[HttpResponse](url) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[NinoLookupConnector][getNino] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[Nino].fold(
              invalid => {
                Logger.warn(s"[NinoLookupConnector][getNino] - Json Validation Error. Parsing Nino Response")
                NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
              },
              valid => valid
            )
          case _ =>
            Logger.debug(s"[NinoLookupConnector][getNino] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[NinoLookupConnector][getNino] - Response status: [${response.status}] from Get Nino call")
            NinoResponseError(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn(s"[NinoLookupConnector][getNino] - Unexpected future failed error")
        NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }
}
