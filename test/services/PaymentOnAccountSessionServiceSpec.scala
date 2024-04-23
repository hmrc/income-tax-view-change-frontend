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

package services

import models.paymentOnAccount.PoAAmmendmentData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import repositories.PoAAmmendmentDataRepository
import testUtils.TestSupport

import scala.concurrent.Future

class PaymentOnAccountSessionServiceSpec extends TestSupport {

  val mockRepository: PoAAmmendmentDataRepository = mock(classOf[PoAAmmendmentDataRepository])

  object TestPaymentOnAccountSessionService extends PaymentOnAccountSessionService(
    mockRepository)

  val sessionData: PoAAmmendmentData = PoAAmmendmentData(
    sessionId = "session-123456", poaAdjustmentReason = Some("reason X"), newPoAAmount = None
  )

  "PaymentOnAccountSessionService.createSession" should {
    "create the mongo session" in {
      when(mockRepository.set(any())).thenReturn(Future.successful(true))
      val result = TestPaymentOnAccountSessionService.createSession
      result.futureValue shouldBe true
    }
  }
  "PaymentOnAccountSessionService.getMongo" should {
    "return the correct mongo data" in {
      when(mockRepository.get(any())).thenReturn(Future.successful(Some(sessionData)))
      TestPaymentOnAccountSessionService.getMongo(headerCarrier, ec).futureValue shouldBe Right(Some(sessionData))
    }
  }
  "PaymentOnAccountSessionService.getMongoKey" should {
    "return the correct session value for given key" in {
      when(mockRepository.get(any())).thenReturn(Future.successful(Some(sessionData)))
      TestPaymentOnAccountSessionService.getMongoKey("poaAdjustmentReason")(headerCarrier, ec).futureValue shouldBe Right(Some("reason X"))
    }
  }
  "PaymentOnAccountSessionService.setMongoData" should {
    "set the mongo value" in {
      when(mockRepository.set(any())).thenReturn(Future.successful(true))
      val result = TestPaymentOnAccountSessionService.setMongoData(sessionData)
      result.futureValue shouldBe true
    }
  }
}
