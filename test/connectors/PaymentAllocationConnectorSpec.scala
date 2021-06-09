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

import assets.BaseTestConstants.testNino
import assets.PaymentAllocationChargesTestConstants.{paymentAllocationChargesModel, paymentAllocationChargesModelMultiplePayments}
import config.FrontendAppConfig
import connectors.agent.mocks.MockHttp
import models.paymentAllocationCharges.{PaymentAllocationChargesErrorModel, PaymentAllocationChargesResponse}
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json.Json
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class PaymentAllocationConnectorSpec extends TestSupport with MockHttp {

  class Setup(mockResponse: HttpResponse) {
    val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
    val connector = new PaymentAllocationConnector(mockHttpGet, appConfig)

    val docNumber: String = "XM0026100122"
    val baseUrl = "http://localhost:9999"

    when(appConfig.itvcProtectedService) thenReturn baseUrl

    setupMockHttpGet(connector.getPaymentAllocationUrl(testNino,docNumber))(mockResponse)

  }

  "PaymentAllocationConnector .getPaymentAllocation" should {

    "a payment allocation" when {
      val successResponse = HttpResponse(OK, Some(Json.toJson(paymentAllocationChargesModel)))
      val successResponseMultiplePayments = HttpResponse(OK, Some(Json.toJson(paymentAllocationChargesModelMultiplePayments)))

      "receiving an OK with only one valid data item" in new Setup(successResponse) {

        val result: Future[PaymentAllocationChargesResponse] = connector.getPaymentAllocation(testNino, docNumber)
        await(result) shouldBe paymentAllocationChargesModel
      }

      "receiving an OK with multiple valid data items" in new Setup(successResponseMultiplePayments) {
        val result: Future[PaymentAllocationChargesResponse] = connector.getPaymentAllocation(testNino, docNumber)
        await(result) shouldBe paymentAllocationChargesModelMultiplePayments
      }
    }

    "return a NOT FOUND payment allocation error" when {

      "receiving a not found response" in new Setup(HttpResponse(NOT_FOUND, Some(Json.toJson("Error message")))) {

        val result: Future[PaymentAllocationChargesResponse] = connector.getPaymentAllocation(testNino, docNumber)
        await(result) shouldBe PaymentAllocationChargesErrorModel(404, """"Error message"""")
      }
    }

    "return an INTERNAL_SERVER_ERROR payment allocation error" when {

      "receiving a 500+ response" in new Setup(HttpResponse(SERVICE_UNAVAILABLE, Some(Json.toJson("Error message")))) {

        val result: Future[PaymentAllocationChargesResponse] = connector.getPaymentAllocation(testNino, docNumber)
        await(result) shouldBe PaymentAllocationChargesErrorModel(503, """"Error message"""")
      }

      "receiving a 400- response" in new Setup(HttpResponse(BAD_REQUEST,Some(Json.toJson("Error message")))) {

        val result: Future[PaymentAllocationChargesResponse] = connector.getPaymentAllocation(testNino, docNumber)
        await(result) shouldBe PaymentAllocationChargesErrorModel(400, """"Error message"""")
      }
    }
  }
}
