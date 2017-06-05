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

import javax.inject.{Singleton, Inject}

import models.{ErrorResponse, SuccessResponse, ConnectorResponseModel}
import play.api.Logger
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier, HttpGet}
import play.api.http.Status.OK
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

@Singleton
class ObligationDataConnector @Inject()(val http: HttpGet) extends ServicesConfig with RawResponseReads {

  lazy val obligationDataUrl: String = baseUrl("self-assessment-api")
  lazy val getObligationDataUrl: String => String = nino => s"$obligationDataUrl/self-assessment/ni/$nino/self-employments"

  def getObligationData(nino: String)(implicit headerCarrier: HeaderCarrier): Future[ConnectorResponseModel] = {

    val url = getObligationDataUrl(nino)

    http.GET[HttpResponse](url) flatMap {
      response =>
        response.status match {
          case OK => Logger.debug(s"[ObligationDataConnector][getObligationData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(SuccessResponse(response.json))
          case _ => Logger.warn(s"[ObligationDataConnector][getObligationData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(ErrorResponse(response.status, response.body))
        }
    }

  }

}
