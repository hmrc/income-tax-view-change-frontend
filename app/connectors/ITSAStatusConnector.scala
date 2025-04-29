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
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusConnector @Inject()(val http: HttpClientV2,
                                    val appConfig: FrontendAppConfig
                                   )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getITSAStatusDetailUrl(taxableEntityId: String, taxYear: String, futureYears: Boolean, history: Boolean): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/itsa-status/status/$taxableEntityId/$taxYear?futureYears=${futureYears.toString}&history=${history.toString}"
  }

  def getITSAStatusDetail(nino: String, taxYear: String, futureYears: Boolean, history: Boolean)
                         (implicit headerCarrier: HeaderCarrier): Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = {
    val itsaURL = getITSAStatusDetailUrl(nino, taxYear, futureYears, history)

    http.get(url"$itsaURL")
      .transform(_.addHttpHeaders("Accept" -> "application/vnd.hmrc.2.0+json"))
      .execute[HttpResponse] map { response =>
        response.status match {
          case OK =>
            response.json.validate[List[ITSAStatusResponseModel]].fold(
              invalid => {
                Logger("application").error(s"Json validation error parsing itsa-status response, error $invalid")
                Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing itsa-status response"))
              },
              valid => {
                Logger("application").debug(s"Get ITSA Status returned OK with valid response ${valid.toString}")
                Right(valid)
              }
            )
          case NOT_FOUND =>
            Logger("application").debug(s"Get ITSA Status returned NOT_FOUND")
            Right(List())
          case status =>
            if (status >= INTERNAL_SERVER_ERROR) {
              Logger("application").error(s"Response status: ${response.status}, body: ${response.body}")
            } else {
              Logger("application").warn(s"Response status: ${response.status}, body: ${response.body}")
            }
            Left(ITSAStatusResponseError(response.status, response.body))
        }
      } recover { case e: Exception =>
        Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, e.getMessage))
      }
  }
}