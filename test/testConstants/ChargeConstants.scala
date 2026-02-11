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
import testConstants.FinancialDetailsTestConstants.{currentYear, dueDateDueIn30Days, dueDateMoreThan30Days, dueDateOverdue, fixedDate, futureFixedDate, id1040000123, id1040000124, id1040000125, id1040000126, noDunningLocks, oneDunningLock, outstandingChargesDueIn30Days, outstandingChargesDueInMoreThan30Days, outstandingChargesModel, testFinancialDetailsModelWithChargesOfSameType}

import java.time.LocalDate

trait ChargeConstants {

  implicit def dateService: DateServiceInterface

  def chargeItemModel(taxYear: TaxYear = TaxYear.forYearEnd(2018),
                      transactionId: String = id1040000123,
                      transactionType: TransactionType = PoaOneDebit,
                      codedOutStatus: Option[CodedOutStatusType] = None,
                      documentDate: LocalDate = LocalDate.of(2018, 3, 29),
                      dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                      originalAmount: BigDecimal = 1400.00,
                      outstandingAmount: BigDecimal = 1400.00,
                      amountCodedOut: Option[BigDecimal] = None,
                      interestOutstandingAmount: Option[BigDecimal] = Some(80.0),
                      accruingInterestAmount: Option[BigDecimal] = Some(100.0),
                      latePaymentInterestAmount: Option[BigDecimal] = Some(100.0),
                      interestRate: Option[BigDecimal] = Some(1.0),
                      interestFromDate: Option[LocalDate] = Some(LocalDate.of(2018, 3, 29)),
                      interestEndDate: Option[LocalDate] = Some(LocalDate.of(2018, 6, 15)),
                      lpiWithDunningLock: Option[BigDecimal] = Some(100.0),
                      dunningLock: Boolean = false,
                      poaRelevantAmount: Option[BigDecimal] = None,
                      chargeReference: Option[String] = None): ChargeItem = ChargeItem(
    transactionId = transactionId,
    taxYear = taxYear,
    transactionType = transactionType,
    codedOutStatus = codedOutStatus,
    documentDate = documentDate,
    dueDate = dueDate,
    originalAmount = originalAmount,
    outstandingAmount = outstandingAmount,
    interestOutstandingAmount = interestOutstandingAmount,
    accruingInterestAmount = accruingInterestAmount,
    latePaymentInterestAmount = latePaymentInterestAmount,
    interestFromDate = interestFromDate,
    interestEndDate = interestEndDate,
    lpiWithDunningLock = lpiWithDunningLock,
    dunningLock = dunningLock,
    interestRate = interestRate,
    amountCodedOut = amountCodedOut,
    poaRelevantAmount = poaRelevantAmount,
    chargeReference = chargeReference
  )

  private def testFinancialDetailsChargeItems(dueDate: List[Option[LocalDate]],
                                              dunningLock: List[Option[String]],
                                              documentDate: List[LocalDate] = List(LocalDate.of(2018, 3, 29), LocalDate.of(2018, 3, 29)),
                                              transactionId: List[String] = List(id1040000124, id1040000125),
                                              transactionTypes: List[ChargeType] = List(PoaOneDebit, PoaTwoDebit),
                                              codedOutStatuses: List[Option[CodedOutStatusType]] = List(None, None),
                                              originalAmount: List[BigDecimal] = List(43.21, 12.34),
                                              outstandingAmount: List[BigDecimal] = List(50, 75),
                                              outstandingInterest: List[Option[BigDecimal]] = List(None, None),
                                              interestRate: List[Option[BigDecimal]] = List(None, None),
                                              accruingInterestAmount: List[Option[BigDecimal]] = List(Some(0.0), None),
                                              latePaymentInterestAmount: List[Option[BigDecimal]] = List(Some(0.0), None),
                                              lpiWithDunningLock: List[Option[BigDecimal]] = List(None, None),
                                              interestFromDate: List[Option[LocalDate]] =
                                              List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
                                              interestEndDate: List[Option[LocalDate]] =
                                              List(Some(LocalDate.of(2018, 6, 15)), Some(LocalDate.of(2018, 6, 15))),
                                              taxYear: String = fixedDate.getYear.toString,
                                              overdue: List[Boolean] = List(true, true),
                                              poaRelevantAmount: Option[BigDecimal] = None,
                                              dueDateForFinancialDetail: List[LocalDate] = List.empty,
                                              chargeReference: List[Option[String]] = List(Some("ABCD1234"), Some("ABCD1234"))): List[ChargeItem] = {

    List(
      ChargeItem(transactionId = transactionId.head,
        taxYear = TaxYear.forYearEnd(taxYear.toInt),
        transactionType = transactionTypes.head,
        codedOutStatus = codedOutStatuses.head,
        documentDate = documentDate.head,
        dueDate = dueDate.head,
        originalAmount = originalAmount.head,
        outstandingAmount = outstandingAmount.head,
        interestOutstandingAmount = outstandingInterest.head,
        accruingInterestAmount = accruingInterestAmount.head,
        latePaymentInterestAmount = latePaymentInterestAmount.head,
        interestFromDate = interestFromDate.head,
        interestEndDate = interestEndDate.head,
        interestRate = interestRate.head,
        lpiWithDunningLock = lpiWithDunningLock.head,
        amountCodedOut = None,
        dunningLock = dunningLock.head.isDefined,
        poaRelevantAmount = poaRelevantAmount,
        dueDateForFinancialDetail = dueDateForFinancialDetail.headOption,
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"),
        chargeReference = chargeReference.head),
      ChargeItem(transactionId = transactionId(1),
        taxYear = TaxYear.forYearEnd(taxYear.toInt),
        transactionType = transactionTypes(1),
        codedOutStatus = codedOutStatuses(1),
        documentDate = documentDate(1),
        dueDate = dueDate(1),
        originalAmount = originalAmount(1),
        outstandingAmount = outstandingAmount(1),
        interestOutstandingAmount = outstandingInterest(1),
        accruingInterestAmount = accruingInterestAmount(1),
        latePaymentInterestAmount = latePaymentInterestAmount(1),
        interestFromDate = interestFromDate(1),
        interestEndDate = interestEndDate(1),
        interestRate = interestRate(1),
        lpiWithDunningLock = lpiWithDunningLock(1),
        amountCodedOut = None,
        dunningLock = dunningLock(1).isDefined,
        poaRelevantAmount = poaRelevantAmount,
        dueDateForFinancialDetail = if (dueDateForFinancialDetail.isEmpty) None else dueDateForFinancialDetail.tail.headOption,
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"),
        chargeReference = chargeReference(1)
      )
    )
  }

  def financialDetailsOverdueInterestDataCi(accruingInterestAmount: List[Option[BigDecimal]]): List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionTypes = List(PoaOneDebit, PoaTwoDebit),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks,
    outstandingAmount = List(50.0, 75.0),
    taxYear = fixedDate.getYear.toString,
    outstandingInterest = List(Some(42.50), Some(24.05)),
    interestRate = List(Some(2.6), Some(6.2)),
    interestFromDate = List(Some(LocalDate.of(2019, 5, 25)), Some(LocalDate.of(2019, 5, 25))),
    interestEndDate = List(Some(LocalDate.of(2019, 6, 25)), Some(LocalDate.of(2019, 6, 25))),
    accruingInterestAmount = accruingInterestAmount,
    poaRelevantAmount = None
  )

  def financialDetailsLatePaymentPenalties: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionTypes = List(FirstLatePaymentPenalty, SecondLatePaymentPenalty),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks,
    outstandingAmount = List(50.0, 75.0),
    taxYear = fixedDate.getYear.toString,
    outstandingInterest = List(Some(42.50), Some(24.05)),
    interestRate = List(Some(2.6), Some(6.2)),
    interestFromDate = List(Some(LocalDate.of(2019, 5, 25)), Some(LocalDate.of(2019, 5, 25))),
    interestEndDate = List(Some(LocalDate.of(2019, 6, 25)), Some(LocalDate.of(2019, 6, 25))),
    poaRelevantAmount = None
  )

  def financialDetailsLateSubmissionPenalty: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionTypes = List(LateSubmissionPenalty, PoaTwoDebit),
    dueDate = List(Some(fixedDate.minusMonths(2)), Some(fixedDate.minusMonths(3))),
    dunningLock = noDunningLocks,
    outstandingAmount = List(100.01, 75.99),
    taxYear = fixedDate.getYear.toString,
    outstandingInterest = List(Some(42.57), Some(24.99)),
    interestRate = List(Some(2.6), Some(6.2)),
    interestFromDate = List(Some(LocalDate.of(2019, 5, 25)), Some(LocalDate.of(2019, 5, 25))),
    interestEndDate = List(Some(LocalDate.of(2019, 6, 25)), Some(LocalDate.of(2019, 6, 25))),
    poaRelevantAmount = None
  )

  def financialDetailsOverdueWithLpi(
                                      accruingInterestAmount: List[Option[BigDecimal]],
                                      outstandingInterest: List[Option[BigDecimal]],
                                      dunningLock: List[Option[String]],
                                      lpiWithDunningLock: List[Option[BigDecimal]] = List(None, None),
                                      outstandingAmount: List[BigDecimal] = List(50.0, 75.0)): List[ChargeItem] =
    testFinancialDetailsChargeItems(
      dueDate = List(Some(fixedDate.minusDays(10)), Some(fixedDate.minusDays(1))),
      dunningLock = dunningLock,
      lpiWithDunningLock = lpiWithDunningLock,
      outstandingAmount = outstandingAmount,
      taxYear = fixedDate.getYear.toString,
      interestRate = List(Some(2.6), Some(6.2)),
      accruingInterestAmount = accruingInterestAmount,
      outstandingInterest = outstandingInterest,
      poaRelevantAmount = None
    )

  def financialDetailsDueInMoreThan30DaysCi(dunningLocks: List[Option[String]] = noDunningLocks): List[ChargeItem] = testFinancialDetailsChargeItems(
    dueDate = dueDateMoreThan30Days,
    dunningLock = dunningLocks,
    interestFromDate = List(None, None),
    interestEndDate = List(None, None),
    poaRelevantAmount = None,
    dueDateForFinancialDetail = List(LocalDate.parse("2024-01-29"), LocalDate.parse("2024-02-03"))
  )

  def financialDetailsDueIn30DaysCi(dunningLocks: List[Option[String]] = noDunningLocks): List[ChargeItem] =
    testFinancialDetailsChargeItems(
      dueDate = dueDateDueIn30Days,
      interestFromDate = List(None, None),
      interestEndDate = List(None, None),
      dunningLock = dunningLocks,
      overdue = List(false, false),
      poaRelevantAmount = None,
      dueDateForFinancialDetail = List(LocalDate.parse("2023-12-15"), LocalDate.parse("2023-12-16"))
    )

  def outstandingChargesModelIt(dueDate: LocalDate): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456789012345.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))


  val outstandingChargesOverdueDataIt: OutstandingChargesModel = outstandingChargesModelIt(LocalDate.of(2022, 3, 5))

  val outstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(LocalDate.of(2023, 11, 15))

  def whatYouOweDataWithDataDueIn30DaysIt(implicit dateService: DateServiceInterface): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = List(
      ChargeItem(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000123",
        transactionType = BalancingCharge,
        codedOutStatus = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = Some(80),
        interestRate = None,
        interestFromDate = Some(LocalDate.parse("2018-03-29")),
        interestEndDate = Some(LocalDate.parse("2023-11-15")),
        accruingInterestAmount = Some(100),
        latePaymentInterestAmount = Some(100),
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01")), dunningLock = false,
        poaRelevantAmount = None,
        chargeReference = Some("chargeRef1")),
      ChargeItem(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000124",
        transactionType = PoaOneDebit,
        codedOutStatus = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        accruingInterestAmount = None,
        latePaymentInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01")), dunningLock = false,
        poaRelevantAmount = None,
        chargeReference = Some("chargeRef2")),
      ChargeItem(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000125",
        transactionType = PoaTwoDebit,
        codedOutStatus = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        accruingInterestAmount = None,
        latePaymentInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01")),
        dunningLock = false,
        poaRelevantAmount = None,
        chargeReference = Some("chargeRef3"))
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
    accruingInterestAmount = List(None, None)
  )

  val financialDetailsBalancingChargesCi: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionTypes = List(BalancingCharge, BalancingCharge),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks,
    interestFromDate = List(None, None),
    interestEndDate = List(None, None),
    dueDateForFinancialDetail = List(LocalDate.parse("2023-12-05"), LocalDate.parse("2023-12-14"))
  )

  def chargeItemWithCodingOutNics2Ci(): ChargeItem = ChargeItem(
    transactionId = "CODINGOUT01", taxYear = TaxYear.forYearEnd(2021),
    transactionType = BalancingCharge,
    codedOutStatus = Some(Nics2),
    outstandingAmount = 0,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    dueDate = Some(LocalDate.parse("2021-08-25")),
    lpiWithDunningLock = None, amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef")
  )

  def chargeItemWithCodingOutCancelledPayeSaCi(): ChargeItem = ChargeItem(
    transactionId = "CODINGOUT01", taxYear = TaxYear.forYearEnd(2021),
    transactionType = BalancingCharge,
    codedOutStatus = Some(Cancelled),
    outstandingAmount = 12.34,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    dueDate = Some(LocalDate.parse("2021-08-25")),
    lpiWithDunningLock = None, amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2024-01-14")),
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("chargeRef")
  )

  def chargeItemWithPoaCodingOutAccepted(): ChargeItem = ChargeItem(
    transactionId = "CODINGOUT01", taxYear = TaxYear.forYearEnd(2021),
    transactionType = PoaOneDebit,
    codedOutStatus = Some(FullyCollected),
    outstandingAmount = 12.34,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    dueDate = Some(LocalDate.parse("2021-08-25")),
    lpiWithDunningLock = None, amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2024-01-14")),
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("chargeRef")
  )

  val poa1WithCodedOut = ChargeItem(
    transactionId = id1040000123,
    taxYear = TaxYear.forYearEnd(currentYear.toInt),
    transactionType = PoaOneDebit,
    codedOutStatus = None,
    outstandingAmount = 1000,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate.plusDays(30)),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    interestOutstandingAmount = None,
    interestFromDate = Some(LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    lpiWithDunningLock = Some(100),
    interestRate = Some(100),
    amountCodedOut = Some(30),
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2024-01-14")),
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("ABCD1234")
  )

  val poa1WithCodingOutAccepted = ChargeItem(
    transactionId = id1040000123,
    taxYear = TaxYear.forYearEnd(currentYear.toInt),
    transactionType = PoaOneDebit,
    codedOutStatus = Some(Accepted),
    outstandingAmount = 1000,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate.plusDays(30)),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    interestOutstandingAmount = None,
    interestFromDate = Some(LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    lpiWithDunningLock = Some(100),
    interestRate = Some(100),
    amountCodedOut = Some(30),
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2024-01-14")),
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("ABCD1234")
  )

  val poa2WithCodedOut = ChargeItem(
    transactionId = id1040000124,
    taxYear = TaxYear.forYearEnd(currentYear.toInt),
    transactionType = PoaTwoDebit,
    codedOutStatus = None,
    outstandingAmount = 400,
    originalAmount = 12.34,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate.minusDays(1)),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    interestOutstandingAmount = None,
    interestFromDate = Some(LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    lpiWithDunningLock = Some(100),
    interestRate = Some(100),
    amountCodedOut = Some(70),
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2023-12-14")),
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("ABCD1234")
  )

  val poa2WithCodingutAccepted = ChargeItem(
    transactionId = id1040000124,
    taxYear = TaxYear.forYearEnd(currentYear.toInt),
    transactionType = PoaTwoDebit,
    codedOutStatus = Some(Accepted),
    outstandingAmount = 400,
    originalAmount = 12.34,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate.minusDays(1)),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    interestOutstandingAmount = None,
    interestFromDate = Some(LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    lpiWithDunningLock = Some(100),
    interestRate = Some(100),
    amountCodedOut = Some(70),
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2023-12-14")),
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("ABCD1234")
  )

  val poa1 = ChargeItem(
    transactionId = id1040000124,
    taxYear = TaxYear.forYearEnd(currentYear.toInt),
    transactionType = PoaOneDebit,
    codedOutStatus = None,
    outstandingAmount = 12.34,
    originalAmount = 12.34,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate.minusDays(1)),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    interestOutstandingAmount = Some(34.56),
    interestFromDate = Some(fixedDate),
    interestEndDate = Some(fixedDate),
    lpiWithDunningLock = None,
    interestRate = Some(4.0),
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2023-12-14")),
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("ABCD1234")
  )

  val poa2 = ChargeItem(
    transactionId = id1040000124,
    taxYear = TaxYear.forYearEnd(currentYear.toInt),
    transactionType = PoaTwoDebit,
    codedOutStatus = None,
    outstandingAmount = 0,
    originalAmount = 12.34,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(fixedDate.minusDays(1)),
    accruingInterestAmount = Some(10.0),
    latePaymentInterestAmount = Some(10.0),
    interestOutstandingAmount = Some(10.0),
    interestFromDate = Some(LocalDate.of(2018, 3, 29)),
    interestEndDate = Some(LocalDate.of(2018, 3, 29)),
    lpiWithDunningLock = Some(100.0),
    interestRate = Some(100.0),
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2023-12-14")),
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("ABCD1234")
  )

  val poa1WithFutureDueDate = ChargeItem(
    transactionId = id1040000123,
    taxYear = TaxYear.forYearEnd(2030),
    transactionType = PoaOneDebit,
    codedOutStatus = None,
    outstandingAmount = 2500,
    originalAmount = 4000,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(futureFixedDate),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    interestOutstandingAmount = None,
    interestFromDate = None,
    interestEndDate = None,
    lpiWithDunningLock = None,
    interestRate = None,
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = Some(2500),
    dueDateForFinancialDetail = None,
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("chargeRef")
  )

  val poa2WithFutureDueDate = ChargeItem(
    transactionId = id1040000123,
    taxYear = TaxYear.forYearEnd(2030),
    transactionType = PoaTwoDebit,
    codedOutStatus = None,
    outstandingAmount = 3500,
    originalAmount = 4000,
    documentDate = LocalDate.of(2018, 3, 29),
    dueDate = Some(futureFixedDate.plusYears(2)),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    interestOutstandingAmount = None,
    interestFromDate = None,
    interestEndDate = None,
    lpiWithDunningLock = None,
    interestRate = None,
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = Some(2500),
    dueDateForFinancialDetail = None,
    paymentLotItem = Some("paymentLotItem"),
    paymentLot = Some("paymentLot"),
    chargeReference = Some("chargeRef")
  )

  val balancingChargeNics2 = ChargeItem(
    taxYear = TaxYear.forYearEnd(2021),
    transactionId = id1040000124,
    transactionType = BalancingCharge,
    codedOutStatus = Some(Nics2),
    originalAmount = 43.21,
    outstandingAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    dueDate = Some(LocalDate.parse("2021-08-24")),
    lpiWithDunningLock = None,
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2021-08-24")),
    paymentLotItem = None, paymentLot = None, chargeReference = Some("chargeRef")
  )

  val balancingChargeCancelled = ChargeItem(taxYear = TaxYear.forYearEnd(2021),
    transactionId = id1040000125,
    transactionType = BalancingCharge,
    codedOutStatus = Some(Cancelled),
    originalAmount = 43.21,
    outstandingAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    dueDate = Some(LocalDate.parse("2021-08-25")),
    lpiWithDunningLock = None,
    amountCodedOut = None,
    dunningLock = false,
    poaRelevantAmount = None,
    dueDateForFinancialDetail = Some(LocalDate.parse("2021-08-25")),
    paymentLotItem = None, paymentLot = None,
    chargeReference = Some("chargeRef")
  )

  val balancingChargePaye = balancingChargeNics2.copy(
    transactionId = id1040000126,
    codedOutStatus = Some(Accepted),
    amountCodedOut = Some(2500.00))

  val balancingCodedOut: CodingOutDetails = CodingOutDetails(2500.00, TaxYear.forYearEnd(2021))

  def financialDetailsOverdueWithLpiDunningLockZeroCi(taxYear: TaxYear,
                                                      accruingInterestAmount: Option[BigDecimal],
                                                      interestOutstandingAmount: Option[BigDecimal],
                                                      dunningLock: Boolean,
                                                      lpiWithDunningLock: Option[BigDecimal]): List[ChargeItem] =

    List(
      ChargeItem(
        id1040000124,
        taxYear,
        transactionType = PoaOneDebit,
        codedOutStatus = None,
        documentDate = LocalDate.of(2018, 3, 29),
        dueDate = Some(fixedDate.minusDays(10)),
        originalAmount = 43.21,
        outstandingAmount = 0.0,
        interestOutstandingAmount = interestOutstandingAmount,
        accruingInterestAmount = accruingInterestAmount,
        latePaymentInterestAmount = accruingInterestAmount,
        interestFromDate = Some(LocalDate.parse("2019-05-25")),
        interestEndDate = Some(LocalDate.parse("2019-06-25")),
        interestRate = Some(2.6),
        lpiWithDunningLock = lpiWithDunningLock,
        amountCodedOut = None,
        dunningLock = dunningLock,
        poaRelevantAmount = None,
        chargeReference = Some("chargeRef1")
      ),
      ChargeItem(
        id1040000125,
        taxYear,
        transactionType = PoaOneDebit,
        codedOutStatus = None,
        documentDate = LocalDate.of(2018, 3, 29),
        dueDate = Some(fixedDate.minusDays(1)),
        originalAmount = 12.34,
        outstandingAmount = 75.0,
        interestOutstandingAmount = interestOutstandingAmount,
        accruingInterestAmount = accruingInterestAmount,
        latePaymentInterestAmount = accruingInterestAmount,
        interestFromDate = Some(LocalDate.parse("2019-05-25")),
        interestEndDate = Some(LocalDate.parse("2019-06-25")),
        interestRate = Some(6.2),
        lpiWithDunningLock = lpiWithDunningLock,
        amountCodedOut = None,
        dunningLock = dunningLock,
        poaRelevantAmount = None,
        chargeReference = Some("chargeRef2")
      )
    )

  def outstandingChargesModelCi(dueDate: LocalDate): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456789012345.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  val staticDateOutstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModelCi(LocalDate.parse("2022-01-01"))

  def whatYouOweWithAZeroOutstandingAmount(firstTransactionType: ChargeType = BalancingCharge)(implicit dateService: DateServiceInterface): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = List(
      ChargeItem(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "transId1",
        transactionType = firstTransactionType,
        codedOutStatus = None,
        outstandingAmount = 1000,
        originalAmount = 3400,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        accruingInterestAmount = None,
        latePaymentInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2018-02-14")),
        dunningLock = false,
        poaRelevantAmount = None,
        chargeReference = Some("chargeRef1")),
      ChargeItem(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "transId2",
        transactionType = PoaOneDebit,
        codedOutStatus = None,
        outstandingAmount = 100,
        originalAmount = 1000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        accruingInterestAmount = None,
        latePaymentInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01").plusDays(1)),
        dunningLock = false,
        poaRelevantAmount = None,
        chargeReference = Some("chargeRef2"))),
    outstandingChargesModel = Some(staticDateOutstandingChargesOverdueData)
  )

  def whatYouOweDataWithPaidPOAs(dunningLocks: List[Option[String]] = noDunningLocks)(implicit dateService: DateService): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 0.00, 0.00, 1.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsDueIn30DaysCi(dunningLocks).map(_.copy(outstandingAmount = 0.0)),
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )

  def whatYouOweDataWithDataDueIn30Days(dunningLocks: List[Option[String]] = noDunningLocks, codedOutDetails: Option[CodingOutDetails] = None)(implicit dateService: DateService): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(50.00, 0.00, 0.00, 50.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsDueIn30DaysCi(dunningLocks),
    outstandingChargesModel = Some(outstandingChargesDueIn30Days),
    codedOutDetails = codedOutDetails
  )

  def whatYouOweDataWithDataDueIn30DaysAvailableCreditZero(dunningLocks: List[Option[String]] = noDunningLocks)(implicit dateService: DateService): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 0.00, 0.00, 1.00, Some(0.00), None, None, None, None, None, None),
    chargesList = financialDetailsDueIn30DaysCi(dunningLocks),
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )

  val financialDetailsBalancingChargeNotOverdue: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(BalancingCharge, BalancingCharge),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )

  val financialDetailsReviewAndReconcile: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("SA POA 1 Reconciliation Debit"), Some("SA POA 2 Reconciliation Debit")),
    mainType = List(Some("SA POA 1 Reconciliation"), Some("SA POA 2 Reconciliation")),
    mainTransaction = List(Some("4911"), Some("4913")),
    dueDate = List(Some(fixedDate.minusDays(1)), Some(fixedDate.plusDays(30))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None)
  )

  val financialDetailsReviewAndReconcileNotYetDue: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("SA POA 1 Reconciliation Debit"), Some("SA POA 2 Reconciliation Debit")),
    mainType = List(Some("SA POA 1 Reconciliation"), Some("SA POA 2 Reconciliation")),
    mainTransaction = List(Some("4911"), Some("4913")),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None)
  )

  val financialDetailsReviewAndReconcileCi: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(50.06, 75.07),
    outstandingInterest = List(Some(BigDecimal(10.01)), Some(BigDecimal(20.20))),
    interestRate = List(Some(BigDecimal(2.75)), Some(BigDecimal(2))),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )

  val financialDetailsLPP2: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(SecondLatePaymentPenalty, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(50.06, 75.07),
    outstandingInterest = List(Some(BigDecimal(10.01)), Some(BigDecimal(20.20))),
    interestRate = List(Some(BigDecimal(2.75)), Some(BigDecimal(2))),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    dunningLock = noDunningLocks)

  val financialDetailsLPP2NoChargeRef: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(SecondLatePaymentPenalty, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(50.06, 75.07),
    outstandingInterest = List(Some(BigDecimal(10.01)), Some(BigDecimal(20.20))),
    interestRate = List(Some(BigDecimal(2.75)), Some(BigDecimal(2))),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    dunningLock = noDunningLocks,
    chargeReference = List(None, None)
  )

  val financialDetailsModelLatePaymentPenalties: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("LPP1"), Some("LPP2")),
    mainType = List(Some("LPP1"), Some("LPP2")),
    mainTransaction = List(Some("4028"), Some("4029")),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    latePaymentInterestAmount = List(None, None),
    interestOutstandingAmount = List(None, None),
    interestEndDate = List(Some(LocalDate.of(2018, 6, 15)), Some(LocalDate.of(2018, 6, 15))),
    lpiWithDunningLock = List(None, None),
    chargeReference = List(Some("chargeRefLPP1"), Some("chargeRefLPP2"))
  )

  val financialDetailsLatePaymentPenaltiesChargeItem: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(FirstLatePaymentPenalty, SecondLatePaymentPenalty),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    latePaymentInterestAmount = List(None, None),
    dunningLock = noDunningLocks,
    interestRate = List(Some(100), Some(100)),
    dueDateForFinancialDetail = List(fixedDate.plusDays(1), fixedDate.plusDays(1)),
    chargeReference = List(Some("chargeRefLPP1"), Some("chargeRefLPP2"))
  )
  val financialDetailsLateSubmissionPenaltyChargeItem: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(LateSubmissionPenalty, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )
  val financialDetailsLatePaymentPenaltiesChargeItemInterest: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(FirstLatePaymentPenalty, SecondLatePaymentPenalty),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(0, 0),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(Some(99.0), Some(98.0)),
    outstandingInterest = List(Some(99.0), Some(98.0)),
    interestEndDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    dunningLock = noDunningLocks,
    interestRate = List(Some(100), Some(100)),
    dueDateForFinancialDetail = List(fixedDate.plusDays(1), fixedDate.plusDays(1))
  )
  val financialDetailsLateSubmissionPenaltyChargeItemInterest: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(LateSubmissionPenalty, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(0, 0),
    taxYear = fixedDate.getYear.toString,
    interestRate = List(Some(2.6), Some(2.6)),
    accruingInterestAmount = List(Some(100.0), None),
    latePaymentInterestAmount = List(Some(100.0), None),
    outstandingInterest = List(Some(100.0), None),
    interestEndDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    dunningLock = noDunningLocks
  )

  val financialDetailsOverdueCharges: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoReconciliationDebit),
    dueDate = List(Some(fixedDate.plusDays(1)), Some(fixedDate.plusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )

  val financialDetailsReviewAndReconcileNotYetDueCi: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoReconciliationDebit),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    dunningLock = noDunningLocks
  )

  val financialDetailsReviewAndReconcileInterest: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoReconciliationDebit),
    dueDate = List(Some(fixedDate.plusYears(100).minusDays(1)), Some(fixedDate.plusYears(100).plusDays(30))),
    outstandingAmount = List(0, 0),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(Some(150), Some(150)),
    latePaymentInterestAmount = List(Some(150), Some(150)),
    dunningLock = noDunningLocks,
    outstandingInterest = List(Some(100), Some(40)),
    interestEndDate = List(Some(LocalDate.of(2100, 1, 1)), Some(LocalDate.of(2100, 1, 1))),
    interestRate = List(Some(10), Some(10))
  )

  def whatYouOweDataWithMixedData1(codedOutDetails: Option[CodingOutDetails] = None): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      transactionId = List(id1040000123, id1040000124),
      taxYear = fixedDate.getYear.toString,
      dueDate = List(Some(fixedDate.plusDays(35)), Some(fixedDate.minusDays(1))),
      outstandingAmount = List(25.0, 50.0),
      outstandingInterest = List(Some(100.0), Some(100.0)),
      latePaymentInterestAmount = List(Some(100.0), Some(100.0)),
      accruingInterestAmount = List(Some(100.0), Some(100.0)),
      lpiWithDunningLock = List(Some(100.0), Some(100.0)),
      interestRate = List(Some(100.0), Some(100.0)),
      interestFromDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      interestEndDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      dunningLock = noDunningLocks, dueDateForFinancialDetail = List(LocalDate.parse("2024-01-19"), fixedDate.minusDays(1))).reverse,
    outstandingChargesModel = Some(OutstandingChargesModel(List())),
    codedOutDetails = codedOutDetails
  )

  val whatYouOweDataWithMixedData2: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      transactionId = List(id1040000123, id1040000124),
      dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
      outstandingAmount = List(50, 75),
      outstandingInterest = List(Some(100.0), Some(100.0)),
      latePaymentInterestAmount = List(Some(100.0), Some(100.0)),
      accruingInterestAmount = List(Some(100.0), Some(100.0)),
      lpiWithDunningLock = List(Some(100.0), Some(100.0)),
      interestRate = List(Some(100.0), Some(100.0)),
      interestFromDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      interestEndDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      dunningLock = noDunningLocks, dueDateForFinancialDetail = List(LocalDate.parse("2024-01-14"), LocalDate.parse("2023-12-14"))).reverse,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val financialDetailsWithMixedData4Ci: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(PoaOneReconciliationDebit, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50, 75),
    outstandingInterest = List(Some(100.0), Some(100.0)),
    accruingInterestAmount = List(None, None),
    lpiWithDunningLock = List(Some(100.0), Some(100.0)),
    interestRate = List(Some(100.0), Some(100.0)),
    interestEndDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
    taxYear = fixedDate.getYear.toString,
    dunningLock = List(None, None),
    dueDateForFinancialDetail = List(LocalDate.parse("2023-12-14"), LocalDate.parse("2023-12-14"))
  )

  val financialDetailsWithMixedData4PenaltiesCi: List[ChargeItem] = testFinancialDetailsChargeItems(
    transactionId = List(id1040000123, id1040000124),
    transactionTypes = List(LateSubmissionPenalty, PoaTwoDebit),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50, 75),
    outstandingInterest = List(Some(100.0), Some(100.0)),
    accruingInterestAmount = List(None, None),
    latePaymentInterestAmount = List(None, None),
    lpiWithDunningLock = List(Some(100.0), Some(100.0)),
    interestRate = List(Some(100.0), Some(100.0)),
    interestEndDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
    taxYear = fixedDate.getYear.toString,
    dunningLock = List(None, None),
    dueDateForFinancialDetail = List(fixedDate.plusDays(30), fixedDate.minusDays(1))
  )

  val whatYouOweDataWithMixedData4Unfiltered: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData4Ci.head,
      financialDetailsWithMixedData4Ci(1)),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )
  val whatYouOweDataWithMixedData4Filtered: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData4Ci(1)),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweDataWithMixedDate4PenaltiesUnfilered: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsWithMixedData4PenaltiesCi.reverse,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweWithReviewReconcileData: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsReviewAndReconcileCi,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweWithReviewReconcileDataNotYetDue: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsReviewAndReconcileNotYetDueCi,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweReconciliationInterestData: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsReviewAndReconcileInterest,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweLatePaymentPenalties: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsLatePaymentPenaltiesChargeItem,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweAllPenalties: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsLateSubmissionPenaltyChargeItem ++ financialDetailsLatePaymentPenaltiesChargeItem,
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweAllPenaltiesInterest: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsLateSubmissionPenaltyChargeItemInterest ++ financialDetailsLatePaymentPenaltiesChargeItemInterest,
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
    accruingInterestAmount = List(None, None),
    dunningLock = List(None, None)
  )

  def whatYouOweDataWithOverdueDataIt(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 3.00, 0.00, 3.00, None, None, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks,
      interestFromDate = List(None, None),
      interestEndDate = List(None, None),
      dueDateForFinancialDetail = List(LocalDate.parse("2023-12-05"), LocalDate.parse("2023-12-14"))
    ),
    outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
  )

  def whatYouOweDataWithOverdueData(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 3.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks,
      interestFromDate = List(None, None),
      interestEndDate = List(None, None),
      dueDateForFinancialDetail = List(LocalDate.parse("2023-12-05"), LocalDate.parse("2023-12-14"))
    ),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )


  def whatYouOweDataWithOverdueDataAndInterest(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 3.00, 0.00, 3.00, None, None, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks,
      outstandingInterest = List(Some(100), Some(100)),
      interestRate = List(Some(1.0), Some(1.0)),
      dueDateForFinancialDetail = List(LocalDate.parse("2024-01-29"), LocalDate.parse("2024-02-03"))
    ),
    outstandingChargesModel = Some(outstandingChargesOverdueData),
    codedOutDetails = Some(balancingCodedOut)
  )


  def whatYouOweDataWithDataDueInMoreThan30Days(dunningLocks: List[Option[String]] = noDunningLocks, dueDates: List[Option[LocalDate]] = dueDateMoreThan30Days, codedOutDetails: Option[CodingOutDetails] = None): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 4.00, 2.00, Some(BigDecimal(100.00)), None, None, Some(BigDecimal(350.00)), None, None, Some(BigDecimal(100.00))),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDates,
      dunningLock = dunningLocks,
      interestEndDate = List(None, None),
      interestFromDate = List(None, None),
      dueDateForFinancialDetail = List(LocalDate.parse("2024-01-29"), LocalDate.parse("2024-02-03"))
    ),
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days),
    codedOutDetails = codedOutDetails
  )


  def whatYouOweDataWithZeroMoneyInAccount(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 0.00, 2.00, None, None, None, None, None, None, Some(BigDecimal(0.00))),
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
    None, None, None, Some(5.0),
    None, Some(2500.0),
    false,
    None,
    chargeReference = Some("chargeRef"))

  val codedOutDetailsA: CodingOutDetails = CodingOutDetails(100.00, TaxYear.forYearEnd(2022))

  val codedOutDetails: CodingOutDetails = CodingOutDetails(43.21, TaxYear.forYearEnd(2021))

  val codedOutDocumentDetailCi: ChargeItem = ChargeItem(
    transactionId = "CODINGOUT02",
    taxYear = TaxYear.forYearEnd(2021),
    transactionType = BalancingCharge,
    codedOutStatus = Some(Nics2),
    outstandingAmount = 12.34,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    amountCodedOut = Some(43.21),
    lpiWithDunningLock = None,
    dunningLock = false,
    dueDate = None,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef"))

  val codedOutDocumentDetailPayeSACi: ChargeItem = ChargeItem(
    transactionId = "CODINGOUT02",
    taxYear = TaxYear.forYearEnd(2021),
    transactionType = BalancingCharge,
    codedOutStatus = Some(Accepted),
    outstandingAmount = 0.00,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = Some(0.0),
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    amountCodedOut = Some(43.21),
    lpiWithDunningLock = None,
    dunningLock = false,
    dueDate = None,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef")
  )

  val codedOutDocumentDetailFullyCollectedCi: ChargeItem = ChargeItem(
    taxYear = TaxYear.forYearEnd(2021),
    transactionId = "CODINGOUT02",
    transactionType = BalancingCharge,
    codedOutStatus = Some(Nics2),
    outstandingAmount = 12.34,
    originalAmount = 43.21,
    documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None,
    interestRate = None,
    interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")),
    accruingInterestAmount = None,
    latePaymentInterestAmount = None,
    amountCodedOut = Some(0),
    dueDate = None,
    lpiWithDunningLock = None,
    dunningLock = false,
    poaRelevantAmount = None,
    chargeReference = Some("chargeRef")
  )

  def testFinancialDetailsModelOneItemInList(documentDescription: Option[String],
                                             mainType: Option[String],
                                             mainTransaction: Option[String],
                                             dueDate: Option[LocalDate],
                                             outstandingAmount: BigDecimal,
                                             taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription, Some("documentText"), outstandingAmount, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100),
          Some("latePaymentInterestId"), None, Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), None, Some(100), Some("paymentLotItem"), Some("paymentLot"),
          effectiveDateOfPayment = dueDate,
          documentDueDate = dueDate)
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType, mainTransaction, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate))))
      )
    )

  def testFinancialDetailsModelWithInterest(documentDescription: List[Option[String]] = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
                                            mainType: List[Option[String]] = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
                                            mainTransaction: List[Option[String]] = List(Some("4920"), Some("4930")),
                                            dueDate: List[Option[LocalDate]],
                                            dunningLock: List[Option[String]],
                                            outstandingAmount: List[BigDecimal] = List(50, 75),
                                            taxYear: String = fixedDate.getYear.toString,
                                            interestOutstandingAmount: List[Option[BigDecimal]] = List(Some(100), Some(100)),
                                            interestRate: List[Option[BigDecimal]] = List(Some(100), Some(100)),
                                            accruingInterestAmount: List[Option[BigDecimal]] = List(None, None)): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), interestOutstandingAmount.head, interestRate.head, Some("latePaymentInterestId1"), None, Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount.head, effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), interestOutstandingAmount(1), interestRate(1), Some("latePaymentInterestId2"), None, Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount(1), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = dueDate.head, dunningLock = dunningLock.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = dueDate(1), dunningLock = dunningLock(1)))))
      )
    )

  val whatYouOwePartialChargesListX: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(balanceDueWithin30Days = 1.00, overDueAmount = 2.00, balanceNotDuein30Days = 4.00, totalBalance = 3.00, None, None, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = List(Some(LocalDate.of(2019, 6, 25)), Some(LocalDate.of(2023, 12, 14))),
      documentDate = List(LocalDate.of(2018, 3, 29), LocalDate.of(2018, 3, 29)),
      interestFromDate = List(Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-05-25"))),
      interestEndDate = List(Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25"))),
      dunningLock = oneDunningLock,
      outstandingInterest = List(Some(42.50), Some(24.05)),
      interestRate = List(Some(2.6), Some(6.2)),
      accruingInterestAmount = List(Some(54.56), Some(98.76)),
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
      accruingInterestAmount = None,
      taxYear = TaxYear.forYearEnd(fixedDate.getYear)
    )) ++ Seq(chargeItemModel(
      transactionId = id1040000124,
      transactionType = PoaOneDebit,
      documentDate = LocalDate.of(2018, 3, 29),
      dueDate = Some(fixedDate.plusDays(45)),
      interestOutstandingAmount = Some(100.0),
      outstandingAmount = 125,
      interestRate = Some(100.0),
      accruingInterestAmount = None,
      interestFromDate = Some(LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 3, 29)),
      taxYear = TaxYear.forYearEnd(fixedDate.getYear)
    )),
    outstandingChargesModel = Some(outstandingChargesOverdueData),
    codedOutDetails = Some(codedOutDetailsA)
  )

  val whatYouOwePartialChargesList: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(balanceDueWithin30Days = 1.00, overDueAmount = 2.00, balanceNotDuein30Days = 4.00, totalBalance = 3.00, None, None, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateOverdue,
      dunningLock = oneDunningLock,
      outstandingInterest = List(Some(42.50), Some(24.05)),
      interestRate = List(Some(2.6), Some(6.2)),
      accruingInterestAmount = List(Some(34.56), Some(34.56)),
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
      taxYear = TaxYear.forYearEnd(fixedDate.getYear),
    )),
    codedOutDetails = Some(codedOutDetailsA)
  )

  val whatYouOweDataWithMFADebitsData: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = testFinancialDetailsChargeItems(
      transactionId = List(id1040000123, id1040000124),
      dueDate = List(Some(fixedDate.minusDays(1)), Some(fixedDate.plusDays(35))),
      dunningLock = noDunningLocks,
      transactionTypes = List(MfaDebitCharge, MfaDebitCharge),
      outstandingAmount = List(100, 50),
      taxYear = fixedDate.getYear.toString,
      interestRate = List(Some(100.0), Some(100.0)),
      lpiWithDunningLock = List(Some(100.0), Some(100.0)),
      outstandingInterest = List(None, None),
      interestEndDate = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
      accruingInterestAmount = List(None, None),
      latePaymentInterestAmount = List(None, None),
      dueDateForFinancialDetail = List(LocalDate.parse("2023-12-14"), LocalDate.parse("2024-01-19"))
    ),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  def whatYouOweDataWithAvailableCredits(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 0.00, 2.00, Some(300.00), Some(150.00), None, Some(350.00), None, None, Some(BigDecimal(100.00))),
    chargesList = testFinancialDetailsChargeItems(
      dueDate = dueDateMoreThan30Days,
      dunningLock = dunningLocks
    ),
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )

  /// integration tests

  def whatYouOweDataWithDataDueIn30DaysIntegration: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = List(
      chargeItemModel(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000123",
        transactionType = BalancingCharge,
        codedOutStatus = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = Some(80),
        interestRate = None,
        interestFromDate = Some(LocalDate.parse("2018-03-29")),
        interestEndDate = Some(LocalDate.parse("2018-03-29")),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = None,
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
        accruingInterestAmount = None,
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
        accruingInterestAmount = None,
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
        codedOutStatus = Some(Nics2),
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = Some(80),
        interestRate = None,
        interestFromDate = Some(LocalDate.parse("2018-03-29")),
        interestEndDate = Some(LocalDate.parse("2018-03-29")),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01"))),
      chargeItemModel(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000124",
        transactionType = PoaOneDebit,
        codedOutStatus = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        accruingInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01"))),
      chargeItemModel(
        taxYear = TaxYear.forYearEnd(2021),
        transactionId = "1040000125",
        transactionType = PoaTwoDebit,
        codedOutStatus = None,
        outstandingAmount = 2000,
        originalAmount = 2000,
        documentDate = LocalDate.parse("2018-03-29"),
        interestOutstandingAmount = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        accruingInterestAmount = None,
        lpiWithDunningLock = None,
        amountCodedOut = None,
        dueDate = Some(LocalDate.parse("2022-01-01")))
    )


    WhatYouOweChargesList(
      balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
      chargesList = inScopeChargeList,
      outstandingChargesModel = Some(outstandingChargesOverdueDataIt)
    )
  }


  def whatYouOweDataFullDataWithoutOutstandingCharges()(implicit dateService: DateServiceInterface): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsOverdueDataCi()
  )

  def financialDetailsOverdueDataCi() = testFinancialDetailsChargeItems(
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
