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
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import models.paymentOnAccount.PaymentOnAccount
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.ClaimToAdjustService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockClaimToAdjustService extends UnitSpec with BeforeAndAfterEach {

  val claimToAdjustService: ClaimToAdjustService = mock(classOf[ClaimToAdjustService])

  val calculationListConnector: CalculationListConnector = mock(classOf[CalculationListConnector])

  def setupMockGetPaymentsOnAccount(): Unit =
    when(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(any()))(any()))
      .thenReturn(
        Future.successful(
          Right(
            Some(
              PaymentOnAccount(
                poaOneTransactionId = "poaOne-Id",
                poaTwoTransactionId = "poaTwo-Id",
                taxYear = TaxYear.makeTaxYearWithEndYear(2024),
                paymentOnAccountOne = 5000.00,
                paymentOnAccountTwo = 5000.00
              )
            )
          )
        )
      )

  def setupMockGetPaymentsOnAccountBuildFailure(): Unit =
    when(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(any()))(any()))
      .thenReturn(
        Future.successful(
          Right(
            None
          )
        )
      )

  def setupMockGetPaymentsOnAccountFailure(): Unit =
    when(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(any()))(any()))
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
