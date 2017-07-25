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

import config.FrontendAppConfig
import models._
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import play.api.http.Status.OK

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BusinessObligationDataConnector @Inject()(val http: HttpGet,
                                                val frontendAppConfig: FrontendAppConfig) extends ServicesConfig with RawResponseReads {

  lazy val obligationDataUrl: String = frontendAppConfig.selfAssessmentApi
  lazy val getObligationDataUrl: (String, String) => String = (nino, selfEmploymentId) =>
    s"$obligationDataUrl/ni/$nino/self-employments/$selfEmploymentId/obligations"

  def getBusinessObligationData(nino: String, selfEmploymentId: String)(implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] = {

    val url = getObligationDataUrl(nino, selfEmploymentId)

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json")) flatMap {
      response =>
        response.status match {
          case OK => Logger.debug(s"[BusinessObligationDataConnector][getObligationData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(response.json.validate[ObligationsModel].fold(
              invalid => {
                Logger.warn(s"[BusinessObligationDataConnector][getObligationData] - Json Validation Error. Parsing Obligation Data Response")
                ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Obligation Data Response.")
              },
              valid => valid
            ))
          case _ =>
            Logger.warn(s"[BusinessObligationDataConnector][getObligationData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(ObligationsErrorModel(response.status, response.body))
        }
    } recoverWith {
      case _ =>
        Logger.warn(s"[BusinessDetailsConnector][getBusinessList] - Unexpected future failed error when calling $url.")
        Future.successful(ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error when calling $url."))
    }
  }

}
