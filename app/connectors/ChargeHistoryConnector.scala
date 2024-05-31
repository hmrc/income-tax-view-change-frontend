/*
 * Copyright 2024 HM Revenue & Customs
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
import models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChargeHistoryConnector @Inject()(val http: HttpClient,
                                       val appConfig: FrontendAppConfig
                                      )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getChargeHistoryUrl(mtdBsa: String, chargeReference: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/charge-history/$mtdBsa/chargeReference/$chargeReference"
  }

  def getChargeHistory(mtdBsa: String, chargeRef: Option[String])
                      (implicit headerCarrier: HeaderCarrier): Future[ChargeHistoryResponseModel] = {
    chargeRef match {
      case Some(chargeReference) => val url = getChargeHistoryUrl(mtdBsa, chargeReference)
        Logger("application").debug(s"GET $url")

        http.GET[HttpResponse](url) map { response =>
          response.status match {
            case OK =>
              Logger("application").debug(s"Status: ${response.status}, json: ${response.json}")
              response.json.validate[ChargesHistoryModel].fold(
                invalid => {
                  Logger("application").error(s"Json Validation Error: $invalid")
                  ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing ChargeHistory Data Response")
                },
                valid => valid
              )
            case status =>
              if (status == 404 || status == 403) {
                Logger("application").info(s"No charge history found for $chargeReference - Status: ${response.status}, body: ${response.body}")
                ChargesHistoryModel("", "", "", None)
              } else {
                if (status >= 500) {
                  Logger("application").error(s"Status: ${response.status}, body: ${response.body}")
                } else {
                  Logger("application").warn(s"Status: ${response.status}, body: ${response.body}")
                }
                ChargesHistoryErrorModel(response.status, response.body)
              }
          }
        } recover {
          case ex =>
            Logger("application").error(s"Unexpected failure, ${ex.getMessage}", ex)
            ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
        }
      case None => Logger("application").error("No charge reference value supplied")
        Future(ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, "No charge reference value supplied"))
    }

  }

}
