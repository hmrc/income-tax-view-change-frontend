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
import models.createIncomeSource._
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateIncomeSourceConnector @Inject()(val http: HttpClient,
                                            val appConfig: FrontendAppConfig
                                           )(implicit val ec: ExecutionContext) {

  def createBusinessIncomeSourcesUrl(mtdItid: String): String =
    s"${appConfig.itvcProtectedService}/income-tax-view-change/create-income-source/business/$mtdItid"

  def createBusiness(mtdItid: String, request: CreateBusinessIncomeSourceRequest)
                        (implicit headerCarrier: HeaderCarrier): Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] = {
    val url = createBusinessIncomeSourcesUrl(mtdItid)
    val jsonRequest = Json.toJson(request)

    http.POST(url, jsonRequest).flatMap(handleResponse)
  }

  def createForeignProperty(mtdItid: String, request: CreateForeignPropertyIncomeSourceRequest)
                    (implicit headerCarrier: HeaderCarrier): Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] = {
    val url = createBusinessIncomeSourcesUrl(mtdItid)
    val jsonRequest = Json.toJson(request)

    http.POST(url, jsonRequest).flatMap(handleResponse)
  }

  def createUKProperty(mtdItid: String, request: CreateUKPropertyIncomeSourceRequest)
                           (implicit headerCarrier: HeaderCarrier): Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] = {
    val url = createBusinessIncomeSourcesUrl(mtdItid)
    val jsonRequest = Json.toJson(request)

    http.POST(url, jsonRequest).flatMap(handleResponse)
  }


  private def handleResponse(response: HttpResponse): Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] = {
    if (response.status == OK) {
      response.json.validate[List[CreateIncomeSourceResponse]].fold(
        errors => {
          Logger("application").error(s"[CreateIncomeSourceConnector][handleResponse] - Json validation error parsing business income sources response, error ${JsError.toJson(errors)}")
          Future.successful(Left(CreateIncomeSourceErrorResponse(response.status, s"Not valid json: ${response.body}")))
        },
        valid => Future.successful(Right(valid))
      )
    } else {
      Logger("application").error(s"[CreateIncomeSourceConnector][handleResponse] - Response status: ${response.status}, body: ${response.body}")
      Future.successful(Left(CreateIncomeSourceErrorResponse(response.status, s"Error creating incomeSource: ${response.json}")))
    }
  }
}