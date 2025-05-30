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

import audit.AuditingService
import config.FrontendAppConfig
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel, PaymentJourneyResponse}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PayApiConnector @Inject()(http: HttpClientV2,
                                auditingService: AuditingService,
                                config: FrontendAppConfig)(implicit ec: ExecutionContext) {

  val journeyStartUrl: String = config.paymentsUrl + "/pay-api/mtd-income-tax/sa/journey/start"

  def startPaymentJourney(saUtr: String, amountInPence: BigDecimal, isAgent: Boolean)(implicit headerCarrier: HeaderCarrier): Future[PaymentJourneyResponse] = {

    val paymentRedirectUrl: String = if (isAgent) config.agentPaymentRedirectUrl else config.paymentRedirectUrl

    val body = Json.parse(
      s"""
         |{
         | "utr": "$saUtr",
         | "amountInPence": $amountInPence,
         | "returnUrl": "$paymentRedirectUrl",
         | "backUrl": "$paymentRedirectUrl"
         |}
      """.stripMargin
    )

    http
      .post(url"$journeyStartUrl")
      .withBody(body)
      .execute[HttpResponse]
      .map {
        case response if response.status == CREATED =>
          response.json.validate[PaymentJourneyModel].fold(
            invalid => {
              Logger("application").error(s"Invalid Json with $invalid")
              PaymentJourneyErrorResponse(response.status, "Invalid Json")
            },
            valid => valid
          )
        case response => if (response.status >= 500) {
            Logger("application").error(s"Payment journey start error with response code: ${response.status} and body: ${response.body}")
          } else {
            Logger("application").warn(s"Payment journey start error with response code: ${response.status} and body: ${response.body}")
          }
          PaymentJourneyErrorResponse(response.status, response.body)
    }
  }
}