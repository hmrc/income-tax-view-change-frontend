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
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import models.core.{RepaymentJourneyResponseModel, RepaymentRefund, ViewHistory}
import play.api.Logger
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, UNAUTHORIZED}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class RepaymentConnector @Inject()(
                                    httpClient: HttpClientV2,
                                    config: FrontendAppConfig
                                  )(implicit ec: ExecutionContext) {

  private[connectors] val startRefundUrl: String = s"${config.repaymentsUrl}/self-assessment-refund-backend/itsa-viewer/journey/start-refund"
  private[connectors] val viewRefundUrl: String = s"${config.repaymentsUrl}/self-assessment-refund-backend/itsa-viewer/journey/view-history"

  def start(nino: String, fullAmount: BigDecimal)(implicit headerCarrier: HeaderCarrier): Future[RepaymentJourneyResponseModel] = {

    val body = Json.toJson[RepaymentRefund](RepaymentRefund(nino, fullAmount))

    httpClient
      .post(url"$startRefundUrl")
      .withBody(body)
      .execute[HttpResponse].map {
        case response if response.status == ACCEPTED =>
          response.json.validate[RepaymentJourneyModel].fold(
            invalidJson => {
              Logger("application").error(s"Invalid Json with $invalidJson")
              RepaymentJourneyErrorResponse(response.status, "Invalid Json")
            },
            identity
          )
        case response if response.status == UNAUTHORIZED =>
          Logger("application").error(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
          RepaymentJourneyErrorResponse(response.status, response.body)
        case response if response.status >= INTERNAL_SERVER_ERROR =>
          Logger("application").error(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
          RepaymentJourneyErrorResponse(response.status, response.body)
        case response =>
          Logger("application").warn(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
          RepaymentJourneyErrorResponse(response.status, response.body)
      }
  }

  def view(nino: String)(implicit headerCarrier: HeaderCarrier): Future[RepaymentJourneyResponseModel] = {

    val body = Json.toJson[ViewHistory](ViewHistory(nino))

    httpClient
      .post(url"$viewRefundUrl")
      .withBody(body)
      .execute[HttpResponse].map {
        case response if response.status == ACCEPTED =>
          response.json.validate[RepaymentJourneyModel].fold(
            invalidJson => {
              Logger("application").error(s"Invalid Json with $invalidJson")
              RepaymentJourneyErrorResponse(response.status, "Invalid Json")
            },
            identity
          )
        case response if response.status == UNAUTHORIZED =>
          Logger("application").error(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
          RepaymentJourneyErrorResponse(response.status, response.body)
        case response if response.status >= INTERNAL_SERVER_ERROR =>
          Logger("application").error(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
          RepaymentJourneyErrorResponse(response.status, response.body)
        case response =>
          Logger("application").warn(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
          RepaymentJourneyErrorResponse(response.status, response.body)
      }
  }
}
