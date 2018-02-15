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
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.ImplicitDateFormatter

import scala.concurrent.Future

@Singleton
class PropertyDetailsConnector @Inject()(val http: HttpClient,
                                         val config: FrontendAppConfig) extends RawResponseReads with ImplicitDateFormatter {

  lazy val getPropertyDetailsUrl: String => String = nino => s"${config.saApiService}/ni/$nino/uk-properties"

  def getPropertyDetails(nino: String)(implicit headerCarrier: HeaderCarrier): Future[PropertyDetailsResponseModel] = {

    val url = getPropertyDetailsUrl(nino)
    Logger.debug(s"[PropertyDetailsConnector][getPropertyDetails] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[PropertyDetailsConnector][getPropertyDetails] - RESPONSE status: ${response.status}, json: ${response.json}")
            featureSwitchResponse(response.json)
          case NOT_FOUND =>
            Logger.debug(s"[PropertyDetailsConnector][getPropertyDetails] - RESPONSE status: ${response.status}, json: ${response.json}")
            NoPropertyIncomeDetails
          case _ =>
            Logger.debug(s"[PropertyDetailsConnector][getPropertyDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[PropertyDetailsConnector][getPropertyDetails] - Response status: [${response.status}] returned from Property Details call")
            PropertyDetailsErrorModel(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn(s"[PropertyDetailsConnector][getPropertyDetails] - Unexpected future failed error")
        PropertyDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }

  def featureSwitchResponse(json: JsValue): PropertyDetailsResponseModel =
    if(config.features.propertyDetailsEnabled()) {
      json.validate[PropertyDetailsModel].fold(
        invalid => {
          Logger.warn(s"[PropertyDetailsConnector][getPropertyDetails] - Failed to parse JSON body of response to PropertyDetailsModel.")
          Logger.debug(s"[PropertyDetailsConnector][getPropertyDetails] - Json validation error: $invalid")
          PropertyDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Failed to parse JSON body of response to PropertyDetailsModel.")
        },
        valid => valid
      )
    } else {
      Logger.debug(s"[PropertyDetailsConnector][getPropertyDetails] - Property Details Feature disabled, returning dummy Accounting Period")
      PropertyDetailsModel(accountingPeriod = AccountingPeriodModel("2017-04-06", "2018-04-05"))
    }
}
