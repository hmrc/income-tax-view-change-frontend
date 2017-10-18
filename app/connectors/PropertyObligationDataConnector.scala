/*
 * Copyright 2017 HM Revenue & Customs
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

import models._
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.play.config.ServicesConfig

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpResponse }

@Singleton
class PropertyObligationDataConnector @Inject()(val http: HttpGet) extends ServicesConfig with RawResponseReads {

  lazy val propertyDataUrl: String = baseUrl("self-assessment-api")
  lazy val getPropertyDataUrl: String => String = nino => s"$propertyDataUrl/ni/$nino/uk-properties/obligations"

  def getPropertyObligationData(nino: String)(implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] = {

    val url = getPropertyDataUrl(nino)
    Logger.debug(s"[PropertyObligationDataConnector][getPropertyData] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[PropertyObligationDataConnector][getPropertyData] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[ObligationsModel].fold(
              invalid => {
                Logger.warn(s"[PropertyObligationDataConnector][getPropertyData] - Json Validation Error. Parsing Property Obligation Data Response")
                ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Property Obligation Data Response")
              },
              valid => valid
            )
          case _ =>
            Logger.debug(s"[PropertyObligationDataConnector][getPropertyData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[PropertyObligationDataConnector][getPropertyData] - Response status: [${response.status}] returned from Property Obligations call")
            ObligationsErrorModel(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn(s"[PropertyObligationDataConnector][getPropertyData] - Unexpected future failed error")
        ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }
}
