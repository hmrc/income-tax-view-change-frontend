/*
 * Copyright 2025 HM Revenue & Customs
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

import config.FrontendAppConfig
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel, OutstandingChargesResponseModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OutstandingChargesConnector @Inject()(
                                            httpV2: HttpClientV2,
                                            appConfig: FrontendAppConfig
                                          )(implicit val ec: ExecutionContext) extends RawResponseReads {

  private[connectors] val baseUrl = s"${appConfig.itvcProtectedService}/income-tax-view-change"

  private[connectors] def getOutstandingChargesUrl(idType: String, idNumber: String, taxYear: String): String =
    baseUrl + s"/out-standing-charges/$idType/$idNumber/$taxYear"

  def getOutstandingCharges(idType: String, idNumber: String, taxYear: String)
                           (implicit headerCarrier: HeaderCarrier): Future[OutstandingChargesResponseModel] = {


    val url = getOutstandingChargesUrl(idType, idNumber, s"$taxYear-04-05")
    Logger("application").debug(s"GET $url")

    httpV2
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").debug(s"Status: ${response.status}, json: ${response.json}")
            response.json.validate[OutstandingChargesModel].fold(
              invalid => {
                Logger("application").error(s"Json Validation Error: $invalid")
                OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing OutstandingCharges Data Response")
              },
              valid => valid
            )
          case status if status >= Status.INTERNAL_SERVER_ERROR =>
            Logger("application").error(s"Status: ${response.status}, body: ${response.body}")
            OutstandingChargesErrorModel(response.status, response.body)
          case _ =>
            Logger("application").warn(s"Status: ${response.status}, body: ${response.body}")
            OutstandingChargesErrorModel(response.status, response.body)
        }
      }.recover {
        case ex =>
          Logger("application").error(s"Unexpected failure, ${ex.getMessage}", ex)
          OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
      }
  }


}
