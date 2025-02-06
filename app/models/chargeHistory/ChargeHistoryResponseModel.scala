/*
 * Copyright 2023 HM Revenue & Customs
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

package models.chargeHistory

import play.api.Logger
import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

sealed trait ChargeHistoryResponseModel

case class ChargesHistoryModel(
    idType:               String,
    idValue:              String,
    regimeType:           String,
    chargeHistoryDetails: Option[List[ChargeHistoryModel]])
    extends ChargeHistoryResponseModel

object ChargesHistoryModel {
  implicit val format: Format[ChargesHistoryModel] = Json.format[ChargesHistoryModel]
}

case class ChargesHistoryErrorModel(code: Int, message: String) extends ChargeHistoryResponseModel

object ChargesHistoryResponse {

  type ChargesHistoryResponse = ChargeHistoryResponseModel

  implicit object ChargesHistoryResponseReads extends HttpReads[ChargesHistoryResponse] {

    override def read(method: String, url: String, response: HttpResponse): ChargesHistoryResponse = {
      response.status match {
        case OK =>
          Logger("application").debug(s"Status: ${response.status}, json: ${response.json}")
          response.json
            .validate[ChargesHistoryModel]
            .fold(
              invalid => {
                Logger("application").error(s"Json Validation Error: $invalid")
                ChargesHistoryErrorModel(
                  INTERNAL_SERVER_ERROR,
                  "Json Validation Error. Parsing ChargeHistory Data Response"
                )
              },
              valid => valid
            )
        case status =>
          if (status == NOT_FOUND || status == FORBIDDEN) {
            Logger("application").info(
              s"No charge history found at url: $url - Status: ${response.status}, body: ${response.body}"
            )
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
    }
  }

}
