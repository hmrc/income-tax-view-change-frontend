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

package mocks.services

import models.core.Nino
import models.paymentAllocationCharges.PaymentAllocationViewModel
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.PaymentAllocationsService
import services.PaymentAllocationsService.PaymentAllocationError
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait MockPaymentAllocationsService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockPaymentAllocationsService: PaymentAllocationsService = mock[PaymentAllocationsService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPaymentAllocationsService)
  }

  def setupMockGetPaymentAllocation(nino: String, docNumber: String)
                                   (response: Future[Either[PaymentAllocationError.type, PaymentAllocationViewModel]]): Unit =
    when(mockPaymentAllocationsService
      .getPaymentAllocation(
        Nino(matches(nino)),
        matches(docNumber)
      )(any(), any()))
      .thenReturn(response)

  def setupMockGetPaymentAllocationSuccess(nino: String, docNumber: String)(model: PaymentAllocationViewModel): Unit =
    setupMockGetPaymentAllocation(nino, docNumber)(Future.successful(Right(model)))

  def setupMockGetPaymentAllocationError(nino: String, docNumber: String): Unit =
    setupMockGetPaymentAllocation(nino, docNumber)(Future.successful(Left(PaymentAllocationError)))

}
