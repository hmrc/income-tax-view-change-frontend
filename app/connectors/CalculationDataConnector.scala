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
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier, HttpGet}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CalculationDataConnector @Inject()(val http: HttpGet) extends ServicesConfig with RawResponseReads {

  lazy val calculationDataUrl: String = baseUrl("self-assessment-api")
  lazy val getCalculationDataUrl: (String, String) => String = (nino, taxCalculationId) => s"$calculationDataUrl/ni/$nino/calculations/$taxCalculationId"

  def getCalculationData(nino: String, taxCalculationId: String)(implicit headerCarrier: HeaderCarrier): Future[CalculationDataResponseModel] = {

    val url = getCalculationDataUrl(nino, taxCalculationId)
    Logger.debug(s"[CalculationDataConnector][getCalculationData] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json")) flatMap {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[CalculationDataConnector][getCalculationData] - RESPONSE status: ${response.status}, json: ${response.json}")
            Future.successful(response.json.validate[CalculationDataModel].fold(
              invalid => {
                Logger.warn(s"[CalculationDataConnector][getCalculationData] - Json Constraints Error. Parsing Calc Breakdown Response. Invalid=$invalid")
                CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Constraints Error. Parsing Calc Breakdown Response")
              },
              valid => valid
            ))
          case _ =>
            Logger.debug(s"[CalculationDataConnector][getCalculationData] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[CalculationDataConnector][getCalculationData] - Response status: [${response.status}] returned from Calc Breakdown call")
            Future.successful(CalculationDataErrorModel(response.status, response.body))
        }
    } recoverWith {
      case _ =>
        Logger.warn(s"[CalculationDataConnector][getCalculationData] - Unexpected future failed error")
        Future.successful(CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error"))
    }
  }

}
