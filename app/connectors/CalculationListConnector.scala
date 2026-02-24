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
import models.calculationList.{CalculationListErrorModel, CalculationListModel, CalculationListResponseModel}
import models.core.Nino
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationListConnector @Inject()(val http: HttpClientV2,
                                         val appConfig: FrontendAppConfig
                                        )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getLegacyCalculationListUrl(nino: String, taxYearEnd: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/list-of-calculation-results/$nino/$taxYearEnd"
  }

  def getCalculationListUrl(nino: String, taxYearRange: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/calculation-list/$nino/$taxYearRange"
  }

  def getLegacyCalculationList(nino: String, taxYearEnd: String)(implicit headerCarrier: HeaderCarrier): Future[CalculationListResponseModel] = {

    http.get(url"${getLegacyCalculationListUrl(nino, taxYearEnd)}")
      .setHeader("Accept" -> "application/vnd.hmrc.2.0+json")
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          response.json.validate[CalculationListModel].fold(
            invalid => {
              Logger("application").error("" +
                s"Json validation error parsing legacy calculation list response, error $invalid")
              CalculationListErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing legacy calculation list response")
            },
            valid => valid
          )
        case status if status >= INTERNAL_SERVER_ERROR =>
          Logger("application").error(s"Response status: ${response.status}, body: ${response.body}")
          CalculationListErrorModel(response.status, response.body)
        case _ =>
          Logger("application").warn(s"Response status: ${response.status}, body: ${response.body}")
          CalculationListErrorModel(response.status, response.body)
      }
    }
  }

  def getCalculationList(nino: Nino, taxYearRange: String)
                        (implicit headerCarrier: HeaderCarrier): Future[CalculationListResponseModel] = {

    val url = getCalculationListUrl(nino.value, taxYearRange)

    http.get(url"$url")
      .setHeader("Accept" -> "application/vnd.hmrc.2.0+json")
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          response.json.validate[CalculationListModel].fold(
            invalid => {
              Logger("application").error("" +
                s"Json validation error parsing calculation list response, error $invalid")
              CalculationListErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation list response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"Response status: ${response.status}, body: ${response.body}")
          }
          CalculationListErrorModel(response.status, response.body)
      }
    }
  }
}