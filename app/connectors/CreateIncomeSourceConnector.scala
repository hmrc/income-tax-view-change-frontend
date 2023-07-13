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
import models.createIncomeSource.{CreateBusinessIncomeSourceRequest, CreateForeignPropertyIncomeSource, CreateIncomeSourcesErrorResponse, CreateIncomeSourcesResponse, CreateUKPropertyIncomeSource}
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateIncomeSourceConnector @Inject()(val http: HttpClient,
                                            val appConfig: FrontendAppConfig
                                           )(implicit val ec: ExecutionContext) {


  def createBusinessIncomeSourcesUrl(mtdItid: String): String =
    s"${appConfig.itvcProtectedService}/income-tax-view-change/create-income-source/business/$mtdItid"

  def createBusiness(mtdItid: String, createBusinessIncomeSourcesRequest: CreateBusinessIncomeSourceRequest)
                    (implicit headerCarrier: HeaderCarrier): Future[Either[CreateIncomeSourcesErrorResponse, List[CreateIncomeSourcesResponse]]] = {
    val bodyAsJson = Json.toJson(createBusinessIncomeSourcesRequest)
    val url = createBusinessIncomeSourcesUrl(mtdItid)
    http.POST(url, bodyAsJson).map {
      case response if response.status == OK =>
        response.json.validate[List[CreateIncomeSourcesResponse]].fold(
          _ => {
            Logger("application").error(s"[CreateIncomeSourceConnector][createBusiness] - Json validation error parsing business income sources response, error ${response.body}")
            Left(CreateIncomeSourcesErrorResponse(response.status, s"Not valid json: ${response.body}"))
          },
          valid =>
            Right(valid)
        )
      case response =>
        Logger("application").error(s"[CreateIncomeSourceConnector][createBusiness] - Response status: ${response.status}, body: ${response.body}")
        Left(CreateIncomeSourcesErrorResponse(response.status, s"Error creating incomeSource: ${response.json}"))
    }
  }

  def createForeignProperty(mtdItid: String, createForeignPropertyRequest: CreateForeignPropertyIncomeSource)
                           (implicit headerCarrier: HeaderCarrier): Future[Either[CreateIncomeSourcesErrorResponse, List[CreateIncomeSourcesResponse]]] = {
    val bodyAsJson = Json.toJson(createForeignPropertyRequest)
    val url = createBusinessIncomeSourcesUrl(mtdItid)
    http.POST(url, bodyAsJson).map {
      case response if response.status == OK =>
        response.json.validate[List[CreateIncomeSourcesResponse]].fold(
          _ => {
            Logger("application").error(s"[CreateIncomeSourceConnector][createForeignProperty] - Json validation error parsing business income sources response, error ${response.body}")
            Left(CreateIncomeSourcesErrorResponse(response.status, s"Not valid json: ${response.body}"))
          },
          valid =>
            Right(valid)
        )
      case response =>
        Logger("application").error(s"[CreateIncomeSourceConnector][createForeignProperty] - Response status: ${response.status}, body: ${response.body}")
        Left(CreateIncomeSourcesErrorResponse(response.status, s"Error creating incomeSource: ${response.json}"))
    }
  }

  def createUKProperty(mtdItid: String, createUKPropertyRequest: CreateUKPropertyIncomeSource)
                           (implicit headerCarrier: HeaderCarrier): Future[Either[CreateIncomeSourcesErrorResponse, List[CreateIncomeSourcesResponse]]] = {
    val bodyAsJson = Json.toJson(createUKPropertyRequest)
    val url = createBusinessIncomeSourcesUrl(mtdItid)
    http.POST(url, bodyAsJson).map {
      case response if response.status == OK =>
        response.json.validate[List[CreateIncomeSourcesResponse]].fold(
          _ => {
            Logger("application").error(s"[CreateIncomeSourceConnector][createUKProperty] - Json validation error parsing business income sources response, error ${response.body}")
            Left(CreateIncomeSourcesErrorResponse(response.status, s"Not valid json: ${response.body}"))
          },
          valid =>
            Right(valid)
        )
      case response =>
        Logger("application").error(s"[CreateIncomeSourceConnector][createUKProperty] - Response status: ${response.status}, body: ${response.body}")
        Left(CreateIncomeSourcesErrorResponse(response.status, s"Error creating incomeSource: ${response.json}"))
    }
  }
}