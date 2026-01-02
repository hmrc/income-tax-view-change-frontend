/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import enums.CodingOutType.{CODING_OUT_CANCELLED, CODING_OUT_CLASS2_NICS}
import enums.MTDUserRole
import models.admin.NavBarFs
import models.financialDetails._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.liabilitycalculation.viewmodels.TYSClaimToAdjustViewModel
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import models.taxyearsummary.TaxYearSummaryChargeItem
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse

import java.time.LocalDate

trait TaxSummaryISpecHelper extends ControllerISpecHelper with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(NavBarFs)
  }

  val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

  val emptyCTAModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(poaTaxYear = None)

  val financialDetailsSuccess: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId",
        documentDescription = Some("ITSA- POA 1"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 1000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        accruingInterestAmount = Some(100.00),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      )),
    financialDetails = List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Payment on Account 1"),
        mainTransaction = Some("4920"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)))))
      )
    )
  )

  val immediatelyRejectedByNps: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CLASS2_NICS),
        documentDate = LocalDate.of(2021, 8, 13),
        originalAmount = 1000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        accruingInterestAmount = Some(0),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId2",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2022, 1, 29),
        originalAmount = 1000.00,
        outstandingAmount = 0,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 1, 23))
      )
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some("testTransactionId2"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 23)))))
      )
    )
  )

  val rejectedByNpsPartWay: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CLASS2_NICS),
        documentDate = LocalDate.of(2021, 1, 31),
        originalAmount = 1000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        accruingInterestAmount = Some(0),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId2",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CANCELLED),
        documentDate = LocalDate.of(2021, 8, 31),
        originalAmount = 1000.00,
        outstandingAmount = 1000.00,
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      )
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId2"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 23)),codedOutStatus = Some("C"))))
      )
    )
  )

  val codingOutPartiallyCollected: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId1",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CLASS2_NICS),
        documentDate = LocalDate.of(2021, 9, 13),
        originalAmount = 1000.00,
        outstandingAmount = 0,
        interestOutstandingAmount = Some(0.00),
        accruingInterestAmount = Some(0),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 8, 23)),
        documentDueDate = Some(LocalDate.of(2021, 8, 23))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId2",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2021, 8, 31),
        originalAmount = 250,
        outstandingAmount = 100,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 30)),
        documentDueDate = Some(LocalDate.of(2021, 4, 30))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId3",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CANCELLED),
        documentDate = LocalDate.of(2022, 1, 31),
        originalAmount = 1000.00,
        outstandingAmount = 0,
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        effectiveDateOfPayment = Some(LocalDate.of(2022, 1, 31)),
        documentDueDate = Some(LocalDate.of(2022, 1, 31))
      )
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId1"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2020, 7, 13)))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId2"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 8, 31)))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId3"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 31)),codedOutStatus = Some("C"))))
      )
    )
  )

  val financialDetailsDunningLockSuccess: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(DocumentDetail(
      taxYear = getCurrentTaxYearEnd.getYear,
      transactionId = "testDunningTransactionId",
      documentDescription = Some("ITSA- POA 1"),
      documentText = Some("documentText"),
      documentDate = LocalDate.of(2018, 3, 29),
      originalAmount = 1000.00,
      outstandingAmount = 500.00,
      interestOutstandingAmount = Some(0.00),
      interestEndDate = Some(LocalDate.of(2021, 6, 24)),
      accruingInterestAmount = Some(100.00),
      effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
      documentDueDate = Some(LocalDate.of(2021, 4, 23))
    ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testDunningTransactionId2",
        documentDescription = Some("ITSA- POA 2"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 2000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      )),
    financialDetails = List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId"),
        mainType = Some("SA Payment on Account 1"),
        mainTransaction = Some("4920"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)), amount = Some(12), dunningLock = Some("Stand over order"), transactionId = Some("testDunningTransactionId"))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId2"),
        mainType = Some("SA Payment on Account 2"),
        mainTransaction = Some("4930"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)), amount = Some(12), dunningLock = Some("Dunning Lock"), transactionId = Some("testDunningTransactionId2"))))
      )
    )
  )

  val financialDetailsMFADebits: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testMFA1",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 1234.00,
        outstandingAmount = 0,
        interestOutstandingAmount = None,
        interestEndDate = None,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testMFA2",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 2234.00,
        outstandingAmount = 0,
        interestOutstandingAmount = None,
        interestEndDate = None,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 22)),
        documentDueDate = Some(LocalDate.of(2021, 4, 22))
      )),
    financialDetails = List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testMFA1"),
        mainType = Some("ITSA PAYE Charge"),
        mainTransaction = Some("4000"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)), amount = Some(12), transactionId = Some("testMFA1"))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testMFA2"),
        mainType = Some("ITSA Calc Error Correction"),
        mainTransaction = Some("4001"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 22)), amount = Some(12), transactionId = Some("testMFA2"))))
      )
    )
  )

  val emptyPaymentsList: List[TaxYearSummaryChargeItem] = List.empty

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "ABC123456789",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#003",
          StatusFulfilled
        ))
    ),
    GroupedObligationsModel(
      identification = "ABC123456789",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#004",
          StatusFulfilled
        ))
    )
  ))

  def testUser(mtdUserRole: MTDUserRole, incomeSources: IncomeSourceDetailsModel = multipleBusinessesAndPropertyResponse): MtdItUser[_] = {
    getTestUser(mtdUserRole, incomeSources)
  }

}
