/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.CreditDetailsModel
import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetail, FinancialDetailsErrorModel, FinancialDetailsModel, Payment, Payments, PaymentsError, SubItem}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import play.api.test.FakeRequest
import services.CreditHistoryService.CreditHistoryError
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.oldUserDetails
import testUtils.TestSupport

import java.time.LocalDate

class CreditHistoryServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector with FeatureSwitching {
  import CreditDetailsModel._

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val getCurrentTaxEndYear: Int = {
    val currentDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val user: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = oldUserDetails,
    btaNavPartial = None,
    saUtr = Some("saUtr"),
    credId = Some("credId"),
    userType = Some("Individual"),
    None
  )(FakeRequest())


  val paymentsForTheGivenTaxYear: List[Payment] = List(Payment(reference = Some("reference"), amount = Some(100.00),
    outstandingAmount = Some(1.00), method = Some("method"), documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"),
    dueDate = Some("date"), documentDate = "docDate", Some("DOCID01")))

  val creditsForTheGivenTaxYear: List[Payment] = List(Payment(reference = Some("reference"), amount = Some(-100.00),
    outstandingAmount = Some(1.00), method = Some("method"), documentDescription = None, lot = None, lotItem = Some("lotItem"),
    dueDate = Some("date"), documentDate = "docDate", Some("DOCID01")))

  object TestCreditHistoryService extends CreditHistoryService(mockIncomeTaxViewChangeConnector, appConfig)

  val taxYear: Int = 2022
  val nino: String = "someNino"
  val taxYearFinancialDetails = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    codingDetails = None,
    documentDetails = List(
      DocumentDetail("testYear2", "testTransactionId1", None, None, Some(100.00), None, LocalDate.of(taxYear, 3, 29)),
      DocumentDetail("testYear2", "testTransactionId2", None, None, None, None, LocalDate.of(taxYear, 3, 29))
    ),
    financialDetails = List(
      FinancialDetail("testYear2", None, Some("testTransactionId1"), None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(LocalDate.now.plusDays(3).toString))))),
      FinancialDetail("testYear2", None, Some("testTransactionId2"), None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(LocalDate.now.plusDays(5).toString)))))
    )
  )

  val financialDetail: FinancialDetail = FinancialDetail(
    taxYear = "2018",
    transactionId = Some("transactionId"),
    transactionDate = Some("2018-03-29"),
    `type` = Some("type"),
    totalAmount = Some(BigDecimal("1000.00")),
    originalAmount = Some(BigDecimal(500.00)),
    outstandingAmount = Some(BigDecimal("500.00")),
    clearedAmount = Some(BigDecimal(500.00)),
    chargeType = Some("NIC4 Wales"),
    mainType = Some("SA Payment on Account 1"),
    items = Some(Seq(
      SubItem(
        subItemId = Some("1"),
        amount = Some(BigDecimal("100.00")),
        clearingDate = Some("2021-01-31"),
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod"),
        paymentReference = Some("paymentReference"),
        paymentAmount = Some(BigDecimal("2000.00")),
        dueDate = Some("2021-01-31"),
        paymentMethod = Some("paymentMethod"),
        paymentLot = Some("paymentLot"),
        paymentLotItem = Some("paymentLotItem"),
        paymentId = Some("paymentLot-paymentLotItem")
      ),
      SubItem(
        subItemId = Some("2"),
        amount = Some(BigDecimal("200.00")),
        clearingDate = None,
        clearingReason = None,
        outgoingPaymentMethod = Some("outgoingPaymentMethod2"),
        paymentReference = None,
        paymentAmount = Some(BigDecimal("3000.00")),
        dueDate = Some("2021-01-31"),
        paymentMethod = Some("paymentMethod2"),
        paymentLot = Some("paymentLot2"),
        paymentLotItem = None,
        paymentId = None
      )))
  )
  val cutOverCreditsAsFinancialDocument : FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(
      DocumentDetail(
        taxYear = "2018",
        transactionId = "id",
        documentDescription = Some("documentDescription"),
        documentText = Some("documentText"),
        originalAmount = Some(-300.00),
        outstandingAmount = Some(-200.00),
        documentDate = LocalDate.of(2018, 3, 29),
        paymentLot = Some("paymentLot"),
        paymentLotItem = Some("paymentLotItem")
      )
    ),
    financialDetails = List(
      financialDetail
    )
  )

  val cutOverCredit: CreditDetailsModel = cutOverCreditsAsFinancialDocument

  val creditModels: List[CreditDetailsModel] = List(taxYearFinancialDetails)

  "getCreditHistory" when {

    "an error is returned from the connector" should {
      "return a credit history error" in {
        setupMockGetFinancialDetails(taxYear, nino)( FinancialDetailsErrorModel(500, "ERROR") )
        TestCreditHistoryService.getCreditsHistory(taxYear, nino).futureValue shouldBe Left(CreditHistoryError)
      }
    }

    "a successful Payment/Credit History response is returned from the connector" should {
      "return a list of MFA credits only" in {
        setupGetPayments(taxYear)( Payments(paymentsForTheGivenTaxYear) )
        setupMockGetFinancialDetails(taxYear, nino)( taxYearFinancialDetails )
        TestCreditHistoryService.getCreditsHistory(taxYear, nino).futureValue shouldBe Right(creditModels)
      }

      "return a list of MFA and CutOver credits" in {
        setupGetPayments(taxYear)( Payments(creditsForTheGivenTaxYear) )
        setupMockGetFinancialDetails(taxYear, nino)( taxYearFinancialDetails )
        setupGetPaymentAllocationCharges(nino, "DOCID01")(cutOverCreditsAsFinancialDocument)
        TestCreditHistoryService.getCreditsHistory(taxYear, nino).futureValue shouldBe Right(cutOverCredit +: creditModels )
      }


    }

  }

}
