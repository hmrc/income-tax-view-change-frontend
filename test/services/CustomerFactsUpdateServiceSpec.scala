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
import org.mockito.Mockito.{mock, times, verify, when}
import play.api.http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class CustomerFactsUpdateServiceSpec extends TestSupport {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val mockConnector: CustomerFactsUpdateConnector = mock(classOf[CustomerFactsUpdateConnector])
    val service = new CustomerFactsUpdateService(mockConnector)
    val mtdId = "XAIT0000123456"
  }

  "updateCustomerFacts" should {

    "return Unit and NOT call the connector when already confirmed" in new Setup {
      val result: Future[Unit] = service.updateCustomerFacts(mtdId, isAlreadyConfirmed = true)

      result.futureValue shouldBe ()
      verify(mockConnector, times(0)).updateCustomerFacts(any())(any())
    }

    "call the connector when not confirmed and return Unit on 200" in new Setup {
      when(mockConnector.updateCustomerFacts(eqTo(mtdId))(any()))
        .thenReturn(Future.successful(HttpResponse(Status.OK, "")))

      val result: Future[Unit] = service.updateCustomerFacts(mtdId, isAlreadyConfirmed = false)

      result.futureValue shouldBe ()
      verify(mockConnector, times(1)).updateCustomerFacts(eqTo(mtdId))(any())
    }

    "call the connector when not confirmed and return Unit on 422" in new Setup {
      when(mockConnector.updateCustomerFacts(eqTo(mtdId))(any()))
        .thenReturn(Future.successful(HttpResponse(Status.UNPROCESSABLE_ENTITY, "")))

      val result: Future[Unit] = service.updateCustomerFacts(mtdId, isAlreadyConfirmed = false)

      result.futureValue shouldBe ()
      verify(mockConnector, times(1)).updateCustomerFacts(eqTo(mtdId))(any())
    }

    "swallow exceptions (recover) and still return Unit" in new Setup {
      when(mockConnector.updateCustomerFacts(eqTo(mtdId))(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result: Future[Unit] = service.updateCustomerFacts(mtdId, isAlreadyConfirmed = false)

      result.futureValue shouldBe ()
      verify(mockConnector, times(1)).updateCustomerFacts(eqTo(mtdId))(any())
    }
  }
}
