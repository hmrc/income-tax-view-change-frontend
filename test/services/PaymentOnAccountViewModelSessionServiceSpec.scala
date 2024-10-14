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

import models.claimToAdjustPoa.{MainIncomeLower, PoaAmendmentData, PoaSessionData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalactic.Fail
import repositories.PoaAmendmentDataRepository
import testUtils.TestSupport

import scala.concurrent.Future

class PaymentOnAccountViewModelSessionServiceSpec extends TestSupport {

  val mockRepository: PoaAmendmentDataRepository = mock(classOf[PoaAmendmentDataRepository])

  object TestPaymentOnAccountSessionService extends PaymentOnAccountSessionService(
    mockRepository)

  val ammendmentData: PoaAmendmentData = PoaAmendmentData(poaAdjustmentReason = Some(MainIncomeLower), newPoaAmount = None)

  val sessionData: PoaSessionData = PoaSessionData(
    sessionId = "session-123456",
    poaAmendmentData = Some(ammendmentData)
  )

  "PaymentOnAccountSessionService.createSession" should {
    "create the mongo session" in {
      when(mockRepository.set(any())).thenReturn(Future.successful(true))
      val result = TestPaymentOnAccountSessionService.createSession
      result.futureValue shouldBe Right(())
    }
  }
  "PaymentOnAccountSessionService.getMongo" should {
    "return the correct mongo data" in {
      when(mockRepository.get(any())).thenReturn(Future.successful(Some(sessionData)))
      TestPaymentOnAccountSessionService.getMongo(headerCarrier, ec).futureValue shouldBe Right(Some(ammendmentData))
    }
  }
  "PaymentOnAccountSessionService.setMongoData" should {
    "set the mongo value" in {
      when(mockRepository.set(any())).thenReturn(Future.successful(true))
      val result = TestPaymentOnAccountSessionService.setMongoData(Some(ammendmentData))
      result.futureValue shouldBe true
    }
  }
  "PaymentOnAccountSessionService.setAdjustmentReason" should {
    "update the adjustment reason" in {
      when(mockRepository.get(any())).thenReturn(Future.successful(Some(sessionData)))
      when(mockRepository.set(any())).thenReturn(Future.successful(true))
      val result = TestPaymentOnAccountSessionService.setAdjustmentReason(MainIncomeLower)
      result.futureValue shouldBe Right(())
    }
    "return an error" when {
      "no mongo session can be found" in {
        when(mockRepository.get(any())).thenReturn(Future.successful(None))
        val result = TestPaymentOnAccountSessionService.setAdjustmentReason(MainIncomeLower)
        result.futureValue match {
          case Left(ex) => ex.getMessage shouldBe "No active mongo session found"
          case _ => Fail
        }
      }
    }
  }
  "PaymentOnAccountSessionService.setNewPoaAmount" should {
    "update the PoA amount" in {
      when(mockRepository.get(any())).thenReturn(Future.successful(Some(sessionData)))
      when(mockRepository.set(any())).thenReturn(Future.successful(true))
      val result = TestPaymentOnAccountSessionService.setNewPoaAmount(100.00)
      result.futureValue shouldBe Right(())
    }
    "return an error" when {
      "no mongo session can be found" in {
        when(mockRepository.get(any())).thenReturn(Future.successful(None))
        val result = TestPaymentOnAccountSessionService.setNewPoaAmount(100.00)
        result.futureValue match {
          case Left(ex) => ex.getMessage shouldBe "No active mongo session found"
          case _ => Fail
        }
      }
    }
  }
}
