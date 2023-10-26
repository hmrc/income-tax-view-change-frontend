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

//package mocks.connectors
//
//import connectors.IncomeTaxViewChangeConnector
//import models.calculationList.CalculationListResponseModel
//import models.core.{Nino, NinoResponse}
//import models.financialDetails.{FinancialDetailsResponseModel, PaymentsResponse}
//import models.incomeSourceDetails.IncomeSourceDetailsResponse
//import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseModel}
//import models.nextUpdates.NextUpdatesResponseModel
//import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsResponse
//import models.paymentAllocations.PaymentAllocationsResponse
//import models.repaymentHistory.{RepaymentHistoryErrorModel, RepaymentHistoryModel}
//import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse}
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers.{any, eq => matches}
//import org.mockito.Mockito._
//import org.scalatest.BeforeAndAfterEach
//import testUtils.UnitSpec
//import uk.gov.hmrc.auth.core.AffinityGroup
//
//import java.time.LocalDate
//import scala.concurrent.Future
//
//trait MockIncomeTaxViewChangeConnector extends UnitSpec with BeforeAndAfterEach {
//
//  val mockIncomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock(classOf[IncomeTaxViewChangeConnector])
//
//  val testFromDate = LocalDate.of(2018, 4, 6)
//  val testToDate = LocalDate.of(2018, 4, 5)
//
//  override def beforeEach(): Unit = {
//    super.beforeEach()
//    reset(mockIncomeTaxViewChangeConnector)
//  }
//
//  def setupMockIncomeSourceDetailsResponse(mtditid: String, nino: String,
//                                           saUtr: Option[String], credId: Option[String],
//                                           userType: Option[AffinityGroup])(response: IncomeSourceDetailsResponse): Unit =
//    when(mockIncomeTaxViewChangeConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any()))
//      .thenReturn(Future.successful(response))
//
//  def verifyMockIncomeSourceDetailsResponse(noOfCalls: Int): Future[IncomeSourceDetailsResponse] =
//    verify(mockIncomeTaxViewChangeConnector, times(noOfCalls)).getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any())
//
//  def setupNinoLookupResponse(mtdRef: String)(response: NinoResponse): Unit =
//    when(mockIncomeTaxViewChangeConnector
//      .getNino(
//        ArgumentMatchers.eq(mtdRef))(ArgumentMatchers.any()))
//      .thenReturn(Future.successful(response))
//
//  def setupMockNextUpdates(response: NextUpdatesResponseModel): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getNextUpdates()(ArgumentMatchers.any(), ArgumentMatchers.any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupMockPreviousObligations(response: NextUpdatesResponseModel): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getPreviousObligations()(ArgumentMatchers.any(), ArgumentMatchers.any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupMockPreviousObligationsWithDates(from: LocalDate, to: LocalDate)(response: NextUpdatesResponseModel): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getPreviousObligations(
//      fromDate = matches(from), toDate = matches(to))(ArgumentMatchers.any(), ArgumentMatchers.any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupMockGetFinancialDetails(taxYear: Int, nino: String)(response: FinancialDetailsResponseModel): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getFinancialDetails(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupBusinessDetails(nino: String)(response: Future[IncomeSourceDetailsResponse]): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getBusinessDetails(ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
//      .thenReturn(response)
//  }
//
//  def setupGetPayments(taxYear: Int)(response: PaymentsResponse): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getPayments(ArgumentMatchers.eq(taxYear))(any(), any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupGetPaymentAllocation(nino: String, paymentLot: String, paymentLotItem: String)(response: PaymentAllocationsResponse): Unit = {
//    when(
//      mockIncomeTaxViewChangeConnector.getPaymentAllocations(Nino(ArgumentMatchers.eq(nino)),
//        ArgumentMatchers.eq(paymentLot), ArgumentMatchers.eq(paymentLotItem))(any())
//    ).thenReturn(Future.successful(response))
//  }
//
//  def setupGetPaymentAllocationCharges(nino: String, documentId: String)(response: FinancialDetailsWithDocumentDetailsResponse): Unit = {
//    when(
//      mockIncomeTaxViewChangeConnector.getFinancialDetailsByDocumentId(Nino(ArgumentMatchers.eq(nino)), ArgumentMatchers.eq(documentId))(any())
//    ).thenReturn(Future.successful(response))
//  }
//
//  def setupGetRepaymentHistoryByRepaymentId(nino: String, repaymentId: String)(response: RepaymentHistoryModel): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getRepaymentHistoryByRepaymentId(Nino(ArgumentMatchers.eq(nino)), ArgumentMatchers.eq(repaymentId))(any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupGetRepaymentHistoryByRepaymentIdError(nino: String, repaymentId: String)(response: RepaymentHistoryErrorModel): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getRepaymentHistoryByRepaymentId(Nino(ArgumentMatchers.eq(nino)), ArgumentMatchers.eq(repaymentId))(any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupGetLegacyCalculationList(nino: String, taxYear: String)(response: CalculationListResponseModel): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getLegacyCalculationList(Nino(ArgumentMatchers.eq(nino)), ArgumentMatchers.eq(taxYear))(any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupGetCalculationList(nino: String, taxYearRange: String)(response: CalculationListResponseModel): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getCalculationList(Nino(ArgumentMatchers.eq(nino)), ArgumentMatchers.eq(taxYearRange))(any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupGetITSAStatusDetail(nino: String, taxYear: String, futureYears: Boolean, history: Boolean)(response: Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]): Unit = {
//    when(mockIncomeTaxViewChangeConnector.getITSAStatusDetail(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(futureYears), ArgumentMatchers.eq(history))(any()))
//      .thenReturn(Future.successful(response))
//  }
//
//  def setupUpdateIncomeSourceTaxYearSpecific(nino: String, incomeSourceId: String, taxYearSpecific: TaxYearSpecific)(response: UpdateIncomeSourceResponse): Unit = {
//    when(mockIncomeTaxViewChangeConnector.updateIncomeSourceTaxYearSpecific(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(incomeSourceId), ArgumentMatchers.eq(taxYearSpecific))(any()))
//      .thenReturn(Future.successful(response))
//  }
//}
