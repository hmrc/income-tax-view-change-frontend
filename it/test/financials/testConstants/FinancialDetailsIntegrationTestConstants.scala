/*
 * Copyright 2021 HM Revenue & Customs
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

package financials.testConstants

import common.helpers.ComponentSpecBase
import common.models.core.AccountingPeriodModel
import common.models.incomeSourceDetails.{IncomeSourceDetailsModel, PropertyDetailsModel}
import common.services.DateServiceInterface
import common.testConstants.BaseIntegrationTestConstants.*
import financials.enums.ChargeType.{ITSA_NI, NIC4_SCOTLAND, NIC4_WALES}
import financials.enums.CodingOutType.{CODING_OUT_ACCEPTED, CODING_OUT_CANCELLED, CODING_OUT_CLASS2_NICS}
import financials.models.*
import financials.models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import financials.testConstants.PaymentHistoryTestConstraints.oldBusiness1
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FinancialDetailsIntegrationTestConstants  extends ComponentSpecBase {

  val currentDate: LocalDate = LocalDate.of(2023, 4, 5)
  private val poa1Description: String = "ITSA- POA 1"
  private val poa2Description: String = "ITSA- POA 2"
  val id1040000123 = "1040000123"


  val noDunningLock: List[String] = List("dunningLock", "dunningLock")
  val oneDunningLock: List[String] = List("Stand over order", "dunningLock")
  val twoDunningLocks: List[String] = List("Stand over order", "Stand over order")
  val noInterestLock: List[String] = List("Interest lock", "Interest lock")
  val twoInterestLocks: List[String] = List("Breathing Space Moratorium Act", "Manual RPI Signal")
  
  def propertyAccountingStartLocalDateOfCurrentYear(year: Int) = LocalDate.of(year, 1, 1)

  def propertyAccounringEndLocalDateOfCurrentYear(year: Int) = LocalDate.of(year, 12, 31)

  def propertyWithCurrentYear(endYear: Int): PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDateOfCurrentYear(endYear),
      end = propertyAccounringEndLocalDateOfCurrentYear(endYear)
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDateOfCurrentYear(endYear)),
    propertyIncomeType,
    propertyTradingStartDate,
    None,
    None,
  )
  
  def propertyOnlyResponseWithMigrationData(year: Int,
                                            yearOfMigration: Option[String]): IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(),
    properties = List(propertyWithCurrentYear(year)),
    yearOfMigration = yearOfMigration
  )
  
  def documentDetailModel(taxYear: Int = 2018,
                          documentDescription: Option[String] = Some("ITSA- POA 1"),
                          outstandingAmount: BigDecimal = 1400.00,
                          originalAmount: BigDecimal = 1400.00): DocumentDetail =
    DocumentDetail(
      taxYear = taxYear,
      transactionId = "1040000123",
      documentDescription,
      documentText = Some("documentText"),
      outstandingAmount = outstandingAmount,
      originalAmount = originalAmount,
      documentDate = LocalDate.of(2018, 3, 29),
      interestOutstandingAmount = Some(100),
      interestRate = Some(100),
      interestFromDate = Some(LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 3, 29)),
      accruingInterestAmount = Some(100),
      paymentLotItem = Some("paymentLotItem"),
      paymentLot = Some("paymentLot")
    )

  def financialDetail(taxYear: Int = 2018, mainType: Option[String] = Some("ITSA- POA 1"), mainTransaction: Option[String] = None): FinancialDetail = FinancialDetail(
    taxYear = taxYear.toString,
    mainType = mainType,
    mainTransaction = mainTransaction,
    transactionId = Some("transactionId"),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    outstandingAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    items =
      Some(Seq(
        SubItem(
          dueDate = Some(LocalDate.of(2019, 5, 15)),
          subItemId = Some("1"),
          amount = Some(100),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentId")
        )
      ))
  )


  def documentDetailWithDueDateModel(taxYear: Int = 2018,
                                     documentDescription: Option[String] = Some("ITSA- POA 1"),
                                     outstandingAmount: BigDecimal = 1400.00,
                                     originalAmount: BigDecimal = 1400.00,
                                     dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)))
                                    (implicit dateService: DateServiceInterface): DocumentDetailWithDueDate =
    DocumentDetailWithDueDate(documentDetailModel(taxYear, documentDescription, outstandingAmount, originalAmount), dueDate)

  val fullDocumentDetailModel: DocumentDetail = documentDetailModel()
  val fullFinancialDetailModel: FinancialDetail = financialDetail()

  def fullDocumentDetailWithDueDateModel(implicit dateService: DateServiceInterface): DocumentDetailWithDueDate = DocumentDetailWithDueDate(fullDocumentDetailModel, Some(LocalDate.of(2019, 5, 15)))

  def financialDetailsModel(taxYear: Int = 2018, outstandingAmount: BigDecimal = 1400.0): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(documentDetailModel(taxYear, outstandingAmount = outstandingAmount)),
      financialDetails = List(financialDetail(taxYear))
    )

  def documentDetailWithDueDateFinancialDetailListModel(taxYear: Int = 2018,
                                                        outstandingAmount: BigDecimal = -1400.0,
                                                        dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                                                        originalAmount: BigDecimal = 1400.00,
                                                        mainType: Option[String] = Some("SA Payment on Account 1"),
                                                        mainTransaction: Option[String] = Some("4920")
                                                       ):
  (DocumentDetailWithDueDate, FinancialDetail) = {
    (documentDetailWithDueDateModel(
      taxYear = taxYear,
      outstandingAmount = outstandingAmount,
      dueDate = dueDate,
      originalAmount = originalAmount),
      financialDetail(mainType = mainType, mainTransaction = mainTransaction))
  }

  def financialDetailModelPartial(taxYear: Int = 2018,
                                  mainType: String = "SA Payment on Account 1",
                                  chargeType: String = NIC4_WALES,
                                  originalAmount: BigDecimal = 100,
                                  dunningLock: Option[String] = None,
                                  interestLock: Option[String] = None,
                                  accruedInterest: Option[BigDecimal] = None,
                                  additionalSubItems: Seq[SubItem] = Seq()): FinancialDetail = FinancialDetail.apply(
    taxYear = taxYear.toString,
    mainType = Some(mainType),
    transactionId = Some(id1040000123),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(originalAmount),
    outstandingAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(chargeType),
    accruedInterest = accruedInterest,
    items =
      Some(Seq(
        SubItem(
          dueDate = Some(LocalDate.of(2019, 5, 15)),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = dunningLock,
          interestLock = interestLock,
          clearingDate = Some(LocalDate.parse("2019-07-23")),
          clearingReason = Some("clearingReason")
        )
      ) ++ additionalSubItems)
  )

  def testFinancialDetailsModel(documentDescription: List[Option[String]],
                                mainType: List[Option[String]],
                                mainTransaction: List[Option[String]],
                                transactionIds: List[Option[String]],
                                transactionDate: Option[LocalDate],
                                `type`: Option[String],
                                totalAmount: Option[BigDecimal],
                                originalAmount: Option[BigDecimal],
                                clearedAmount: Option[BigDecimal],
                                chargeType: Option[String],
                                dueDate: List[Option[LocalDate]],
                                dunningLock: List[String] = noDunningLock,
                                interestLock: List[String] = noInterestLock,
                                subItemId: Option[String],
                                amount: Option[BigDecimal],
                                clearingDate: Option[LocalDate],
                                clearingReason: Option[String],
                                outgoingPaymentMethod: Option[String],
                                paymentReference: Option[String],
                                paymentAmount: Option[BigDecimal],
                                paymentMethod: Option[String],
                                paymentLot: Option[String],
                                paymentLotItem: Option[String],
                                paymentId: Option[String],
                                outstandingAmount: List[BigDecimal],
                                taxYear: String,
                                accruingInterestAmount: List[Option[BigDecimal]] = List(Some(100), Some(100))
                               ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, transactionIds(0).get, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId1"),
          None, Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), accruingInterestAmount(0), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate.head),
        DocumentDetail(taxYear.toInt, transactionIds(1).get, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId2"),
          None, Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), accruingInterestAmount(1), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, transactionIds(0), Some(LocalDate.parse("2020-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head, dunningLock = Some(dunningLock.head), interestLock = Some(interestLock.head))))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), transactionIds(1), Some(LocalDate.parse("2020-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1), dunningLock = Some(dunningLock(1)), interestLock = Some(interestLock(1))))))
      )
    )


  def testFinancialDetailsModelWithChargesOfSameType(documentDescription: List[Option[String]],
                                                     mainType: List[Option[String]],
                                                     mainTransaction: List[Option[String]],
                                                     transactionIds: List[Option[String]],
                                                     transactionDate: Option[LocalDate],
                                                     `type`: Option[String],
                                                     totalAmount: Option[BigDecimal],
                                                     originalAmount: Option[BigDecimal],
                                                     clearedAmount: Option[BigDecimal],
                                                     chargeType: Option[String],
                                                     dueDate: List[Option[LocalDate]],
                                                     subItemId: Option[String],
                                                     amount: Option[BigDecimal],
                                                     clearingDate: Option[LocalDate],
                                                     clearingReason: Option[String],
                                                     outgoingPaymentMethod: Option[String],
                                                     paymentReference: Option[String],
                                                     paymentAmount: Option[BigDecimal],
                                                     paymentMethod: Option[String],
                                                     paymentLot: Option[String],
                                                     paymentLotItem: Option[String],
                                                     paymentId: Option[String],
                                                     outstandingAmount: List[BigDecimal],
                                                     taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, transactionIds(0).get, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId1"),
          None, Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate.head),
        DocumentDetail(taxYear.toInt, transactionIds(1).get, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId2"),
          None, Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, transactionIds(0), Some(LocalDate.parse("2020-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), transactionIds(1), Some(LocalDate.parse("2020-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def outstandingChargesModel(dueDate: LocalDate): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456789012345.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  def outstandingChargesEmptyBCDModel(dueDate: LocalDate): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("LATE", Some(dueDate), 123456789012345.67, 1234)))

  val outstandingChargesEmptyBCDModel: OutstandingChargesModel = outstandingChargesEmptyBCDModel(currentDate.plusDays(30))

  val outstandingChargesDueInMoreThan30Days: OutstandingChargesModel = outstandingChargesModel(currentDate.plusDays(35))

  val outstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(currentDate.minusYears(1).minusMonths(1))

  val outstandingChargesDueIn30Days: OutstandingChargesModel = outstandingChargesModel(currentDate.plusDays(30))

  val financialDetailsDueInMoreThan30Days: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.plusDays(45)), Some(currentDate.plusDays(50))),
    noDunningLock,
    noInterestLock,
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(50, 75),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsDueIn30Days: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate), Some(currentDate)),
    noDunningLock,
    noInterestLock,
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(2000, 2000),
    taxYear = currentDate.getYear.toString,
    accruingInterestAmount = List(None, None)
  )

  def financialDetailsOverdueData(dunningLock: List[String] = noDunningLock, interestLock: List[String] = noInterestLock): FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.minusDays(15)), Some(currentDate.minusDays(15))),
    dunningLock,
    interestLock,
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(2000, 2000),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsWithMixedData1: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.plusDays(35)), Some(currentDate.minusDays(1))),
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(50, 75),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsWithMixedData2: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.plusDays(30)), Some(currentDate.minusDays(1))),
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(25, 50),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsDueIn30DaysWithAZeroOutstandingAmount: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainType = List(Some("ITSA- POA 1"), Some("ITSA- POA 2")),
    mainTransaction = List(None, None),
    transactionIds = List(Some("transId1"), Some("transId2")),
    transactionDate = Some(LocalDate.parse("2020-08-16")),
    `type` = Some("type"),
    totalAmount = Some(100),
    originalAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(NIC4_WALES),
    dueDate = List(Some(currentDate.plusDays(1)), Some(currentDate)),
    noDunningLock,
    noInterestLock,
    subItemId = Some("1"),
    amount = Some(100),
    clearingDate = Some(LocalDate.parse("2020-08-16")),
    clearingReason = Some("clearingReason"),
    outgoingPaymentMethod = Some("outgoingPaymentMethod"),
    paymentReference = Some("paymentReference"),
    paymentAmount = Some(100),
    paymentMethod = Some("paymentMethod"),
    paymentLot = Some("paymentLot"),
    paymentLotItem = Some("paymentLotItem"),
    paymentId = Some("paymentId"),
    outstandingAmount = List(100, 0),
    taxYear = currentDate.getYear.toString
  )

  val financialDetailsWithMFADebits: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(
        taxYear = currentDate.getYear,
        transactionId = "testMFA1",
        documentDescription = Some("ITSA PAYE Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 1234.00,
        outstandingAmount = 0,
        interestOutstandingAmount = None,
        interestEndDate = None,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23))
      ),
      DocumentDetail(
        taxYear = currentDate.getYear,
        transactionId = "testMFA2",
        documentDescription = Some("ITSA Calc Error Correction"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 2234.00,
        outstandingAmount = 0,
        interestOutstandingAmount = None,
        interestEndDate = None,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 22))
      )),
    financialDetails = List(
      FinancialDetail(
        taxYear = currentDate.getYear.toString,
        transactionId = Some("testMFA1"),
        mainType = Some("ITSA PAYE Charge"),
        mainTransaction = Some("4000"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)), amount = Some(12), transactionId = Some("testMFA1"))))
      ),
      FinancialDetail(
        taxYear = currentDate.getYear.toString,
        transactionId = Some("testMFA2"),
        mainType = Some("ITSA Calc Error Correction"),
        mainTransaction = Some("4001"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 22)), amount = Some(12), transactionId = Some("testMFA2"))))
      )
    )
  )

  val staticDateOutstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(LocalDate.parse("2022-01-01"))


  val whatYouOweOutstandingChargesOnly: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    outstandingChargesModel = Some(outstandingChargesOverdueData))

  val whatYouOweEmptyMFA: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.0, 2.0, 4.0, 3.0, None, None, None, None, None, None, None), List(), Some(OutstandingChargesModel(List())), None)

  val whatYouOweNoChargeList: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(0.00, 0.00, 0.00, 0.00, None, None, None, None, None, None, None), List.empty)

  val whatYouOweFinancialDetailsEmptyBCDCharge: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(1.00, 2.00, 4.00, 3.00, None, None, None, None, None, None, None),
    outstandingChargesModel = Some(outstandingChargesEmptyBCDModel))

  val testInvalidFinancialDetailsJson: JsValue = Json.obj(
    "amount" -> "invalidAmount",
    "payMethod" -> "Payment by Card",
    "valDate" -> "2019-05-27"
  )

  val testFinancialDetailsErrorModelParsing: FinancialDetailsErrorModel = FinancialDetailsErrorModel(
    testErrorStatus, "Json Validation Error. Parsing FinancialDetails Data Response")

  val testFinancialDetailsErrorModel: FinancialDetailsErrorModel = FinancialDetailsErrorModel(testErrorStatus, testErrorMessage)

  val testFinancialDetailsNotFoundErrorModel: FinancialDetailsErrorModel = FinancialDetailsErrorModel(testErrorNotFoundStatus, testErrorMessage)
  
  val propertyTradingStartDate = Some(LocalDate.parse((startYear - 1).toString + "-01-01"))

  val oldProperty: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1)),
    propertyIncomeType,
    propertyTradingStartDate,
    None,
    None,
  )

  val paymentHistoryBusinessAndPropertyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    businesses = List(oldBusiness1),
    properties = List(oldProperty)
  )

  def testValidFinancialDetailsModelMFADebitsJson(originalAmount: BigDecimal,
                                                  outstandingAmount: BigDecimal,
                                                  taxYear: String = "2018",
                                                  dueDate: String = "2018-02-14"
                                                 ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000126",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA PAYE Charge",
        "mainTransaction" -> "4000",
        "transactionId" -> "1040000123",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Calc Error Correction",
        "mainTransaction" -> "4001",
        "transactionId" -> "1040000124",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001")
        )
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Manual Penalty Pre CY-4",
        "mainTransaction" -> "4002",
        "transactionId" -> "1040000125",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001")
        )
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Misc Charge",
        "mainTransaction" -> "4003",
        "transactionId" -> "1040000126",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001")
        )
      )
    )
  )

  def testValidFinancialDetailsModelCreditAndRefundsJsonV2(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                                           dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                                           interestLocks: List[String] = noInterestLock,
                                                           accruingInterestAmount: Option[BigDecimal] = Some(100)
                                                          ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00,
      "totalCreditAvailableForRepayment" -> 5.00,
      "totalCredit" -> 5.00,
      "firstPendingAmountRequested" -> 3.00,
      "secondPendingAmountRequested" -> 2.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000126",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000127",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000123",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000124",
        "chargeReference" -> "ABCD1235",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000125",
        "chargeReference" -> "ABCD1236",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge Credit",
        "mainTransaction" -> "4905",
        "transactionId" -> "1040000126",
        "chargeReference" -> "ABCD1237",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge Credit",
        "mainTransaction" -> "4905",
        "transactionId" -> "1040000127",
        "chargeReference" -> "ABCD1238",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      )
    )
  )

  def testValidFinancialDetailsModelCreditAndRefundsJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                                         dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                                         interestLocks: List[String] = noInterestLock,
                                                         accruingInterestAmount: Option[BigDecimal] = Some(100)
                                                        ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00,
      "totalCreditAvailableForRepayment" -> 5.00,
      "totalCredit" -> 5.00,
      "firstPendingAmountRequested" -> 3.00,
      "secondPendingAmountRequested" -> 2.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      documentDetailsPayment("PAYID01", -500.0, LocalDate.of(taxYear.toInt, 3, 29)),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000126",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000127",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000129",
        "documentDescription" -> "SA Repayment Supplement Credit",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      financialDetailsPayment("PAYID01", -500.0, LocalDate.of(taxYear.toInt, 3, 29), "123456"),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000123",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000124",
        "chargeReference" -> "ABCD1235",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Cutover Credits",
        "mainTransaction" -> "6110",
        "transactionId" -> "1040000125",
        "chargeReference" -> "ABCD1236",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "ITSA Overpayment Relief",
        "mainTransaction" -> "4004",
        "transactionId" -> "1040000126",
        "chargeReference" -> "ABCD1237",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge Credit",
        "mainTransaction" -> "4905",
        "transactionId" -> "1040000127",
        "chargeReference" -> "ABCD1238",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Repayment Supplement Credit",
        "mainTransaction" -> "6020",
        "transactionId" -> "1040000129",
        "chargeReference" -> "ABCD1239",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 2000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
    )
  )
  
  def testChargeHistoryJson(mtdBsa: String, documentId: String, amount: BigDecimal): JsValue = Json.obj(
    "idType" -> "MTDBSA",
    "idValue" -> mtdBsa,
    "regimeType" -> "ITSA",
    "chargeHistoryDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "documentId" -> documentId,
        "documentDate" -> "2018-02-14",
        "documentDescription" -> poa1Description,
        "totalAmount" -> amount,
        "reversalDate" -> "2019-02-14T09:30:45Z",
        "reversalReason" -> "Customer Request",
        "poaAdjustmentReason" -> "002"
      )
    )
  )

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private def documentDetailsPayment(transactionId: String,
                                     amount: BigDecimal,
                                     clearingDate: LocalDate): JsObject = {
    Json.obj(
      "taxYear" -> testTaxYear,
      "transactionId" -> transactionId,
      "documentDescription" -> "Payment on Account",
      "outstandingAmount" -> amount,
      "originalAmount" -> amount,
      "documentDueDate" -> dateFormatter.format(clearingDate),
      "effectiveDateOfPayment" -> dateFormatter.format(clearingDate),
      "documentDate" -> dateFormatter.format(clearingDate),
      "paymentLot" -> "081203010025",
      "paymentLotItem" -> "000001",
    )
  }

  private def financialDetailsPayment(transactionId: String,
                                      amount: BigDecimal,
                                      clearingDate: LocalDate,
                                      clearingSapDocument: String): JsObject = {
    Json.obj(
      "taxYear" -> s"$testTaxYear",
      "mainType" -> "Payment on Account",
      "mainTransaction" -> "0060",
      "transactionId" -> transactionId,
      "chargeReference" -> "ABCD1234",
      "originalAmount" -> amount,
      "items" -> Json.arr(
        Json.obj(
          "subItemId" -> "001",
          "amount" -> amount,
          "clearingDate" -> dateFormatter.format(clearingDate),
          "dueDate" -> dateFormatter.format(clearingDate)),
        "paymentAmount" -> amount,
        "paymentReference" -> "GF235687",
        "paymentMethod" -> "Payment",
        "clearingSAPDocument" -> clearingSapDocument
      )
    )
  }

  lazy val documentText = (isClass2Nic: Boolean, otherwise: String) => {
    if (isClass2Nic) {
      CODING_OUT_CLASS2_NICS.name
    } else {
      otherwise
    }
  }

  def testValidFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                         dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                         interestLocks: List[String] = noInterestLock,
                                         accruingInterestAmount: Option[BigDecimal] = Some(100),
                                         isClass2Nic: Boolean = false, poaRelevantAmount: Option[BigDecimal] = None
                                        ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> documentText(isClass2Nic, "TRM New Charge"),
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "interestRate" -> "3",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate,
        "poaRelevantAmount" -> poaRelevantAmount
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate,
        "poaRelevantAmount" -> poaRelevantAmount
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate,
        "poaRelevantAmount" -> poaRelevantAmount
      ),
      Json.obj(
        "taxYear" -> 9999,
        "transactionId" -> "PAYID01",
        "documentDescription" -> "TRM Amend Charge",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "documentDate" -> "2018-03-29",
        "paymentLot" -> "081203010024",
        "paymentLotItem" -> "000001",
        "latePaymentInterestId" -> "latePaymentInterestId",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate,
        "poaRelevantAmount" -> poaRelevantAmount
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000123",
        "chargeType" -> ITSA_NI,
        "chargeReference" -> "ABCD1234",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "mainTransaction" -> "4920",
        "transactionId" -> "1040000124",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "mainTransaction" -> "4930",
        "transactionId" -> "1040000125",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "PAYID01",
        "chargeType" -> ITSA_NI,
        "chargeReference" -> "ABCD1234",
        "originalAmount" -> originalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate))
      )
    )
  )

  def testValidFinancialDetailsModelWithPaymentAllocationJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                                              dueDate: String = "2018-02-14", dunningLock: List[String] = noDunningLock,
                                                              interestLocks: List[String] = noInterestLock,
                                                              accruingInterestAmount: Option[BigDecimal] = Some(100),
                                                              isClass2Nic: Boolean = false
                                                             ): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> documentText(isClass2Nic, "TRM New Charge"),
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> 9999,
        "transactionId" -> "PAYID01",
        "documentDescription" -> "TRM Amend Charge",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "documentDate" -> "2018-03-29",
        "paymentLot" -> "081203010024",
        "paymentLotItem" -> "000001",
        "latePaymentInterestId" -> "latePaymentInterestId",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "9999",
        "mainType" -> "Payment On Account",
        "transactionId" -> "PAYID01",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> -10000,
            "subItemId" -> "001",
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912",
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "mainTransaction" -> "4910",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingSAPDocument" -> "012345678912",
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "transactionId" -> "1040000124",
        "mainTransaction" -> "4920",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "transactionId" -> "1040000125",
        "mainTransaction" -> "4930",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      )
    )
  )

  def testValidFinancialDetailsModelJsonAccruingInterest(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                         taxYear: String = "2018", dueDate: String = "2018-04-14",
                                                         accruingInterestAmount: Option[BigDecimal] = Some(0),
                                                         latePaymentInterestAmount: Option[BigDecimal] = Some(0)): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "balanceNotDuein30Days" -> 4.00, "totalBalance" -> 3.00),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-04-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "latePaymentInterestAmount" -> latePaymentInterestAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "latePaymentInterestAmount" -> latePaymentInterestAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "latePaymentInterestAmount" -> latePaymentInterestAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000123",
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "mainTransaction" -> "4920",
        "transactionId" -> "1040000124",
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "mainTransaction" -> "4930",
        "transactionId" -> "1040000125",
        "chargeReference" -> "chargeRef",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      )
    )
  )

  def testFinancialDetailsModelWithMissingOriginalAmountJson(): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2018".toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> documentText(false, "TRM New Charge"),
        "outstandingAmount" -> 1.2,
        "originalAmount-added-this-text-to-exclude-it" -> 10.34,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> Some(100),
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> "2018-02-14",
        "documentDueDate" -> "2018-02-14"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2018",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000123",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> 10.34,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> "2018-02-14",
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      )
    )
  )

  def testValidFinancialDetailsModelJsonCodingOut(originalAmount: BigDecimal, outstandingAmount: BigDecimal,
                                                  taxYear: String = "2018", dueDate: String = "2018-04-14",
                                                  accruingInterestAmount: BigDecimal = 0,
                                                  payeSaTaxYear: String = "2018", totalLiabilityAmount: BigDecimal = 0): JsValue = Json.obj(
    "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "balanceNotDuein30Days" -> 4.00, "totalBalance" -> 3.00),
    "codingDetails" -> Json.arr(Json.obj(
      "totalLiabilityAmount" -> totalLiabilityAmount,
      "taxYearReturn" -> taxYear
    )),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> CODING_OUT_CLASS2_NICS,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> payeSaTaxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> CODING_OUT_ACCEPTED,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "amountCodedOut" -> totalLiabilityAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> "TRM Amend Charge",
        "documentText" -> CODING_OUT_CANCELLED,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000126",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> "42.50",
        "interestRate" -> "3",
        "interestFromDate" -> "2018-02-14",
        "interestEndDate" -> "2019-01-01",
        "accruingInterestAmount" -> accruingInterestAmount,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000123",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000124",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001",
            "dunningLock" -> "Coded Out"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> "1040000125",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001",
            "dunningLock" -> "Coded Out"))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "mainTransaction" -> "4930",
        "transactionId" -> "1040000126",
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"))
      )
    )
  )

  def documentDetailJson(originalAmount: BigDecimal,
                         outstandingAmount: BigDecimal,
                         taxYear: Int = 2018,
                         documentDescription: String = "TRM New Charge",
                         transactionId: String = "1040000123",
                         dueDate: String = "2018-02-14"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "transactionId" -> transactionId,
    "documentDescription" -> documentDescription,
    "outstandingAmount" -> outstandingAmount,
    "originalAmount" -> originalAmount,
    "documentDate" -> "2018-03-29",
    "effectiveDateOfPayment" -> dueDate,
    "documentDueDate" -> dueDate
  )

  def financialDetailJson(taxYear: String = "2018",
                          mainType: String = "SA Balancing Charge",
                          mainTransaction: String = "4910",
                          dueDate: String = "2018-02-14",
                          transactionId: String = "1040000123"): JsValue = Json.obj(
    "taxYear" -> taxYear,
    "mainType" -> mainType,
    "mainTransaction" -> mainTransaction,
    "transactionId" -> transactionId,
    "items" -> Json.arr(
      Json.obj("dueDate" -> dueDate)
    )
  )

  def testAuditFinancialDetailsModelJson(originalAmount: BigDecimal, outstandingAmount: BigDecimal, taxYear: String = "2018",
                                         dueDate: String = "2018-04-14", dunningLock: List[String] = noDunningLock,
                                         interestLocks: List[String] = noInterestLock, totalAmount: BigDecimal = 100,
                                         accruingInterestAmount: Option[BigDecimal] = Some(100.0)): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-04-14",
        "interestEndDate" -> "2019-01-01",
        "interestRate" -> "3",
        "accruingInterestAmount" -> accruingInterestAmount,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000124",
        "documentDescription" -> poa1Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> taxYear.toInt,
        "transactionId" -> "1040000125",
        "documentDescription" -> poa2Description,
        "outstandingAmount" -> outstandingAmount,
        "originalAmount" -> originalAmount,
        "documentDate" -> "2018-03-29",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      ),
      Json.obj(
        "taxYear" -> 9999,
        "transactionId" -> "PAYID01",
        "documentDescription" -> "Payment On Account",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "documentDate" -> "2018-03-29",
        "paymentLot" -> "081203010024",
        "paymentLotItem" -> "000001",
        "effectiveDateOfPayment" -> dueDate,
        "documentDueDate" -> dueDate
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "9999",
        "mainType" -> "Payment On Account",
        "mainTransaction" -> "0060",
        "chargeReference" -> "ABCD1234",
        "transactionId" -> "PAYID01",
        "outstandingAmount" -> -outstandingAmount,
        "originalAmount" -> -originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> -10000,
            "subItemId" -> "001",
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912",
            "paymentLot" -> "081203010024",
            "paymentLotItem" -> "000001"
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> "1040000123",
        "mainTransaction" -> "4910",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> ITSA_NI,
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912"
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 1",
        "mainTransaction" -> "4920",
        "transactionId" -> "1040000123",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> NIC4_SCOTLAND,
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 9000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912"
          ),
          Json.obj(
            "interestLock" -> interestLocks.head,
            "dunningLock" -> dunningLock.head
          ))
      ),
      Json.obj(
        "taxYear" -> taxYear,
        "mainType" -> "SA Payment on Account 2",
        "mainTransaction" -> "4930",
        "transactionId" -> "1040000123",
        "chargeReference" -> "ABCD1234",
        "chargeType" -> NIC4_SCOTLAND,
        "originalAmount" -> originalAmount,
        "totalAmount" -> totalAmount,
        "items" -> Json.arr(
          Json.obj("amount" -> 8000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> dueDate,
            "clearingSAPDocument" -> "012345678912"
          ),
          Json.obj(
            "interestLock" -> interestLocks(1),
            "dunningLock" -> dunningLock(1)
          ))
      )
    )
  )

  val testEmptyFinancialDetailsModelJson: JsValue = Json.obj("balanceDetails" -> Json.obj(
    "balanceDueWithin30Days" -> 0.00,
    "overDueAmount" -> 0.00,
    "balanceNotDuein30Days" -> 0.00,
    "totalBalance" -> 0.00
  ), "codingDetails" -> Json.arr(), "documentDetails" -> Json.arr(), "financialDetails" -> Json.arr())

}
