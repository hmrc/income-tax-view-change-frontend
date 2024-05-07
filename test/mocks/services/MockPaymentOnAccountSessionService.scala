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

import models.paymentOnAccount.PoAAmendmentData
import org.mockito.ArgumentMatchers.{any, same, eq => matches}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.PaymentOnAccountSessionService
import testUtils.UnitSpec
import viewmodels.adjustPoa.checkAnswers.SelectYourReason

import scala.concurrent.Future

trait MockPaymentOnAccountSessionService extends UnitSpec with BeforeAndAfterEach {

  val mockPaymentOnAccountSessionService: PaymentOnAccountSessionService = mock(classOf[PaymentOnAccountSessionService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPaymentOnAccountSessionService)
  }

  def setupMockPaymentOnAccountSessionService(response: Future[Either[Throwable, Option[PoAAmendmentData]]]): Unit = {
      when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(response)
  }

  def setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(reason: SelectYourReason,
                                                                 response: Future[Either[Throwable, Unit]] = Future.successful(Right(()))): Unit = {
    when(mockPaymentOnAccountSessionService.setAdjustmentReason(same(reason))(any(), any())).thenReturn(response)
  }

}
