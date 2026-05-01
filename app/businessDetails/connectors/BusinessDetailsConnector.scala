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

package businessDetails.connectors

import config.FrontendAppConfig
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessDetailsConnector @Inject()(
                                          httpClient: HttpClientV2,
                                          appConfig: FrontendAppConfig
                                        )(implicit val ec: ExecutionContext) {

  private[connectors] def getBusinessDetailsUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/get-business-details/nino/$nino"
  }

  def getBusinessDetails(nino: String)(implicit headerCarrier: HeaderCarrier): Future[IncomeSourceDetailsResponse] = {
    val url = getBusinessDetailsUrl(nino)
    Logger("application").debug(s"GET $url")

    httpClient
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").debug(s"[FE Business Details Connector][getBusinessDetails] RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[IncomeSourceDetailsModel].fold(
              invalid => {
                Logger("application").error(s"$invalid")
                IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
              },
              valid => valid
            )
          case status if status >= 500 =>
            Logger("application").error(s"[FE Business Details Connector][getBusinessDetails] RESPONSE status: ${response.status}, body: ${response.body}")
            IncomeSourceDetailsError(response.status, response.body)
          case _ =>
            Logger("application").warn(s"[FE Business Details Connector][getBusinessDetails] RESPONSE status: ${response.status}, body: ${response.body}")
            IncomeSourceDetailsError(response.status, response.body)
        }
      }.recover {
        case ex =>
          Logger("application").error(s"[FE Business Details Connector][getBusinessDetails] Unexpected future failed error, ${ex.getMessage}")
          IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      }
  }
}