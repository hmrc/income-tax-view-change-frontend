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

package services

import connectors.CustomerFactsUpdateConnector
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{mock, verify, when}
import testUtils.TestSupport
import uk.gov.hmrc.http.HeaderCarrier

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

    when(
      mockCustomerFactsUpdateConnector.updateCustomerFacts(any[String])(any[HeaderCarrier])
    ).thenReturn(Future.successful(()))
  }

  "updateCustomerFacts" should {

    "call the connector and return Unit on success" in new Setup {
      val result: Future[Unit] = service.updateCustomerFacts(mtdId)

      result.futureValue shouldBe ()
      verify(mockCustomerFactsUpdateConnector).updateCustomerFacts(eqTo(mtdId))(any[HeaderCarrier])
    }

    "still return Unit when the connector fails with an exception" in new Setup {
      when(mockCustomerFactsUpdateConnector.updateCustomerFacts(eqTo(mtdId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result: Future[Unit] = service.updateCustomerFacts(mtdId)

      result.futureValue shouldBe ()
      verify(mockCustomerFactsUpdateConnector).updateCustomerFacts(eqTo(mtdId))(any[HeaderCarrier])
    }

    "not throw even if connector returns a failed future" in new Setup {
      when(mockCustomerFactsUpdateConnector.updateCustomerFacts(eqTo(mtdId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new Exception("unexpected")))

      noException should be thrownBy {
        service.updateCustomerFacts(mtdId).futureValue
      }
    }
  }
}