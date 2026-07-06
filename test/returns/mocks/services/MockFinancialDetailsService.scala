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

package returns.mocks.services

import common.models.core.Nino
import common.models.incomeSourceDetails.TaxYear
import common.testConstants.BaseTestConstants.testTaxYear
import common.testUtils.UnitSpec
import financials.models.claimToAdjustPoa.viewModels.PaymentOnAccountViewModel
import financials.models.{FinancialDetailsModel, FinancialDetailsResponseModel}
import returns.services.FinancialDetailsService
import financials.testConstants.FinancialDetailsTestConstants.*
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Future

trait MockFinancialDetailsService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockFinancialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])
  
  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFinancialDetailsService)
  }

  def setupMockGetFinancialDetails(taxYear: Int)(response: FinancialDetailsResponseModel): Unit =
    when(mockFinancialDetailsService.getFinancialDetails(any(), any())
    (any(), any())).thenReturn(Future.successful(response))

  def setupMockGetFinancialDetailsWithTaxYearAndNino(taxYear: Int, nino: String)(response: FinancialDetailsResponseModel): Unit = {
    when(mockFinancialDetailsService.getFinancialDetails(any(), any())
    (any(), any())).thenReturn(Future.successful(response))
  }

  def mockFinancialDetailsSuccess(financialDetailsModelResponse: FinancialDetailsModel = financialDetailsModel(), taxYear: Int = testTaxYear): Unit =
    setupMockGetFinancialDetails(taxYear)(financialDetailsModelResponse)

  def mockFinancialDetailsFailed(): Unit =
    setupMockGetFinancialDetails(testTaxYear)(testFinancialDetailsErrorModel)

  def mockFinancialDetailsNotFound(): Unit =
    setupMockGetFinancialDetails(testTaxYear)(testFinancialDetailsNotFoundErrorModel)

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

  def setupMockGetPoaTaxYearForEntryPointCall(response: Either[Throwable, Option[TaxYear]]): Unit = {
    when(mockFinancialDetailsService
      .getPoaTaxYearForEntryPoint(Nino(any()))(any(), any()))
      .thenReturn(Future.successful(response))
  }
}
