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

  private def createRequest(businessDetails: BusinessDetails): JsValue = {
    val requestObject = AddBusinessIncomeSourcesRequest(businessDetails = Some(
      List(businessDetails)
    ))
    Json.toJson(requestObject)
  }

  def create(mtdItid: String, businessDetails: BusinessDetails)(implicit headerCarrier: HeaderCarrier): Future[Either[CreateBusinessErrorResponse, List[IncomeSourceResponse]]] = {
    val body = createRequest(businessDetails)
    val url = addBusinessDetailsUrl(mtdItid)
    http.POST(url, body).map { response =>
      response.status match {
        case OK =>
          response.json.validateOpt[List[IncomeSourceResponse]].fold(
            _ => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][create] - Json validation error parsing repayment response, error ${response.body}")
              Left(CreateBusinessErrorResponse(response.status, s"Not valid json: ${response.body}"))
            },
            valid => valid match {
              case Some(validJson) =>
                Right(validJson)
              case None =>
                Left(CreateBusinessErrorResponse(response.status, s"Not valid json: ${response.body}"))
            }
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