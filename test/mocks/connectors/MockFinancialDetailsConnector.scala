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

package mocks.connectors

import connectors.FinancialDetailsConnector
import models.core.Nino
import models.financialDetails.{FinancialDetailsResponseModel, PaymentsResponse}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsResponse
import models.paymentAllocations.PaymentAllocationsResponse
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec
import scala.concurrent.Future

trait MockFinancialDetailsConnector extends UnitSpec with BeforeAndAfterEach {

  val mockFinancialDetailsConnector: FinancialDetailsConnector = mock(classOf[FinancialDetailsConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFinancialDetailsConnector)
  }

  def setupMockGetFinancialDetails(taxYear: Int, nino: String)(response: FinancialDetailsResponseModel): Unit = {
    when(mockFinancialDetailsConnector.getFinancialDetails(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupGetPayments(taxYear: Int)(response: PaymentsResponse): Unit = {
    when(mockFinancialDetailsConnector.getPayments(ArgumentMatchers.eq(taxYear))(any(), any()))
      .thenReturn(Future.successful(response))
  }

  def setupGetPaymentAllocation(nino: String, paymentLot: String, paymentLotItem: String)(response: PaymentAllocationsResponse): Unit = {
    when(
      mockFinancialDetailsConnector.getPaymentAllocations(Nino(ArgumentMatchers.eq(nino)),
        ArgumentMatchers.eq(paymentLot), ArgumentMatchers.eq(paymentLotItem))(any())
    ).thenReturn(Future.successful(response))
  }

  def setupGetPaymentAllocationCharges(nino: String, documentId: String)(response: FinancialDetailsWithDocumentDetailsResponse): Unit = {
    when(
      mockFinancialDetailsConnector.getFinancialDetailsByDocumentId(Nino(ArgumentMatchers.eq(nino)), ArgumentMatchers.eq(documentId))(any())
    ).thenReturn(Future.successful(response))
  }

}
