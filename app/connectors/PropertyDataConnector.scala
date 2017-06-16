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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PropertyDataConnector @Inject()(val http: HttpGet) extends ServicesConfig with RawResponseReads {

  lazy val propertyDataUrl: String = baseUrl("self-assessment-api")
  lazy val getPropertyDataUrl: String => String = nino => s"$propertyDataUrl/self-assessment/ni/$nino/uk-properties/obligations"

  def getPropertyData(nino: String)(implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] = {

    val url = getPropertyDataUrl(nino)

    http.GET[HttpResponse](url) flatMap {
      response =>
        response.status match {
          case OK => Logger.debug(s"[PropertyDataConnector][getPropertyData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(response.json.validate[ObligationsModel].fold(
              invalid => {
                Logger.warn(s"[PropertyDataConnector][getPropertyData] - Json Validation Error. Parsing Property Obligation Data Response")
                ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Property Obligation Data Response.")
              },
              valid => valid
            ))
          case _ =>
            Logger.warn(s"[PropertyDataConnector][getPropertyData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(ObligationsErrorModel(response.status, response.body))
        }
    } recoverWith {
      case _ =>
        Logger.warn(s"[PropertyDataConnector][getPropertyData] - Unexpected future failed error when calling $url.")
        Future.successful(ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error when calling $url."))
    }
  }

}
