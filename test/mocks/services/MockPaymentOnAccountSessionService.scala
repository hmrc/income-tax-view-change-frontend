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

package mocks.services

import models.claimToAdjustPoa.{PoaAmendmentData, SelectYourReason}
import org.mockito.ArgumentMatchers.{any, same}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.PaymentOnAccountSessionService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockPaymentOnAccountSessionService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockPaymentOnAccountSessionService: PaymentOnAccountSessionService = mock(classOf[PaymentOnAccountSessionService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPaymentOnAccountSessionService)
  }

  def setupMockPaymentOnAccountSessionService(response: Future[Either[Throwable, Option[PoaAmendmentData]]]): Unit = {
      when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(response)
  }

  def setupMockPaymentOnAccountSessionServiceCreateSession(response: Future[Either[Throwable, Unit]]): Unit = {
    when(mockPaymentOnAccountSessionService.createSession(any(), any())).thenReturn(response)
  }

  def setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(reason: SelectYourReason,
                                                                 response: Future[Either[Throwable, Unit]] = Future.successful(Right(()))): Unit = {
    when(mockPaymentOnAccountSessionService.setAdjustmentReason(same(reason))(any(), any())).thenReturn(response)
  }

  def verifyMockCreateSession(noOfCalls: Int): Future[Either[Throwable, Unit]] =
    verify(mockPaymentOnAccountSessionService, times(noOfCalls)).createSession(any(), any())

}
