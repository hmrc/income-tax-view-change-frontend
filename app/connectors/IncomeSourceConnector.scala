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

package connectors

import config.FrontendAppConfig
import models.addIncomeSource._
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceConnector @Inject()(val http: HttpClient,
                                      val appConfig: FrontendAppConfig
                                     )(implicit val ec: ExecutionContext) {


  def addBusinessDetailsUrl(authTag: String): String = s"${appConfig.itvcProtectedService}/income-tax-view-change/create-income-source/business/$authTag"

  def create(mtdItid: String, addBusinessIncomeSourcesRequest: AddBusinessIncomeSourcesRequest)(implicit headerCarrier: HeaderCarrier): Future[Either[CreateBusinessErrorResponse, List[IncomeSourceResponse]]] = {
    val bodyAsJson = Json.toJson(addBusinessIncomeSourcesRequest)
    println(s"Here is a body: ${bodyAsJson}")
    val url = addBusinessDetailsUrl(mtdItid)
    http.POST(url, bodyAsJson).map { response =>
      response.status match {
        case OK =>
          response.json.validate[List[IncomeSourceResponse]].fold(
            _ => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][create] - Json validation error parsing repayment response, error ${response.body}")
              Left(CreateBusinessErrorResponse(response.status, s"Not valid json: ${response.body}"))
            },
            valid =>
              Right(valid)
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][create] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][create] - Response status: ${response.status}, body: ${response.body}")
          }
          Left(CreateBusinessErrorResponse(response.status, s"Error creating incomeSource: ${response.json}"))
      }
    }
  }

}