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
import models.claimToAdjustPoa.ClaimToAdjustPoaRequest
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse._
import models.core.CorrelationId
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import play.api.libs.ws.writeableOf_JsValue
import javax.inject.Inject
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}


class ClaimToAdjustPoaConnector @Inject() ( frontendAppConfig: FrontendAppConfig,
                                            val http: HttpClientV2)
                                          ( implicit val ec: ExecutionContext ) {

  val endpoint = s"${frontendAppConfig.itvcProtectedService}/income-tax-view-change/submit-claim-to-adjust-poa"

  def postClaimToAdjustPoa(request: ClaimToAdjustPoaRequest)(implicit hc: HeaderCarrier
  ): Future[ClaimToAdjustPoaResponse] = {

    val correlationId = CorrelationId.fromHeaderCarrier(hc)
      .getOrElse(CorrelationId())

    http
      .post(url"$endpoint")
      .setHeader(correlationId.asHeader())
      .withBody(Json.toJson(request))
      .transform(_.withRequestTimeout(Duration(frontendAppConfig.claimToAdjustTimeout, SECONDS)))
      .execute[ClaimToAdjustPoaResponse]
      .recover {
        case e => {
          Logger("application").error(e.getMessage)
          Left(UnexpectedError)
        }
      }
  }
}
