/*
 * Copyright 2021 HM Revenue & Customs
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
import implicits.ImplicitJsonValidationFormatter._
import javax.inject.Inject
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel, PaymentJourneyResponse}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class PayApiConnector @Inject()(val http: HttpClient,
                                val auditingService: AuditingService,
                                val config: FrontendAppConfig)(implicit ec: ExecutionContext) {

  val url: String = config.paymentsUrl + "/pay-api/mtd-income-tax/sa/journey/start"

  def startPaymentJourney(saUtr: String, amountInPence: BigDecimal)(implicit headerCarrier: HeaderCarrier): Future[PaymentJourneyResponse] = {

    val body = Json.parse(
      s"""
         |{
         | "utr": "$saUtr",
         | "amountInPence": $amountInPence,
         | "returnUrl": "${config.paymentRedirectUrl}",
         | "backUrl": "${config.paymentRedirectUrl}"
         |}
      """.stripMargin
    )

    http.POST(url, body).map {
      case response if response.status == CREATED =>
        response.json.validate[PaymentJourneyModel].fold(
          invalid => {
            Logger("application").error(s"Invalid Json with $invalid")
            PaymentJourneyErrorResponse(response.status, "Invalid Json")
          },
          valid => valid
        )
      case response =>
        if(response.status >= 500) {
          Logger("application").error(s"Payment journey start error with response code: ${response.status} and body: ${response.body}")
        } else {
          Logger("application").warn(s"Payment journey start error with response code: ${response.status} and body: ${response.body}")
        }
        PaymentJourneyErrorResponse(response.status, response.body)
    }
  }
}