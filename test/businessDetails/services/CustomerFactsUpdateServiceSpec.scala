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

package businessDetails.services

import common.connectors.CustomerFactsUpdateConnector
import common.services.CustomerFactsUpdateService
import common.testUtils.TestSupport
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{mock, verify, when}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class CustomerFactsUpdateServiceSpec extends TestSupport {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val mockCustomerFactsUpdateConnector: CustomerFactsUpdateConnector =
      mock(classOf[CustomerFactsUpdateConnector])

    val service = new CustomerFactsUpdateService(
      customerFactsUpdateConnector = mockCustomerFactsUpdateConnector
    )

    val mtdId = "XAIT0000123456"
  }

  "updateCustomerFacts" should {

    "return Unit when the connector returns OK" in new Setup {

      when(mockCustomerFactsUpdateConnector.updateCustomerFacts(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200, "")))

      val result: Future[Unit] = service.updateCustomerFacts(mtdId)

      result.futureValue shouldBe ()
      verify(mockCustomerFactsUpdateConnector).updateCustomerFacts(eqTo(mtdId))(any[HeaderCarrier])
    }

    "fail when the connector returns a non-OK response" in new Setup {

      when(mockCustomerFactsUpdateConnector.updateCustomerFacts(eqTo(mtdId))(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(500, "boom")))

      val exception: Exception = intercept[Exception] {
        service.updateCustomerFacts(mtdId).futureValue
      }

      exception.getMessage should include("Customer facts update failed")
    }

    "propagate connector exceptions" in new Setup {

      when(mockCustomerFactsUpdateConnector.updateCustomerFacts(eqTo(mtdId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("fail")))

      val exception: RuntimeException = intercept[RuntimeException] {
        service.updateCustomerFacts(mtdId).futureValue
      }

      exception.getMessage shouldBe "The future returned an exception of type: java.lang.RuntimeException, with message: fail."
    }
  }
}