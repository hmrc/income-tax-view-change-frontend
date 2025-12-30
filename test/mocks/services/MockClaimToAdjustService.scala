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

import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.ClaimToAdjustService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockClaimToAdjustService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockClaimToAdjustService: ClaimToAdjustService = mock(classOf[ClaimToAdjustService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClaimToAdjustService)
  }

  val defaultPaymentOnAccountModel: PaymentOnAccountViewModel = PaymentOnAccountViewModel(
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 5000.00,
    totalAmountTwo = 5000.00,
    relevantAmountOne = 5000.00,
    relevantAmountTwo = 5000.00,
    partiallyPaid = false,
    fullyPaid = false,
    previouslyAdjusted = Some(false)
  )

  val previouslyReducedPaymentOnAccountModel: PaymentOnAccountViewModel = PaymentOnAccountViewModel(
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 3000.00,
    totalAmountTwo = 3000.00,
    relevantAmountOne = 5000.00,
    relevantAmountTwo = 5000.00,
    partiallyPaid = false,
    fullyPaid = false,
    previouslyAdjusted = Some(false)
  )

  def setupMockGetPaymentOnAccountViewModel(data: PaymentOnAccountViewModel = defaultPaymentOnAccountModel): Unit =
    when(mockClaimToAdjustService.getAmendablePoaViewModel(Nino(any()))(any(), any()))
      .thenReturn(Future.successful(Right(data)))

  def setupMockGetPaymentsOnAccount(data: Option[PaymentOnAccountViewModel] = Some(defaultPaymentOnAccountModel)): Unit =
    when(mockClaimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(any()))(any(), any()))
      .thenReturn(Future.successful(Right(data)))

  def setupMockGetPaymentsOnAccountBuildFailure(): Unit =
    when(mockClaimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(any()))(any(), any()))
      .thenReturn(
        Future.successful(
          Left(
            new Exception("Failed to create AmendablePoaViewModel")
          )
        )
      )

  def setupMockGetAmendablePoaViewModelFailure(): Unit =
    when(mockClaimToAdjustService.getAmendablePoaViewModel(Nino(any()))(any(), any()))
      .thenReturn(
        Future.successful(
          Left(
            new Exception("Unexpected Error occurred")
          )
        )
      )

  def setupMockGetPoaAmountViewModel(response: Either[Throwable, PaymentOnAccountViewModel]): Unit = {
    when(mockClaimToAdjustService
      .getPoaViewModelWithAdjustmentReason(Nino(any()))(any(), any(), any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockGetPoaAmountViewModelFailure(): Unit =
    when(mockClaimToAdjustService.getPoaViewModelWithAdjustmentReason(Nino(any()))(any(), any(), any()))
      .thenReturn(
        Future.successful(
          Left(
            new Exception("Unexpected Error occurred")
          )
        )
      )

  def setupMockGetPoaTaxYearForEntryPointCall(response: Option[TaxYear]): Unit = {
    when(mockClaimToAdjustService
      .getPoaTaxYearForEntryPoint(Nino(any()))(any(), any()))
      .thenReturn(Future.successful(response))
  }
  def setupMockGetPoaTaxYearForEntryPointCallFailure(ex: Throwable): Unit = {
    when(mockClaimToAdjustService
      .getPoaTaxYearForEntryPoint(Nino(any()))(any(), any()))
      .thenReturn(Future.failed(new Exception("There was an error when getting the POA Entry point")))
  }
}
