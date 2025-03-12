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

package testConstants

import enums.ChargeType.NIC4_WALES
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import services.{DateService, DateServiceInterface}
import testConstants.FinancialDetailsTestConstants.{currentYear, dueDateDueIn30Days, dueDateMoreThan30Days, dueDateOverdue, financialDetailsReviewAndReconcileInterest, fixedDate, id1040000123, id1040000124, id1040000125, id1040000126, noDunningLocks, oneDunningLock, outstandingChargesDueIn30Days, outstandingChargesDueInMoreThan30Days, outstandingChargesModel, testFinancialDetailsModelWithChargesOfSameType}

import java.time.LocalDate

trait ChargeConstants {

  implicit val dateService: DateServiceInterface

  def chargeItemModel(taxYear: TaxYear = TaxYear.forYearEnd(2018),
                      transactionId: String = id1040000123,
                      transactionType: TransactionType = PoaOneDebit,
                      subTransactionType: Option[SubTransactionType] = None,
                      documentDate: LocalDate = LocalDate.of(2018, 3, 29),
                      dueDate: Option[LocalDate] =  Some(LocalDate.of(2019, 5, 15)),
                      originalAmount : BigDecimal = 1400.00,
                      outstandingAmount: BigDecimal = 1400.00,
                      amountCodedOut: Option[BigDecimal] = None,
                      interestOutstandingAmount: Option[BigDecimal] = Some(80.0),
                      latePaymentInterestAmount: Option[BigDecimal] = Some(100.0),
                      interestRate: Option[BigDecimal] = Some(1.0),
                      interestFromDate:  Option[LocalDate] = Some(LocalDate.of(2018, 3, 29)),
                      interestEndDate: Option[LocalDate] = Some(LocalDate.of(2018, 6, 15)),
                      lpiWithDunningLock: Option[BigDecimal] = Some(100.0),
                      isOverdue:Boolean = true,
                      dunningLock:Boolean = false,
                      poaRelevantAmount: Option[BigDecimal] = None): ChargeItem = ChargeItem(
    transactionId = transactionId,
    taxYear = taxYear,
    transactionType = transactionType,
    subTransactionType = subTransactionType,
    documentDate = documentDate,
    dueDate = dueDate,
    originalAmount = originalAmount,
    outstandingAmount = outstandingAmount,
    interestOutstandingAmount = interestOutstandingAmount,
    latePaymentInterestAmount = latePaymentInterestAmount,
    interestFromDate = interestFromDate,
    interestEndDate = interestEndDate,
    lpiWithDunningLock = lpiWithDunningLock,
    dunningLock = dunningLock,
    interestRate = interestRate,
    amountCodedOut = amountCodedOut,
    poaRelevantAmount = poaRelevantAmount
  )

  private def testFinancialDetailsChargeItems(dueDate: List[Option[LocalDate]],
                                              dunningLock: List[Option[String]],
                                              documentDate: List[LocalDate] = List(LocalDate.of(2018, 3, 29), LocalDate.of(2018, 3, 29)),
                                              transactionId: List[String] = List(id1040000124, id1040000125),
                                              transactionTypes: List[ChargeType] = List(PoaOneDebit, PoaTwoDebit),
                                              subTransactionTypes: List[Option[SubTransactionType]] = List(None, None),
                                              originalAmount: List[BigDecimal] = List( 43.21, 12.34),
                                              outstandingAmount: List[BigDecimal] = List(50, 75),
                                              outstandingInterest: List[Option[BigDecimal]] = List(None, None),
                                              interestRate: List[Option[BigDecimal]] = List(None, None),
                                              latePaymentInterestAmount: List[Option[BigDecimal]] = List(Some(0.0), None),
                                              lpiWithDunningLock: List[Option[BigDecimal]] = List(None, None),
                                              interestFromDate:  List[Option[LocalDate]] =
                                                List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
                                              interestEndDate: List[Option[LocalDate]] =
                                                List(Some(LocalDate.of(2018, 6, 15)), Some(LocalDate.of(2018, 6, 15))),
                                              taxYear: String = fixedDate.getYear.toString,
                                              overdue: List[Boolean] = List(true, true),
                                              poaRelevantAmount: Option[BigDecimal] = None): List[ChargeItem] = {

    List(
      ChargeItem(transactionId = transactionId.head,
        taxYear = TaxYear.forYearEnd(taxYear.toInt),
        transactionType = transactionTypes.head,
        subTransactionType = subTransactionTypes.head,
        documentDate = documentDate.head,
        dueDate = dueDate.head,
        originalAmount = originalAmount.head,
        outstandingAmount = outstandingAmount.head,
        interestOutstandingAmount = outstandingInterest.head,
        latePaymentInterestAmount = latePaymentInterestAmount.head,
        interestFromDate = interestFromDate.head,
        interestEndDate = interestEndDate.head,
        interestRate = interestRate.head,
        lpiWithDunningLock = lpiWithDunningLock.head,
        amountCodedOut = None,
        dunningLock = dunningLock.head.isDefined,
        poaRelevantAmount = poaRelevantAmount),
      ChargeItem(transactionId = transactionId(1),
        taxYear = TaxYear.forYearEnd(taxYear.toInt),
        transactionType = transactionTypes(1),
        subTransactionType = subTransactionTypes(1),
        documentDate = documentDate(1),
        dueDate = dueDate(1),
        originalAmount = originalAmount(1),
        outstandingAmount = outstandingAmount(1),
        interestOutstandingAmount = outstandingInterest(1),
        latePaymentInterestAmount = latePaymentInterestAmount(1),
        interestFromDate = interestFromDate(1),
        interestEndDate = interestEndDate(1),
        interestRate = interestRate(1),
        lpiWithDunningLock = lpiWithDunningLock(1),
        amountCodedOut = None,
        dunningLock = dunningLock(1).isDefined,
        poaRelevantAmount = poaRelevantAmount)
    )
  }

  def financialDetailsOverdueInterestDataCi(latePaymentInterest: List[Option[BigDecimal]]): List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionTypes = List(PoaOneDebit, PoaTwoDebit),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks,
    outstandingAmount = List(50.0, 75.0),
    taxYear = fixedDate.getYear.toString,
    outstandingInterest = List(Some(42.50), Some(24.05)),
    interestRate = List(Some(2.6), Some(6.2)),
    interestFromDate = List(Some(LocalDate.of(2019, 5, 25)), Some(LocalDate.of(2019, 5, 25))),
    interestEndDate = List(Some(LocalDate.of(2019, 6, 25)), Some(LocalDate.of(2019, 6, 25))),
    latePaymentInterestAmount = latePaymentInterest,
    poaRelevantAmount = None
  )


  def financialDetailsOverdueWithLpi(
                                      latePaymentInterest: List[Option[BigDecimal]],
                                      dunningLock: List[Option[String]],
                                      lpiWithDunningLock: List[Option[BigDecimal]] = List(None, None)): List[ChargeItem] =
    testFinancialDetailsChargeItems(
      dueDate = List(Some(fixedDate.minusDays(10)), Some(fixedDate.minusDays(1))),
      dunningLock = dunningLock,
      lpiWithDunningLock = lpiWithDunningLock,
      outstandingAmount = List(50.0, 75.0),
      taxYear = fixedDate.getYear.toString,
      interestRate = List(Some(2.6), Some(6.2)),
      latePaymentInterestAmount = latePaymentInterest,
      poaRelevantAmount = None
    )
  def financialDetailsDueInMoreThan30DaysCi(dunningLocks: List[Option[String]] = noDunningLocks): List[ChargeItem] = testFinancialDetailsChargeItems(
    dueDate = dueDateMoreThan30Days,
    dunningLock = dunningLocks,
    interestFromDate = List(None, None),
    interestEndDate = List(None, None),
    poaRelevantAmount = None
  )

  def financialDetailsDueIn30DaysCi(dunningLocks: List[Option[String]] = noDunningLocks): List[ChargeItem] =
    testFinancialDetailsChargeItems(
      dueDate = dueDateDueIn30Days,
      interestFromDate = List(None, None),
      interestEndDate = List(None, None),
      dunningLock = dunningLocks,
      overdue = List(false, false),
      poaRelevantAmount = None
    )

  def outstandingChargesModelIt(dueDate: LocalDate): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456789012345.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))


  val outstandingChargesOverdueDataIt: OutstandingChargesModel = outstandingChargesModelIt(LocalDate.of(2022, 3, 5))

  val outstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(LocalDate.of(2023, 11, 15))

  def whatYouOweDataWithDataDueIn30DaysIt(implicit dateService: DateServiceInterface): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(
      ChargeItem(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000123",
        transactionType = BalancingCharge,
        subTransactionType = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = Some(80),
        interestRate = None,
        interestFromDate = Some(LocalDate.parse("2018-03-29")),
        interestEndDate = Some(LocalDate.parse("2023-11-15")),
        latePaymentInterestAmount = Some(100),
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01")), dunningLock = false,
        poaRelevantAmount = None),
      ChargeItem(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000124",
        transactionType = PoaOneDebit,
        subTransactionType = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        latePaymentInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01")), dunningLock = false,
        poaRelevantAmount = None),
      ChargeItem(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000125",
        transactionType = PoaTwoDebit,
        subTransactionType = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        latePaymentInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01")),
        dunningLock = false,
        poaRelevantAmount = None)
    ),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def financialDetailsOverdueDataCi(dunningLocks: List[Option[String]] = noDunningLocks): List[ChargeItem] =
    testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks
    )

  val financialDetailsMFADebitsCi: List[ChargeItem] = testFinancialDetailsChargeItems(
    dueDate = List(Some(fixedDate.minusDays(1)), Some(fixedDate.plusDays(35))),
    dunningLock = List(None, None),
    transactionTypes = List(MfaDebitCharge, MfaDebitCharge),
    outstandingAmount = List(100.0, 50.0),
    taxYear = fixedDate.getYear.toString,
    outstandingInterest = List(None, None),
    latePaymentInterestAmount = List(None, None)
  )

  val financialDetailsBalancingChargesCi: List[ChargeItem] = testFinancialDetailsChargeItems(
   transactionTypes = List(BalancingCharge, BalancingCharge),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks,
    interestFromDate = List(None, None),
    interestEndDate = List(None, None)
  )

  def chargeItemWithCodingOutNics2Ci(): ChargeItem = ChargeItem(
          transactionId = "CODINGOUT01", taxYear = TaxYear.forYearEnd(2021),
          transactionType = BalancingCharge,
          subTransactionType = Some(Nics2),
          outstandingAmount = 12.34,
          originalAmount = 43.21,
          documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None,
          interestRate = None,
          interestFromDate = Some(LocalDate.parse("2019-05-25")),
          interestEndDate = Some(LocalDate.parse("2019-06-25")),
          latePaymentInterestAmount = None,
          dueDate = Some(LocalDate.parse("2021-08-25")),
          lpiWithDunningLock = None, amountCodedOut = None,
          dunningLock = false,
          poaRelevantAmount = None
        )

  def chargeItemWithCodingOutCancelledPayeSaCi(): ChargeItem = ChargeItem(
    transactionId = "CODINGOUT01", taxYear = TaxYear.forYearEnd(2021),
    transactionType = BalancingCharge,
    subTransactionType = Some(Cancelled),
    outstandingAmount = 12.34,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    latePaymentInterestAmount = None,
    dueDate = Some(LocalDate.parse("2021-08-25")),
    lpiWithDunningLock = None, amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None
  )

  val poa1WithCodedOut = ChargeItem(
    transactionId = id1040000123,
    taxYear = TaxYear.forYearEnd(currentYear.toInt),
    transactionType = PoaOneDebit,
    subTransactionType = None,
    outstandingAmount = 1000,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate.plusDays(30)),
    latePaymentInterestAmount = None,
    interestOutstandingAmount = None,
    interestFromDate = Some( LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    lpiWithDunningLock = Some(100),
    interestRate = Some(100),
    amountCodedOut = Some(30),
    dunningLock = false,
    poaRelevantAmount = None
  )
  val poa2WithCodedOut = ChargeItem(
    transactionId = id1040000124,
    taxYear = TaxYear.forYearEnd(currentYear.toInt),
    transactionType = PoaTwoDebit,
    subTransactionType = None,
    outstandingAmount = 400,
    originalAmount = 12.34,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate.minusDays(1)),
    latePaymentInterestAmount = None,
    interestOutstandingAmount = None,
    interestFromDate = Some( LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    lpiWithDunningLock = Some(100),
    interestRate = Some(100),
    amountCodedOut = Some(70),
    dunningLock = false,
    poaRelevantAmount = None
  )

  val poa2 = ChargeItem(
      transactionId = id1040000124,
      taxYear = TaxYear.forYearEnd(currentYear.toInt),
      transactionType = PoaTwoDebit,
      subTransactionType = None,
      outstandingAmount = 0,
      originalAmount = 12.34,
      documentDate = LocalDate.of(2018, 3, 29),
      dueDate = Some(fixedDate.minusDays(1)),
      latePaymentInterestAmount = Some(10.0),
      interestOutstandingAmount = Some(10.0),
      interestFromDate = Some( LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 3, 29)),
      lpiWithDunningLock = Some(100.0),
      interestRate = Some(100.0),
      amountCodedOut = None,
      dunningLock = false,
      poaRelevantAmount = None
  )

  val balancingChargeNics2 = ChargeItem(
    taxYear = TaxYear.forYearEnd(2021),
    transactionId = id1040000124,
    transactionType = BalancingCharge,
    subTransactionType = Some(Nics2),
    originalAmount = 43.21,
    outstandingAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
    dueDate = Some(LocalDate.parse("2021-08-24")),
    lpiWithDunningLock = None,
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None)

  val balancingChargeCancelled = ChargeItem(taxYear = TaxYear.forYearEnd(2021),
    transactionId = id1040000125,
    transactionType = BalancingCharge,
    subTransactionType = Some(Cancelled),
    originalAmount = 43.21,
    outstandingAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    latePaymentInterestAmount = None,
    dueDate = Some(LocalDate.parse("2021-08-25")),
    lpiWithDunningLock = None,
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None)

  val  balancingChargePaye = balancingChargeNics2.copy(
    transactionId = id1040000126,
    subTransactionType = Some(Accepted),
    amountCodedOut = Some(2500.00))

  def financialDetailsOverdueWithLpiDunningLockZeroCi(taxYear: TaxYear,
                                                      latePaymentInterest: Option[BigDecimal],
                                                      dunningLock: Boolean,
                                                      lpiWithDunningLock: Option[BigDecimal]): List[ChargeItem] =

    List(
      ChargeItem(
        id1040000124,
        taxYear,
        transactionType = PoaOneDebit,
        subTransactionType = None,
        documentDate = LocalDate.of(2018, 3, 29),
        dueDate = Some(fixedDate.minusDays(10)),
        originalAmount = 43.21,
        outstandingAmount = 50.0,
        interestOutstandingAmount = None,
        latePaymentInterestAmount = latePaymentInterest,
        interestFromDate = Some(LocalDate.parse("2019-05-25")),
        interestEndDate = Some(LocalDate.parse("2019-06-25")),
        interestRate = Some(2.6),
        lpiWithDunningLock = lpiWithDunningLock,
        amountCodedOut = None,
        dunningLock = dunningLock,
        poaRelevantAmount = None
      ),
      ChargeItem(
        id1040000125,
        taxYear,
        transactionType = PoaOneDebit,
        subTransactionType = None,
        documentDate = LocalDate.of(2018, 3, 29),
        dueDate = Some(fixedDate.minusDays(1)),
        originalAmount = 12.34,
        outstandingAmount = 75.0,
        interestOutstandingAmount = None,
        latePaymentInterestAmount = latePaymentInterest,
        interestFromDate = Some(LocalDate.parse("2019-05-25")),
        interestEndDate = Some(LocalDate.parse("2019-06-25")),
        interestRate = Some(6.2),
        lpiWithDunningLock = lpiWithDunningLock,
        amountCodedOut = None,
        dunningLock = dunningLock,
        poaRelevantAmount = None
      )
    )

  def outstandingChargesModelCi(dueDate: LocalDate): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456789012345.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  val staticDateOutstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModelCi(LocalDate.parse("2022-01-01"))

    def whatYouOweWithAZeroOutstandingAmount(firstTransactionType: ChargeType = BalancingCharge)(implicit dateService: DateServiceInterface): WhatYouOweChargesList = WhatYouOweChargesList(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      chargesList = List(
        ChargeItem(
          taxYear = TaxYear.forYearEnd(2021),
          transactionId = "transId1",
          transactionType = firstTransactionType,
          subTransactionType = None,
          outstandingAmount = 1000,
          originalAmount = 3400,
          documentDate = LocalDate.parse("2018-03-29"),
          interestOutstandingAmount = None,
          interestRate = None,
          interestFromDate = None,
          interestEndDate = None,
          latePaymentInterestAmount = None,
          lpiWithDunningLock = None,
          amountCodedOut = None,
          dueDate = Some(LocalDate.parse("2018-02-14")),
          dunningLock = false,
          poaRelevantAmount = None),
        ChargeItem(
          taxYear = TaxYear.forYearEnd(2021),
          transactionId = "transId2",
          transactionType = PoaOneDebit,
          subTransactionType = None,
          outstandingAmount = 100,
          originalAmount = 1000,
          documentDate = LocalDate.parse("2018-03-29"),
          interestOutstandingAmount = None,
          interestRate = None,
          interestFromDate = None,
          interestEndDate = None,
          latePaymentInterestAmount = None,
          lpiWithDunningLock = None,
          amountCodedOut = None,
          dueDate = Some(LocalDate.parse("2022-01-01").plusDays(1)),
          dunningLock = false,
          poaRelevantAmount = None)),
      outstandingChargesModel = Some(staticDateOutstandingChargesOverdueData)
    )

  def whatYouOweDataWithPaidPOAs(dunningLocks: List[Option[String]] = noDunningLocks)(implicit dateService: DateService): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 0.00, 1.00, None, None, None, None, None),
    chargesList = financialDetailsDueIn30DaysCi(dunningLocks).map(_.copy(outstandingAmount = 0.0)),
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )

  def whatYouOweDataWithDataDueIn30Days(dunningLocks: List[Option[String]] = noDunningLocks)(implicit dateService: DateService): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 0.00, 1.00, None, None, None, None, None),
    chargesList = financialDetailsDueIn30DaysCi(dunningLocks),
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )

  def whatYouOweDataWithDataDueIn30DaysAvailableCreditZero(dunningLocks: List[Option[String]] = noDunningLocks)(implicit dateService: DateService): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 0.00, 1.00, Some(0.00), None, None, None, None),
    chargesList = financialDetailsDueIn30DaysCi(dunningLocks),
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )

  val financialDetailsBalancingChargeNotOverdue: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(BalancingCharge, BalancingCharge),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )

  val financialDetailsReviewAndReconcile: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("SA POA 1 Reconciliation Debit"), Some("SA POA 2 Reconciliation Debit")),
    mainType = List(Some("SA POA 1 Reconciliation"), Some("SA POA 2 Reconciliation")),
    mainTransaction = List(Some("4911"), Some("4913")),
    dueDate = List(Some(fixedDate.minusDays(1)), Some(fixedDate.plusDays(30))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = List(None, None)
  )

  val financialDetailsReviewAndReconcileNotYetDue: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("SA POA 1 Reconciliation Debit"), Some("SA POA 2 Reconciliation Debit")),
    mainType = List(Some("SA POA 1 Reconciliation"), Some("SA POA 2 Reconciliation")),
    mainTransaction = List(Some("4911"), Some("4913")),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = List(None, None)
  )

  val financialDetailsReviewAndReconcileCi: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(50, 75),
    outstandingInterest = List(Some(BigDecimal(10)),Some(BigDecimal(20))),
    interestRate = List(Some(BigDecimal(2)),Some(BigDecimal(2))),
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )

  val financialDetailsOverdueCharges: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoReconciliationDebit),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )

  val financialDetailsReviewAndReconcileNotYetDueCi: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoReconciliationDebit),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )

  val financialDetailsReviewAndReconcileInterest: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoReconciliationDebit),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(0, 0),
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = List(Some(150), Some(150)),
    dunningLock = noDunningLocks,
    outstandingInterest = List(Some(100), Some(40)),
    interestEndDate = List(Some(LocalDate.of(2100,1,1)), Some(LocalDate.of(2100,1,1))),
    interestRate = List(Some(10), Some(10))
  )

  val whatYouOweDataWithMixedData1: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      transactionId = List(id1040000123, id1040000124),
      taxYear = fixedDate.getYear.toString,
      dueDate = List(Some(fixedDate.plusDays(35)), Some(fixedDate.minusDays(1))),
      outstandingAmount = List(25.0, 50.0),
      outstandingInterest  = List(Some(100.0), Some(100.0)),
      latePaymentInterestAmount  = List(Some(100.0), Some(100.0)),
      lpiWithDunningLock = List(Some(100.0), Some(100.0)),
      interestRate = List(Some(100.0), Some(100.0)),
      interestFromDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      interestEndDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      dunningLock = noDunningLocks).reverse,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweDataWithMixedData2: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      transactionId = List(id1040000123, id1040000124),
      dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
      outstandingAmount = List(50, 75),
      outstandingInterest  = List(Some(100.0), Some(100.0)),
      latePaymentInterestAmount  = List(Some(100.0), Some(100.0)),
      lpiWithDunningLock = List(Some(100.0), Some(100.0)),
      interestRate = List(Some(100.0), Some(100.0)),
      interestFromDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      interestEndDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      dunningLock = noDunningLocks).reverse,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val financialDetailsWithMixedData4Ci: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50, 75),
    outstandingInterest  = List(Some(100.0), Some(100.0)),
    latePaymentInterestAmount  = List(None, None),
    lpiWithDunningLock = List(Some(100.0), Some(100.0)),
    interestRate = List(Some(100.0), Some(100.0)),
    interestEndDate = List(Some(LocalDate.of(2018,3,29)),Some(LocalDate.of(2018,3,29))),
    taxYear = fixedDate.getYear.toString,
    dunningLock = List(None, None)
  )

  val whatYouOweDataWithMixedData4Unfiltered: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData4Ci.head,
      financialDetailsWithMixedData4Ci(1)),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )
  val whatYouOweDataWithMixedData4Filtered: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData4Ci(1)),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweWithReviewReconcileData: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsReviewAndReconcileCi,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweWithReviewReconcileDataNotYetDue: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsReviewAndReconcileNotYetDueCi,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweReconciliationInterestData: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsReviewAndReconcileInterest,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val financialDetailsWithMixedData1Ci: List[ChargeItem] = testFinancialDetailsChargeItems(
    dueDate = List(Some(fixedDate.plusDays(35)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(25.0, 50.0),
    taxYear = fixedDate.getYear.toString,
    dunningLock = List(None, None)
  )

  val financialDetailsWithMixedData2Ci: List[ChargeItem] = testFinancialDetailsChargeItems(
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50.0, 75.0),
    taxYear = fixedDate.getYear.toString,
    dunningLock = List(None, None)
  )

  val financialDetailsWithMixedData3Ci: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50.0, 75.0),
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = List(None, None),
    dunningLock = List(None, None)
  )

  def whatYouOweDataWithOverdueDataIt(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks,
      interestFromDate = List(None, None),
      interestEndDate = List(None, None)
    ),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueData(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks,
      interestFromDate = List(None, None),
      interestEndDate = List(None, None)
    ),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )



  def whatYouOweDataWithOverdueDataAndInterest(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks,
      outstandingInterest = List(Some(100), Some(100)),
      interestRate = List(Some(1.0), Some(1.0))
    ),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )


  def whatYouOweDataWithDataDueInMoreThan30Days(dunningLocks: List[Option[String]] = noDunningLocks, dueDates: List[Option[LocalDate]] = dueDateMoreThan30Days): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00, Some(BigDecimal(100.00)), None, None, None, Some(BigDecimal(100.00))),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDates,
      dunningLock = dunningLocks,
      interestEndDate = List(None, None),
      interestFromDate = List(None, None)
    ),
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )


  def whatYouOweDataWithZeroMoneyInAccount(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00, None, None, None, None, Some(BigDecimal(0.00))),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateMoreThan30Days,
      dunningLock = dunningLocks
    ),
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )





  val codedOutChargeItemsA: ChargeItem = ChargeItem(
    id1040000124,
    TaxYear.forYearEnd(2022),
    PoaOneDebit,
    None,
    LocalDate.of(2018, 3, 29),
    Some(LocalDate.of(2018, 3, 29)),
    100.0,
    43.21,
    Some(100),
    Some(5.0),
    None, None, Some(5.0),
    None, Some(2500.0),
    false,
    None)

  val codedOutDocumentDetailCi: ChargeItem = ChargeItem(
    transactionId = "CODINGOUT02",
    taxYear = TaxYear.forYearEnd(2021),
    transactionType = BalancingCharge,
    subTransactionType = Some(Nics2),
    outstandingAmount = 12.34,
    originalAmount = 43.21,
    documentDate =LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    latePaymentInterestAmount = None,
    amountCodedOut = Some(43.21),
    lpiWithDunningLock = None,
    dunningLock = false,
    dueDate = None,
    poaRelevantAmount = None)


  val codedOutDocumentDetailPayeSACi: ChargeItem = ChargeItem(
    transactionId = "CODINGOUT02",
    taxYear = TaxYear.forYearEnd(2021),
    transactionType = BalancingCharge,
    subTransactionType = Some(Accepted),
    outstandingAmount = 0.00,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = Some(0.0),
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    latePaymentInterestAmount = None,
    amountCodedOut = Some(43.21),
    lpiWithDunningLock = None,
    dunningLock = false,
    dueDate = None,
    poaRelevantAmount = None
  )

  val codedOutDocumentDetailFullyCollectedCi: ChargeItem = ChargeItem(
    taxYear = TaxYear.forYearEnd(2021),
    transactionId = "CODINGOUT02",
    transactionType = BalancingCharge,
    subTransactionType = Some(Nics2),
    outstandingAmount = 12.34,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    latePaymentInterestAmount = None,
    amountCodedOut = Some(0),
    dueDate = None,
    lpiWithDunningLock = None,
    dunningLock = false,
    poaRelevantAmount = None
  )

  def testFinancialDetailsModelOneItemInList(documentDescription: Option[String],
                                             mainType: Option[String],
                                             mainTransaction: Option[String],
                                             dueDate: Option[LocalDate],
                                             outstandingAmount: BigDecimal,
                                             taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription, Some("documentText"), outstandingAmount, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100),
          Some("latePaymentInterestId"), Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), None, Some(100), Some("paymentLotItem"), Some("paymentLot"),
          effectiveDateOfPayment = dueDate,
          documentDueDate = dueDate)
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType, mainTransaction, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate))))
      )
    )

  def testFinancialDetailsModelWithInterest(documentDescription: List[Option[String]] = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
                                            mainType: List[Option[String]] = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
                                            mainTransaction: List[Option[String]] = List(Some("4920"), Some("4930")),
                                            dueDate: List[Option[LocalDate]],
                                            dunningLock: List[Option[String]],
                                            outstandingAmount: List[BigDecimal] = List(50, 75),
                                            taxYear: String = fixedDate.getYear.toString,
                                            interestOutstandingAmount: List[Option[BigDecimal]] = List(Some(100), Some(100)),
                                            interestRate: List[Option[BigDecimal]] = List(Some(100), Some(100)),
                                            latePaymentInterestAmount: List[Option[BigDecimal]] = List(None, None)): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), interestOutstandingAmount.head, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount.head, effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), interestOutstandingAmount(1), interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount(1), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = dueDate.head, dunningLock = dunningLock.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = dueDate(1), dunningLock = dunningLock(1)))))
      )
    )

  val whatYouOwePartialChargesListX: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(balanceDueWithin30Days = 1.00, overDueAmount = 2.00, totalBalance = 3.00, None, None, None, None, None),
    chargesList =  testFinancialDetailsChargeItems(
      dueDate = List(Some(LocalDate.of(2019, 6, 25)), Some(LocalDate.of(2023, 12, 14))),
      documentDate = List(LocalDate.of(2018, 3, 29), LocalDate.of(2018, 3, 29)),
      interestFromDate = List(Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-05-25"))),
      interestEndDate = List(Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25"))),
      dunningLock = oneDunningLock,
      outstandingInterest = List(Some(42.50), Some(24.05)),
      interestRate = List(Some(2.6), Some(6.2)),
      latePaymentInterestAmount = List(Some(54.56), Some(98.76)),
      outstandingAmount = List(50, 75),
      taxYear = fixedDate.getYear.toString
    ) ++ Seq(chargeItemModel(
      transactionId = id1040000124,
      transactionType = PoaTwoDebit,
      documentDate = LocalDate.of(2018, 3, 29),
      dueDate = Some(LocalDate.of(2023, 12, 16)),
      interestOutstandingAmount = Some(100.0),
      interestFromDate = Some(LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 3, 29)),
      outstandingAmount = 100,
      interestRate = Some(100.0),
      latePaymentInterestAmount = None,
      taxYear = TaxYear.forYearEnd(fixedDate.getYear)
    )) ++ Seq(chargeItemModel(
      transactionId = id1040000124,
      transactionType = PoaOneDebit,
      documentDate = LocalDate.of(2018, 3, 29),
      dueDate = Some(fixedDate.plusDays(45)),
      interestOutstandingAmount = Some(100.0),
      outstandingAmount = 125,
      interestRate = Some(100.0),
      latePaymentInterestAmount = None,
      interestFromDate = Some(LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 3, 29)),
      taxYear = TaxYear.forYearEnd(fixedDate.getYear)
    )),
    outstandingChargesModel = Some(outstandingChargesOverdueData),
    codedOutDocumentDetail = Some(codedOutChargeItemsA)
  )

  val whatYouOwePartialChargesList: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(balanceDueWithin30Days = 1.00, overDueAmount = 2.00, totalBalance = 3.00, None, None, None, None, None),
    chargesList =  testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = oneDunningLock,
      outstandingInterest = List(Some(42.50), Some(24.05)),
      interestRate = List(Some(2.6), Some(6.2)),
      latePaymentInterestAmount = List(Some(34.56), Some(34.56)),
      outstandingAmount = List(50, 75)
    ) ++ Seq(chargeItemModel(
      transactionId = id1040000124,
      transactionType = PoaTwoDebit,
      dueDate = Some(fixedDate.plusDays(1)),
      outstandingAmount = 100,
      taxYear = TaxYear.forYearEnd(fixedDate.getYear)
    )) ++ Seq(chargeItemModel(
      transactionId = id1040000124,
      transactionType = PoaOneDebit,
      dueDate = Some(fixedDate.plusDays(45)),
      outstandingAmount = 125,
      taxYear = TaxYear.forYearEnd(fixedDate.getYear)
    )),
    codedOutDocumentDetail = Some(codedOutChargeItemsA)
  )

  val whatYouOweDataWithMFADebitsData: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      transactionId = List(id1040000123, id1040000124),
      dueDate = List(Some(fixedDate.minusDays(1)), Some(fixedDate.plusDays(35))),
      dunningLock = noDunningLocks,
      transactionTypes = List(MfaDebitCharge, MfaDebitCharge),
      outstandingAmount = List(100, 50),
      taxYear = fixedDate.getYear.toString,
      interestRate =  List(Some(100.0), Some(100.0)),
      lpiWithDunningLock = List(Some(100.0), Some(100.0)),
      outstandingInterest = List(None, None),
      interestEndDate = List(Some(LocalDate.of(2018,3,29)), Some(LocalDate.of(2018,3,29))),
      latePaymentInterestAmount = List(None, None)
    ),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  def whatYouOweDataWithAvailableCredits(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00, Some(300.00), None, None, None, Some(BigDecimal(100.00))),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateMoreThan30Days,
      dunningLock = dunningLocks
    ),
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )

  /// integration tests

  def whatYouOweDataWithDataDueIn30DaysIntegration: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(
      chargeItemModel(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000123",
        transactionType = BalancingCharge,
        subTransactionType = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = Some(80),
        interestRate =None,
        interestFromDate =Some(LocalDate.parse("2018-03-29")),
        interestEndDate =Some(LocalDate.parse("2018-03-29")),
        latePaymentInterestAmount =Some(100),
        lpiWithDunningLock =None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2018-03-29"))),
      chargeItemModel(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000124",
        transactionType = PoaOneDebit,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        latePaymentInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01"))),
      chargeItemModel(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000125",
        transactionType = PoaTwoDebit,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        latePaymentInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01")))),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

    def whatYouOweDataWithDataDueInSomeDays(implicit dateService: DateServiceInterface): WhatYouOweChargesList = {
      val inScopeChargeList = List(
        chargeItemModel(
            taxYear = TaxYear.forYearEnd(2021),
            transactionId = "1040000123",
            transactionType = BalancingCharge,
            subTransactionType = Some(Nics2),
            outstandingAmount = 2000,
            originalAmount = 2000,
            documentDate = LocalDate.parse("2018-03-29"),
            interestOutstandingAmount = Some(80),
            interestRate = None,
            interestFromDate = Some(LocalDate.parse("2018-03-29")),
            interestEndDate = Some(LocalDate.parse("2018-03-29")),
            latePaymentInterestAmount = Some(100),
            lpiWithDunningLock = None,
            amountCodedOut = None,
            dueDate = Some(LocalDate.parse("2022-01-01"))),
          chargeItemModel(
            taxYear = TaxYear.forYearEnd(2021),
            transactionId = "1040000124",
            transactionType = PoaOneDebit,
            subTransactionType = None,
            outstandingAmount = 2000,
            originalAmount = 2000,
            documentDate = LocalDate.parse("2018-03-29"),
            interestOutstandingAmount = None,
            interestRate = None,
            interestFromDate = None,
            interestEndDate = None,
            latePaymentInterestAmount = None,
            lpiWithDunningLock = None,
            amountCodedOut = None,
            dueDate = Some(LocalDate.parse("2022-01-01"))),
          chargeItemModel(
            taxYear = TaxYear.forYearEnd(2021),
            transactionId = "1040000125",
            transactionType = PoaTwoDebit,
            subTransactionType = None,
            outstandingAmount = 2000,
            originalAmount = 2000,
            documentDate = LocalDate.parse("2018-03-29"),
            interestOutstandingAmount = None,
            interestRate = None,
            interestFromDate = None,
            interestEndDate = None,
            latePaymentInterestAmount = None,
            lpiWithDunningLock = None,
            amountCodedOut = None,
            dueDate = Some(LocalDate.parse("2022-01-01")))
      )


      WhatYouOweChargesList(
        balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
        chargesList = inScopeChargeList,
        outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
      )
    }


    def whatYouOweDataFullDataWithoutOutstandingCharges()(implicit dateService: DateServiceInterface): WhatYouOweChargesList = WhatYouOweChargesList(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      chargesList = financialDetailsOverdueDataCi()
    )

  def financialDetailsOverdueDataCi() =  testFinancialDetailsChargeItems(
    transactionId = List("transId1", "transId2"),
    documentDate = List(LocalDate.parse("2020-08-16"), LocalDate.parse("2020-08-16")),
    originalAmount = List(100.0, 100.0),
    outstandingAmount = List(2000.0, 2000.0),
    dueDate = List(Some(dateService.getCurrentDate.minusDays(15)), Some(dateService.getCurrentDate.minusDays(15))),
    dunningLock = List(None, None),
    interestFromDate = List(None, None),
    interestEndDate = List(None, None),
    taxYear = dateService.getCurrentDate.getYear.toString
  )

}
