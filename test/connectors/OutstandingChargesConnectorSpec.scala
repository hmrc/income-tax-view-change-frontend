/*
 * Copyright 2025 HM Revenue & Customs
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
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesResponseModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Configuration
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.OutstandingChargesTestConstants._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class OutstandingChargesConnectorSpec extends BaseConnectorSpec {

  trait Setup {

    val baseUrl = "http://localhost:9999"

    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new OutstandingChargesConnector(mockHttpClientV2, getAppConfig())
  }


  "OutstandingChargesConnector" should {
    ".getOutstandingChargesUrl()" should {

      "return the correct url" in new Setup {
        connector.getOutstandingChargesUrl(testSaUtr, idNumber, testTo) shouldBe
          s"$baseUrl/income-tax-view-change/out-standing-charges/$testSaUtr/$idNumber/$testTo"
      }
    }

    ".getOutstandingCharges()" should {

      val successResponse = HttpResponse(status = Status.OK, json = testValidOutStandingChargeModelJson, headers = Map.empty)
      val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidOutstandingChargesJson, headers = Map.empty)
      val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

      "return a OutstandingCharges model when successful JSON is received" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))

        val result: Future[OutstandingChargesResponseModel] = connector.getOutstandingCharges(idType, idNumber, taxYear2020)
        result.futureValue shouldBe testValidOutstandingChargesModel

      }

      "return a OutstandingCharges model in case of future failed scenario" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.failed(new Exception("unknown error")))

        val result: Future[OutstandingChargesResponseModel] = connector.getOutstandingCharges(idType, idNumber, taxYear2020)
        result.futureValue shouldBe OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
      }


      "return OutstandingChargesErrorResponse model in case of failure" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))

        val result: Future[OutstandingChargesResponseModel] = connector.getOutstandingCharges(idType, idNumber, taxYear2020)
        result.futureValue shouldBe OutstandingChargesErrorModel(Status.BAD_REQUEST, "Error Message")
      }

      "return OutstandingChargesErrorResponse model in case of bad/malformed JSON response" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))

        val result: Future[OutstandingChargesResponseModel] = connector.getOutstandingCharges(idType, idNumber, taxYear2020)
        result.futureValue shouldBe testOutstandingChargesErrorModelParsing
      }

    }
  }
}
