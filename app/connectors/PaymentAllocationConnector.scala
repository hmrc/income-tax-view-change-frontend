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

import config.FrontendAppConfig
import models.paymentAllocationCharges.{PaymentAllocationChargesErrorModel, PaymentAllocationChargesModel, PaymentAllocationChargesResponse}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}

class PaymentAllocationConnector(val http: HttpClient,
                                 val config: FrontendAppConfig) extends RawResponseReads {

  def getPaymentAllocationUrl(nino: String, documentNumber: String): String = {
    s"${config.itvcProtectedService}/income-tax-view-change/$nino/financial-details/charges/documentId/$documentNumber"
  }

  def getPaymentAllocation(nino: String, documentNumber: String)(implicit headerCarrier: HeaderCarrier,
                                                          ec: ExecutionContext): Future[PaymentAllocationChargesResponse] = {
    http.GET[HttpResponse](getPaymentAllocationUrl(nino, documentNumber))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[PaymentAllocationChargesModel].fold(
            invalid => {
              Logger.error(s"[PaymentAllocationConnector][getCalculation] - Json validation error parsing calculation response, error $invalid")
              PaymentAllocationChargesErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger.error(s"[PaymentAllocationConnector][getCalculation] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger.warn(s"[PaymentAllocationConnector][getCalculation] - Response status: ${response.status}, body: ${response.body}")
          }
          PaymentAllocationChargesErrorModel(response.status, response.body)
      }
    }
  }

}
