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
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusConnector @Inject()(val http: HttpClient,
                                    val appConfig: FrontendAppConfig
                                   )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getITSAStatusDetailUrl(taxableEntityId: String, taxYear: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/itsa-status/status/$taxableEntityId/$taxYear"
  }

  def getITSAStatusDetail(nino: String, taxYear: String, futureYears: Boolean, history: Boolean)
                         (implicit headerCarrier: HeaderCarrier): Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = {
    val url = getITSAStatusDetailUrl(nino, taxYear)
    val queryParams = Seq("futureYears" -> futureYears.toString, "history" -> history.toString)
    http.GET[HttpResponse](url = url, queryParams = queryParams)(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[List[ITSAStatusResponseModel]].fold(
            invalid => {
              Logger("application").error(s"Json validation error parsing itsa-status response, error $invalid")
              Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing itsa-status response"))
            },
            valid => Right(valid)
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"Response status: ${response.status}, body: ${response.body}")
          }
          Left(ITSAStatusResponseError(response.status, response.body))
      }
    }
  }
}