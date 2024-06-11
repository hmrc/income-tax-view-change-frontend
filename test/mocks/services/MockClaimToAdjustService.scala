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

import connectors.CalculationListConnector
import models.claimToAdjustPoa.{AmendablePoaViewModel, PaymentOnAccountViewModel, PoAAmountViewModel}
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.ClaimToAdjustService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockClaimToAdjustService extends UnitSpec with BeforeAndAfterEach {

  val claimToAdjustService: ClaimToAdjustService = mock(classOf[ClaimToAdjustService])

  val calculationListConnector: CalculationListConnector = mock(classOf[CalculationListConnector])

  val defaultPaymentOnAccountModel = PaymentOnAccountViewModel(
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    paymentOnAccountOne = 5000.00,
    paymentOnAccountTwo = 5000.00,
    poARelevantAmountOne = 5000.00,
    poARelevantAmountTwo = 5000.00,
    poAPartiallyPaid = false
  )

  val defaultPaymentOnAccountViewModel = AmendablePoaViewModel(
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    paymentOnAccountOne = 5000.00,
    paymentOnAccountTwo = 5000.00,
    poARelevantAmountOne = 5000.00,
    poARelevantAmountTwo = 5000.00,
    poAPartiallyPaid = false,
    poAFullyPaid = false,
    poasHaveBeenAdjustedPreviously = false
  )

  def setupMockGetPaymentOnAccountViewModel(data: AmendablePoaViewModel = defaultPaymentOnAccountViewModel): Unit =
    when(claimToAdjustService.getAdjustPaymentOnAccountViewModel(Nino(any()))(any(), any()))
      .thenReturn(Future.successful(Right(data)))

  def setupMockGetPaymentsOnAccount(data: Option[PaymentOnAccountViewModel] = Some(defaultPaymentOnAccountModel)): Unit =
    when(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(any()))(any()))
      .thenReturn(Future.successful(Right(data)))

  def setupMockGetPaymentsOnAccountBuildFailure(): Unit =
    when(claimToAdjustService.getAdjustPaymentOnAccountViewModel(Nino(any()))(any(), any()))
      .thenReturn(
        Future.successful(
          Left(
            new Exception("Failed to create AmendablePoaViewModel")
          )
        )
      )

  def setupMockGetPaymentsOnAccountFailure(): Unit =
    when(claimToAdjustService.getAdjustPaymentOnAccountViewModel(Nino(any()))(any(), any()))
      .thenReturn(
        Future.successful(
          Left(
            new Exception("Unexpected Error occurred")
          )
        )
      )

  def setupMockGetPoaAmountViewModel(response: Either[Throwable, PoAAmountViewModel]): Unit = {
    when(claimToAdjustService
      .getEnterPoAAmountViewModel(Nino(any()))(any(), any(), any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockGetPoaAmountViewModelFailure(): Unit =
    when(claimToAdjustService.getEnterPoAAmountViewModel(Nino(any()))(any(), any(), any()))
      .thenReturn(
        Future.successful(
          Left(
            new Exception("Unexpected Error occurred")
          )
        )
      )

  def setupMockGetPoaTaxYearForEntryPointCall(response: Either[Throwable, Option[TaxYear]]): Unit = {
    when(claimToAdjustService
      .getPoaTaxYearForEntryPoint(Nino(any()))(any))
      .thenReturn(Future.successful(response))
  }
}
