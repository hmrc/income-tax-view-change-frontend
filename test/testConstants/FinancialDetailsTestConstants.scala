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

package testConstants

import enums.ChargeType.NIC4_WALES
import enums.CodingOutType._
import models.creditDetailModel.CreditDetailModel
import models.creditsandrefunds.{CreditsModel, Transaction}
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.libs.json.{JsValue, Json}
import services.DateService
import testConstants.BaseTestConstants.{app, chargeReference, testErrorMessage, testErrorNotFoundStatus, testErrorStatus, testTaxYear}
import testConstants.FinancialDetailsTestConstants.{documentDetailWithDueDateModel, financialDetail}

import java.time.LocalDate


object FinancialDetailsTestConstants {

  val fixedDate: LocalDate = LocalDate.of(2023, 12, 15)

  val fixedDateTwo: LocalDate = LocalDate.of(2018, 12, 15)

  val futureFixedDate: LocalDate = LocalDate.of(2030, 3, 12)

  implicit val dateService: DateService = app.injector.instanceOf[DateService]

  val id1040000123 = "1040000123"
  val id1040000124 = "1040000124"
  val id1040000125 = "1040000125"
  val id1040000126 = "1040000126"

  val testValidFinancialDetailsModelJsonWrites: JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> 2019,
        "transactionId" -> id1040000123,
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "documentText",
        "outstandingAmount" -> 10.33,
        "originalAmount" -> 10.33,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> 100,
        "interestRate" -> 100,
        "latePaymentInterestId" -> "latePaymentInterestId1",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> 100,
        "lpiWithDunningLock" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      ),
      Json.obj(
        "taxYear" -> 2020,
        "transactionId" -> id1040000124,
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "documentText",
        "outstandingAmount" -> 10.34,
        "originalAmount" -> 10.34,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> 100,
        "interestRate" -> 100,
        "latePaymentInterestId" -> "latePaymentInterestId2",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> 100,
        "lpiWithDunningLock" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000123,
        "transactionDate" -> "2020-08-16",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> NIC4_WALES,
        "accruedInterest" -> 100,
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "dunningLock" -> "Stand over order",
            "interestLock" -> "interestLock",
            "clearingDate" -> "2020-08-16",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem",
            "codedOutStatus" -> "I"
          )
        )
      ),
      Json.obj(
        "taxYear" -> "2020",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000124,
        "transactionDate" -> "2020-08-16",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> NIC4_WALES,
        "accruedInterest" -> 100,
        "items" -> Json.arr(
          Json.obj("dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "clearingDate" -> "2020-08-16",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem"
          )
        )
      )
    )
  )

  val testValidFinancialDetailsModelJsonReads: JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> 2019,
        "transactionId" -> id1040000123,
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "documentText",
        "outstandingAmount" -> 10.33,
        "originalAmount" -> 10.33,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> 100,
        "interestRate" -> 100,
        "latePaymentInterestId" -> "latePaymentInterestId1",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> 100,
        "lpiWithDunningLock" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      ),
      Json.obj(
        "taxYear" -> 2020,
        "transactionId" -> id1040000124,
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "documentText",
        "outstandingAmount" -> 10.34,
        "originalAmount" -> 10.34,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> 100,
        "interestRate" -> 100,
        "latePaymentInterestId" -> "latePaymentInterestId2",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> 100,
        "lpiWithDunningLock" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000123,
        "transactionDate" -> "2020-08-16",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> NIC4_WALES,
        "accruedInterest" -> 100,
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "dunningLock" -> "Stand over order",
            "interestLock" -> "interestLock",
            "clearingDate" -> "2020-08-16",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem",
            "codedOutStatus" -> "I"
          )
        )
      ),
      Json.obj(
        "taxYear" -> "2020",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000124,
        "transactionDate" -> "2020-08-16",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> NIC4_WALES,
        "accruedInterest" -> 100,
        "items" -> Json.arr(
          Json.obj("dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "clearingDate" -> "2020-08-16",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem"
          )
        )
      )
    )
  )


  val testValidFinancialDetailsLpiModelJson: JsValue = Json.obj(
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "transactionId" -> id1040000123,
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 10.33,
        "originalAmount" -> 10.33,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> 100,
        "interestRate" -> 100,
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> 100,
        "lpiWithDunningLock" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      ),
      Json.obj(
        "taxYear" -> "2020",
        "transactionId" -> id1040000124,
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 10.34,
        "originalAmount" -> 10.34,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount" -> 100,
        "interestRate" -> 100,
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "accruingInterestAmount" -> 100,
        "lpiWithDunningLock" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000123,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> NIC4_WALES,
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "clearingDate" -> "clearingDate",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem"
          )
        )
      ),
      Json.obj(
        "taxYear" -> "2020",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000124,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> NIC4_WALES,
        "items" -> Json.arr(
          Json.obj("dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "clearingDate" -> "clearingDate",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem"
          )
        )
      )
    )
  )

  val testValidFinancialDetailsNoLpiModelJson: JsValue = Json.obj(
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "transactionId" -> id1040000123,
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 10.33,
        "originalAmount" -> 10.33,
        "documentDate" -> "2018-03-29",
        "interestRate" -> 100,
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      ),
      Json.obj(
        "taxYear" -> "2020",
        "transactionId" -> id1040000124,
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 10.34,
        "originalAmount" -> 10.34,
        "documentDate" -> "2018-03-29",
        "interestRate" -> 100,
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000123,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> NIC4_WALES,
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "clearingDate" -> "clearingDate",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem"
          )
        )
      ),
      Json.obj(
        "taxYear" -> "2020",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> "4910",
        "transactionId" -> id1040000124,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> NIC4_WALES,
        "items" -> Json.arr(
          Json.obj("dueDate" -> LocalDate.parse("2019-05-15"),
            "subItemId" -> "1",
            "amount" -> 100,
            "clearingDate" -> "clearingDate",
            "clearingReason" -> "clearingReason",
            "outgoingPaymentMethod" -> "outgoingPaymentMethod",
            "paymentReference" -> "paymentReference",
            "paymentAmount" -> 100,
            "paymentMethod" -> "paymentMethod",
            "paymentLot" -> "paymentLot",
            "paymentLotItem" -> "paymentLotItem",
            "paymentId" -> "paymentLot-paymentLotItem"
          )
        )
      )
    )
  )


  def documentDetailModel(taxYear: Int = 2018,
                          documentDescription: Option[String] = Some("ITSA- POA 1"),
                          documentText: Option[String] = Some("documentText"),
                          outstandingAmount: BigDecimal = 1400.00,
                          originalAmount: BigDecimal = 1400.00,
                          documentDate: LocalDate = LocalDate.of(2018, 3, 29),
                          documentDueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                          paymentLotItem: Option[String] = Some("paymentLotItem"),
                          paymentLot: Option[String] = Some("paymentLot"),
                          accruingInterestAmount: Option[BigDecimal] = Some(100),
                          interestOutstandingAmount: Option[BigDecimal] = Some(80),
                          transactionId: String = id1040000123,
                          lpiWithDunningLock: Option[BigDecimal] = Some(100),
                          effectiveDateOfPayment: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                          amountCodedOut: Option[BigDecimal] = None): DocumentDetail =
    DocumentDetail(
      taxYear = taxYear,
      transactionId = transactionId,
      documentDescription,
      documentText = documentText,
      outstandingAmount = outstandingAmount,
      originalAmount = originalAmount,
      documentDate = documentDate,
      documentDueDate = documentDueDate,
      interestOutstandingAmount = interestOutstandingAmount,
      interestRate = Some(100),
      interestFromDate = Some(LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 6, 15)),
      accruingInterestAmount = accruingInterestAmount,
      lpiWithDunningLock = lpiWithDunningLock,
      paymentLotItem = paymentLotItem,
      paymentLot = paymentLot,
      effectiveDateOfPayment = effectiveDateOfPayment,
      amountCodedOut = amountCodedOut
    )

  def financialDetail(taxYear: Int = 2018,
                      mainType: String = "SA Payment on Account 1",
                      mainTransaction: String = "4920",
                      chargeType: String = NIC4_WALES,
                      originalAmount: BigDecimal = 100,
                      chargeRef: Option[String] = Some(chargeReference),
                      dunningLock: Option[String] = None,
                      interestLock: Option[String] = None,
                      accruedInterest: Option[BigDecimal] = None,
                      additionalSubItems: Seq[SubItem] = Seq(),
                      dueDateValue: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                      transactionId: Option[String] = Some(id1040000123),
                      codedOutStatus: Option[String] = None
                     ): FinancialDetail = FinancialDetail.apply(
    taxYear = taxYear.toString,
    mainType = Some(mainType),
    mainTransaction = Some(mainTransaction),
    transactionId = transactionId,
    transactionDate = Some(LocalDate.parse("2022-08-16")),
    chargeReference = chargeRef,
    `type` = Some("type"),
    originalAmount = Some(originalAmount),
    outstandingAmount = Some(100),
    clearedAmount = Some(100),
    chargeType = Some(chargeType),
    accruedInterest = accruedInterest,
    items =
      Some(Seq(
        SubItem(
          dueDate = dueDateValue,
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = dunningLock,
          interestLock = interestLock,
          clearingDate = Some(LocalDate.parse("2019-07-23")),
          clearingReason = Some("clearingReason"),
          codedOutStatus = codedOutStatus
        )
      ) ++ additionalSubItems)
  )


  def documentDetailWithDueDateModel(taxYear: Int = 2018,
                                     documentDescription: Option[String] = Some("ITSA- POA 1"),
                                     documentText: Option[String] = Some("documentText"),
                                     outstandingAmount: BigDecimal = 1400.00,
                                     originalAmount: BigDecimal = 1400.00,
                                     transactionId: String = id1040000123,
                                     accruingInterestAmount: Option[BigDecimal] = Some(100),
                                     paymentLot: Option[String] = Some("paymentLot"),
                                     paymentLotItem: Option[String] = None,
                                     dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                                     isMFADebit: Boolean = false): DocumentDetailWithDueDate =
    DocumentDetailWithDueDate(documentDetailModel(taxYear = taxYear, documentDescription = documentDescription, outstandingAmount = outstandingAmount,
      originalAmount = originalAmount, documentText = documentText, transactionId = transactionId, paymentLot = paymentLot, paymentLotItem = paymentLotItem,
      accruingInterestAmount = accruingInterestAmount, documentDueDate = dueDate), dueDate = dueDate, isMFADebit = isMFADebit)

  val balanceDetails: BalanceDetails = BalanceDetails(
    balanceDueWithin30Days = 1.00,
    overDueAmount = 2.00,
    totalBalance = 3.00,
    totalCreditAvailableForRepayment = Some(100.00),
    None,
    None,
    totalCredit = Some(200.00),
    None,
    None,
    None
  )

  val documentDetailPOA1: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("ITSA- POA 1"))
  val documentDetailPOA2: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("ITSA - POA 2"))
  val documentDetailBalancingCharge: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"))
  val documentDetailAmendedBalCharge: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM Amend Charge"))
  val documentDetailClass2Nic: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CLASS2_NICS), paymentLot = None, accruingInterestAmount = None)
  val documentDetailPaye: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED), paymentLot = None, accruingInterestAmount = None)
  val fullDocumentDetailModel: DocumentDetail = documentDetailModel()
  val fullFinancialDetailModel: FinancialDetail = financialDetail()
  val fullDocumentDetailWithDueDateModel: DocumentDetailWithDueDate = DocumentDetailWithDueDate(fullDocumentDetailModel, Some(LocalDate.of(2019, 5, 15)))

  def financialDetails(balanceDetails: BalanceDetails = balanceDetails,
                       documentDetails: DocumentDetail = documentDetailModel(),
                       financialDetails: FinancialDetail = financialDetail()): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(documentDetails),
      financialDetails = List(financialDetails)
    )

  def financialDetailsModel(
                             taxYear: Int = 2018,
                             documentDescription: Option[String] = Some("ITSA- POA 1"),
                             mainTransaction:String = "4920",
                             outstandingAmount: BigDecimal = 1400.0,
                             dunningLock: Option[String] = None,
                             lpiWithDunningLock: Option[BigDecimal] = Some(100),
                             dueDateValue: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                             accruingInterestAmount: Option[BigDecimal] = Some(100),
                             amountCodedOut: Option[BigDecimal] = None
                           ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(documentDetailModel(taxYear, outstandingAmount = outstandingAmount, documentDueDate = dueDateValue, paymentLot = None,
        paymentLotItem = None, lpiWithDunningLock = lpiWithDunningLock, accruingInterestAmount = accruingInterestAmount, amountCodedOut = amountCodedOut)),
      financialDetails = List(financialDetail(taxYear, dunningLock = dunningLock, dueDateValue = dueDateValue, mainTransaction = mainTransaction)
      )
    )

  def financialDetailsModelWithPoaOneAndTwoWithRarCredits() =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(
        documentDetailModel(transactionId = id1040000126, taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1))),
        documentDetailModel(transactionId = id1040000125, taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1))),
        documentDetailModel(transactionId = id1040000124, taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1))),
        documentDetailModel(transactionId = id1040000123, taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1)))
      ),
      financialDetails = List(
        financialDetail(transactionId = Some(id1040000125), taxYear = testTaxYear, mainTransaction = "4920"),
        financialDetail(transactionId = Some(id1040000126), taxYear = testTaxYear, mainTransaction = "4930"),
        financialDetail(transactionId = Some(id1040000124), taxYear = testTaxYear, mainTransaction = "4914"),
        financialDetail(transactionId = Some(id1040000123), taxYear = testTaxYear, mainTransaction = "4912")
      )
    )

  def financialDetailsModelWithPoaOneAndTwo() =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(
        documentDetailModel(transactionId = id1040000126, taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1))),
        documentDetailModel(transactionId = id1040000125, taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1)))
      ),
      financialDetails = List(
        financialDetail(transactionId = Some(id1040000125), taxYear = testTaxYear, mainTransaction = "4920"),
        financialDetail(transactionId = Some(id1040000126), taxYear = testTaxYear, mainTransaction = "4930")
      )
    )

  def financialDetailsModelWithPoaOneAndTwoFullyPaid() =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(
        documentDetailModel(transactionId = id1040000126, taxYear = testTaxYear, outstandingAmount = 0.00, originalAmount = 1500.00, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, interestOutstandingAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1))),
        documentDetailModel(transactionId = id1040000125, taxYear = testTaxYear, outstandingAmount = 0.00, originalAmount = 1500.00, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, interestOutstandingAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1)))
      ),
      financialDetails = List(
        financialDetail(transactionId = Some(id1040000125), taxYear = testTaxYear, mainTransaction = "4920"),
        financialDetail(transactionId = Some(id1040000126), taxYear = testTaxYear, mainTransaction = "4930")
      )
    )

  def financialDetailsModelWithPoaOneNoChargeRef(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(
        documentDetailModel(transactionId = id1040000125, taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = None, documentDueDate = Some(LocalDate.of(2020,1,1)))
      ),
      financialDetails = List(
        financialDetail(transactionId = Some(id1040000125), taxYear = testTaxYear, mainTransaction = "4920", chargeRef = None),
      )
    )

  def financialDetailsModelWithPoaOneWithLpi() =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(
        documentDetailModel(transactionId = "CODINGOUT01", taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = Some(100), documentDueDate = Some(LocalDate.of(2020,1,1))),
      ),
      financialDetails = List(
        financialDetail(transactionId = Some("CODINGOUT01"), taxYear = testTaxYear, mainTransaction = "4920")
      )
    )

  def financialDetailsModelWithPoaTwoWithLpi() =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(
        documentDetailModel(transactionId = "CODINGOUT01", taxYear = testTaxYear, paymentLot = None, paymentLotItem = None, accruingInterestAmount = Some(100), documentDueDate = Some(LocalDate.of(2020,1,1))),
      ),
      financialDetails = List(
        financialDetail(transactionId = Some("CODINGOUT01"), taxYear = testTaxYear, mainTransaction = "4930")
      )
    )


  def financialDetailsModelWithMFADebit() =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(documentDetailModel(testTaxYear, paymentLot = None,
        paymentLotItem = None, accruingInterestAmount = None)),
      financialDetails = List(financialDetail(testTaxYear, mainType = "ITSA Calc Error Correction", mainTransaction = "4001")
      )
    )

  def chargesWithAllocatedPaymentModel(taxYear: Int = 2018,
                                       outstandingAmount: BigDecimal = 1400.0,
                                       lpiWithDunningLock: Option[BigDecimal] = Some(100)): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        // charge
        documentDetailModel(
          taxYear = taxYear,
          outstandingAmount = outstandingAmount,
          paymentLot = None,
          paymentLotItem = None,
          lpiWithDunningLock = lpiWithDunningLock,
          transactionId = id1040000123
        ),
        // payment
        documentDetailModel(taxYear = 9999,
          outstandingAmount = -outstandingAmount,
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          lpiWithDunningLock = lpiWithDunningLock,
          transactionId = id1040000124
        ),
        // credit
        documentDetailModel(
          taxYear = taxYear,
          transactionId = id1040000125,
          outstandingAmount = outstandingAmount,
          paymentLot = None,
          paymentLotItem = None,
        )),
      financialDetails = List(
        // charge
        financialDetail(
          transactionId = Some(id1040000123),
          taxYear = taxYear,
          additionalSubItems = Seq(
            // cleared by payment
            SubItem(
              amount = Some(500.0),
              dueDate = Some(LocalDate.parse("2018-09-07")),
              clearingDate = Some(LocalDate.parse("2018-09-07")),
              clearingSAPDocument = Some("000000000000"),
              paymentAmount = Some(500.0),
              paymentLot = None,
              paymentLotItem = None),
            // cleared by credit
            SubItem(
              amount = Some(500.0),
              dueDate = Some(LocalDate.parse("2018-09-10")),
              clearingDate = Some(LocalDate.parse("2018-09-10")),
              clearingReason = Some("Cleared by Credit?"),
              clearingSAPDocument = Some("000000000001"),
              paymentAmount = Some(500.0),
              paymentLot = None,
              paymentLotItem = None)
          )),
        // payment on account
        financialDetail(
          transactionId = Some(id1040000124),
          taxYear = taxYear,
          mainType = "Payment on Account",
          mainTransaction = "0060",
          additionalSubItems = Seq(
            SubItem(
              amount = Some(500.0),
              clearingDate = Some(LocalDate.parse("2018-09-07")),
              clearingSAPDocument = Some("000000000000"),
              paymentAmount = Some(500.0),
              paymentLot = Some("paymentLot"),
              paymentLotItem = Some("paymentLotItem"))
          )),
        // credit
        financialDetail(
          transactionId = Some(id1040000125),
          taxYear = taxYear,
          mainType = "ITSA Cutover Credits",
          mainTransaction = "6110",
          additionalSubItems = Seq(
            SubItem(
              amount = Some(500.0),
              dueDate = Some(LocalDate.parse("2018-09-10")),
              clearingDate = Some(LocalDate.parse("2018-09-10")),
              clearingReason = Some("Allocated to Charge"),
              clearingSAPDocument = Some("000000000001"),
              paymentAmount = Some(500.0),
              paymentLot = None,
              paymentLotItem = None)
          ),
          chargeType = "Cutover Credits"))
    )

  val testValidFinancialDetailsModel: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(taxYear = 2019,
        transactionId = id1040000123,
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        outstandingAmount = 10.33,
        originalAmount = 10.33,
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId1"),
        interestFromDate = Some(LocalDate.of(2018, 3, 29)),
        interestEndDate = Some(LocalDate.of(2018, 3, 29)),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = Some(100),
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot")),
      DocumentDetail(taxYear = 2020,
        transactionId = id1040000124,
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        outstandingAmount = 10.34,
        originalAmount = 10.34,
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId2"),
        interestFromDate = Some(LocalDate.of(2018, 3, 29)),
        interestEndDate = Some(LocalDate.of(2018, 3, 29)),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = Some(100),
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"))
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = "2019",
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some(id1040000123),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(100),
        clearedAmount = Some(100),
        chargeType = Some(NIC4_WALES),
        accruedInterest = Some(100),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = Some("Stand over order"),
          interestLock = Some("interestLock"),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem"),
          codedOutStatus = Some("I"))))),
      FinancialDetail(
        taxYear = "2020",
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some(id1040000124),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(100),
        clearedAmount = Some(100),
        chargeType = Some(NIC4_WALES),
        accruedInterest = Some(100),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = None,
          interestLock = None,
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem")))))
    )
  )

  val testValidFinancialDetailsModelWithBalancingCharge: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(taxYear = 2019,
        transactionId = id1040000123,
        documentDescription = None,
        documentText = Some("documentText"),
        documentDueDate = Some(LocalDate.of(2018, 3, 29)),
        outstandingAmount = 10.33,
        originalAmount = 10.33,
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId1"),
        interestFromDate = Some(LocalDate.of(2018, 3, 29)),
        interestEndDate = Some(LocalDate.of(2018, 3, 29)),
        accruingInterestAmount = None,
        lpiWithDunningLock = None,
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"))
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = "2019",
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some(id1040000123),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(100),
        clearedAmount = Some(100),
        chargeType = Some(NIC4_WALES),
        accruedInterest = Some(100),
        chargeReference = Some("chargeRef"),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = Some("Stand over order"),
          interestLock = Some("interestLock"),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem")))))
    )
  )

  val testValidFinancialDetailsModelWithITSAReturnAmendment: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(taxYear = 2019,
        transactionId = id1040000123,
        documentDescription = None,
        documentText = Some("documentText"),
        documentDueDate = Some(LocalDate.of(2018, 3, 29)),
        outstandingAmount = 10.33,
        originalAmount = 10.33,
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId1"),
        interestFromDate = Some(LocalDate.of(2018, 3, 29)),
        interestEndDate = Some(LocalDate.of(2018, 3, 29)),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = None,
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"))
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = "2019",
        mainType = Some("ITSA Return Amendment"),
        mainTransaction = Some("4915"),
        transactionId = Some(id1040000123),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(100),
        clearedAmount = Some(100),
        chargeType = Some(NIC4_WALES),
        accruedInterest = Some(100),
        chargeReference = Some("chargeRef"),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = Some("Stand over order"),
          interestLock = Some("interestLock"),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem")))))
    )
  )

  val testValidFinancialDetailsModelWithLateSubmissionPenalty: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(taxYear = 2019,
        transactionId = id1040000123,
        documentDescription = None,
        documentText = Some("documentText"),
        documentDueDate = Some(LocalDate.of(2018, 3, 29)),
        outstandingAmount = 10.33,
        originalAmount = 10.33,
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId1"),
        interestFromDate = Some(LocalDate.of(2018, 3, 29)),
        interestEndDate = Some(LocalDate.of(2018, 3, 29)),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = None,
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"))
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = "2019",
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4027"),
        transactionId = Some(id1040000123),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(100),
        clearedAmount = Some(100),
        chargeType = Some(NIC4_WALES),
        accruedInterest = Some(100),
        chargeReference = Some("chargeRef"),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = Some("Stand over order"),
          interestLock = Some("interestLock"),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem")))))
    )
  )

  val testValidFinancialDetailsModelWithLatePaymentPenalty: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(taxYear = 2021,
        transactionId = id1040000123,
        documentDescription = None,
        documentText = Some("documentText"),
        documentDueDate = Some(LocalDate.of(2020, 3, 29)),
        outstandingAmount = 200.33,
        originalAmount = 10.33,
        documentDate = LocalDate.of(2020, 1, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId1"),
        interestFromDate = Some(LocalDate.of(2020, 3, 29)),
        interestEndDate = Some(LocalDate.of(2020, 3, 29)),
        accruingInterestAmount = None,
        lpiWithDunningLock = None,
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"))
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = "2019",
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4028"),
        transactionId = Some(id1040000123),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(100),
        clearedAmount = Some(100),
        chargeType = Some(NIC4_WALES),
        accruedInterest = Some(100),
        chargeReference = Some("chargeRef"),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = Some("Stand over order"),
          interestLock = Some("interestLock"),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem")))))
    )
  )

  val testValidFinancialDetailsModelWithBalancingChargeWithAccruingInterest: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(
      DocumentDetail(taxYear = 2019,
        transactionId = id1040000123,
        documentDescription = None,
        documentText = Some("documentText"),
        documentDueDate = Some(LocalDate.of(2018, 3, 29)),
        outstandingAmount = 100,
        originalAmount = 10.33,
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(100),
        interestRate = Some(100),
        latePaymentInterestId = Some("latePaymentInterestId1"),
        interestFromDate = Some(LocalDate.of(2018, 3, 29)),
        interestEndDate = Some(LocalDate.of(2018, 3, 29)),
        accruingInterestAmount = Some(100),
        lpiWithDunningLock = None,
        paymentLotItem = Some("paymentLotItem"),
        paymentLot = Some("paymentLot"))
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = "2019",
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some(id1040000123),
        transactionDate = Some(LocalDate.parse("2020-08-16")),
        `type` = Some("type"),
        totalAmount = Some(100),
        originalAmount = Some(100),
        outstandingAmount = Some(0),
        clearedAmount = Some(100),
        chargeType = Some(NIC4_WALES),
        accruedInterest = Some(100),
        chargeReference = Some("chargeRef"),
        items = Some(Seq(SubItem(
          dueDate = Some(LocalDate.parse("2019-05-15")),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = Some("Stand over order"),
          interestLock = Some("interestLock"),
          clearingDate = Some(LocalDate.parse("2020-08-16")),
          clearingReason = Some("clearingReason"),
          clearingSAPDocument = None,
          outgoingPaymentMethod = Some("outgoingPaymentMethod"),
          paymentReference = Some("paymentReference"),
          paymentAmount = Some(100),
          paymentMethod = Some("paymentMethod"),
          paymentLot = Some("paymentLot"),
          paymentLotItem = Some("paymentLotItem"),
          paymentId = Some("paymentLot-paymentLotItem")))))
    )
  )

  val noDunningLocks: List[Option[String]] = List(None, None)
  val oneDunningLock: List[Option[String]] = List(Some("Stand over order"), None)
  val twoDunningLocks: List[Option[String]] = List(Some("Stand over order"), Some("Stand over order"))

  val dueDateMoreThan30Days: List[Option[LocalDate]] = List(Some(fixedDate.plusDays(45)), Some(fixedDate.plusDays(50)))
  val dueDateDueIn30Days: List[Option[LocalDate]] = List(Some(fixedDate), Some(fixedDate.plusDays(1)))

  val dueDateOverdue: List[Option[LocalDate]] = List(Some(fixedDate.minusDays(10)), Some(fixedDate.minusDays(1)))

  val currentYear: String = fixedDate.getYear.toString
  val currentYearMinusOne: String = (fixedDate.getYear - 1).toString

  private def testFinancialDetailsModel(dueDate: List[Option[LocalDate]],
                                        dunningLock: List[Option[String]],
                                        documentDescription: List[Option[String]] = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
                                        mainType: List[Option[String]] = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
                                        mainTransaction: List[Option[String]] = List(Some("4920"), Some("4930")),
                                        outstandingAmount: List[BigDecimal] = List(50, 75),
                                        taxYear: String = fixedDate.getYear.toString,
                                        balanceDetails: BalanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None)): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), None, None, None,
          None, None, Some(0), None, Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), None, None, None,
          None, None, None, None, Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head, dunningLock = dunningLock.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1), dunningLock = dunningLock(1)))))
      )
    )

  def testFinancialDetailsModelWithNoLpi(documentDescription: List[Option[String]],
                                         mainType: List[Option[String]],
                                         mainTransaction: List[Option[String]],
                                         dueDate: List[Option[LocalDate]],
                                         outstandingAmount: List[BigDecimal],
                                         taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId1"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), None, None, Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId2"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), None, None, Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1)))))
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
                                            accruingInterestAmount: List[Option[BigDecimal]] = List(None, None)): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), interestOutstandingAmount.head, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount.head, effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), interestOutstandingAmount(1), interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount(1), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = dueDate.head, dunningLock = dunningLock.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = dueDate(1), dunningLock = dunningLock(1)))))
      )
    )

  def testFinancialDetailsModelWithCodingOutNics2(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 12.34,
          originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None, interestRate = None,
          latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
          interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
          effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
          documentDueDate = Some(LocalDate.parse("2021-08-25"))
        )
      ),
      financialDetails = List(
        FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some("CODINGOUT01"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
          Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25"))))))
      )
    )

  def testFinancialDetailsModelWithPayeSACodingOut(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some("documentText"), outstandingAmount = 12.34,
          originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None, interestRate = None,
          latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
          interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
          effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
          documentDueDate = Some(LocalDate.parse("2021-08-25"))
        )),
      financialDetails = List(
        FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some("CODINGOUT01"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
          Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")),codedOutStatus = Some("I")))))
      )
    )
  def testFinancialDetailsModelWithPayeSACodingOutPOA1(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some("documentText"), outstandingAmount = 12.34,
          originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None, interestRate = None,
          latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
          interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
          effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
          documentDueDate = Some(LocalDate.parse("2021-08-25"))
        )),
      financialDetails = List(
        FinancialDetail("2021", Some("SA Payment on Account 1"), Some("4920"), Some("CODINGOUT01"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
          Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")),codedOutStatus = Some("I")))))
      )
    )

  def testFinancialDetailsModelWithPayeSACodingOutPOA1WithInterest(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some("documentText"), outstandingAmount = 12.34,
          originalAmount = 11.22, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = Some(BigDecimal(34.56)), interestRate = Some(BigDecimal(2)),
          latePaymentInterestId =  Some("latePaymentInterestId"), interestFromDate = Some(LocalDate.parse("2019-05-25")),
          interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = Some(BigDecimal(100)),
          effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
          documentDueDate = Some(LocalDate.parse("2021-08-25"))
        )),
      financialDetails = List(
        FinancialDetail("2021", Some("SA Payment on Account 1"), Some("4920"), Some("CODINGOUT01"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
          Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")),codedOutStatus = Some("I")))))
      )
    )

  def testFinancialDetailsModelWithPayeSACodingOutPOA2(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some("documentText"), outstandingAmount = 12.34,
          originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None, interestRate = None,
          latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
          interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
          effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
          documentDueDate = Some(LocalDate.parse("2021-08-25"))
        )),
      financialDetails = List(
        FinancialDetail("2021", Some("SA Payment on Account 2"), Some("4930"), Some("CODINGOUT01"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
          Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")),codedOutStatus = Some("I")))))
      )
    )

  def testFinancialDetailsModelWithCancelledPayeSa(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some("documentText"), outstandingAmount = 12.34,
          originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None, interestRate = None,
          latePaymentInterestId = None, interestFromDate = None,
          interestEndDate = None, accruingInterestAmount = None, amountCodedOut = Some(0),
          effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
          documentDueDate = Some(LocalDate.parse("2021-08-25"))
        )),
      financialDetails = List(
        FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some("CODINGOUT01"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
          Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")),codedOutStatus = Some("C")))))
      )
    )

  def financialDetailsModelWithPoaExtraCharge() =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(documentDetailModel(testTaxYear, paymentLot = None,
        paymentLotItem = None, accruingInterestAmount = None, documentDescription = Some("ITSA - POA 2"))),
      financialDetails = List(financialDetail(testTaxYear, mainType = "ITSA Poa 2", mainTransaction = "4930"),
        financialDetail(testTaxYear, transactionId = Some("123456"), mainType = "ITSA Poa 2 extra charge", mainTransaction = "4913")
      )
    )

  def testFinancialDetailsModelWithMFADebits(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2018, transactionId = "id1040000123", documentDescription = Some("TRM New Charge"),
          documentText = Some("documentText"), outstandingAmount = 1400.00, originalAmount = 1400.00,
          documentDate = LocalDate.of(2018, 3, 29), accruingInterestAmount = Some(100),
          interestOutstandingAmount = Some(80), lpiWithDunningLock = Some(100),
          effectiveDateOfPayment = Some(LocalDate.parse("2019-05-15")),
          documentDueDate = Some(LocalDate.parse("2019-05-15"))
        )),
      financialDetails = List(
        FinancialDetail("2018", Some("ITSA PAYE Charge"), Some("4000"), Some("id1040000123"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(1400),
          Some(1400), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2019-05-15"))))))
      )
    )

  def testFinancialDetailsModelWithAccruingInterest(documentDescription: List[Option[String]],
                                                    mainType: List[Option[String]],
                                                    mainTransaction: List[Option[String]],
                                                    dueDate: List[Option[LocalDate]],
                                                    dunningLock: List[Option[String]],
                                                    outstandingAmount: List[BigDecimal],
                                                    taxYear: String,
                                                    interestRate: List[Option[BigDecimal]],
                                                    accruingInterestAmount: List[Option[BigDecimal]]): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), None, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount.head, effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), None, interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount(1), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head, dunningLock = dunningLock.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1), dunningLock = dunningLock(1)))))
      )
    )

  def testFinancialDetailsModelWithLPIDunningLock(documentDescription: List[Option[String]],
                                                  mainType: List[Option[String]],
                                                  mainTransaction: List[Option[String]],
                                                  dueDate: List[Option[LocalDate]],
                                                  outstandingAmount: List[BigDecimal],
                                                  taxYear: String,
                                                  interestRate: List[Option[BigDecimal]],
                                                  accruingInterestAmount: Option[BigDecimal],
                                                  lpiWithDunningLock: Option[BigDecimal]
                                                 ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), None, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount, lpiWithDunningLock = Some(1000), effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), None, interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount, lpiWithDunningLock = Some(1000), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def testFinancialDetailsModelWithLpiDunningLockZero(documentDescription: List[Option[String]],
                                                      mainType: List[Option[String]],
                                                      mainTransaction: List[Option[String]],
                                                      dueDate: List[Option[LocalDate]],
                                                      outstandingAmount: List[BigDecimal],
                                                      taxYear: String,
                                                      interestRate: List[Option[BigDecimal]],
                                                      accruingInterestAmount: Option[BigDecimal],
                                                      lpiWithDunningLock: Option[BigDecimal]
                                                     ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), None, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount, lpiWithDunningLock = Some(0), effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), None, interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), accruingInterestAmount, lpiWithDunningLock = Some(0), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def testFinancialDetailsModelWithNoLpi(documentDescription: List[Option[String]],
                                         mainType: List[Option[String]],
                                         mainTransaction: List[Option[String]],
                                         dueDate: List[Option[LocalDate]],
                                         outstandingAmount: List[BigDecimal],
                                         taxYear: String,
                                         interestOutstandingAmount: List[Option[BigDecimal]],
                                         interestRate: List[Option[BigDecimal]]): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), interestOutstandingAmount.head, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), interestOutstandingAmount(1), interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def testFinancialDetailsModelWithChargesOfSameType(documentDescription: List[Option[String]],
                                                     mainType: List[Option[String]],
                                                     mainTransaction: List[Option[String]],
                                                     dueDate: List[Option[LocalDate]],
                                                     outstandingAmount: List[BigDecimal],
                                                     taxYear: String,
                                                     interestOutstandingAmount: List[Option[BigDecimal]] = List(Some(100), Some(100)),
                                                     accruingInterestAmount: List[Option[BigDecimal]] = List(Some(100), Some(100)),
                                                     interestEndDate: List[Option[LocalDate]] = List(Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29))),
                                                     lpiWithDunningLock: List[Option[BigDecimal]] = List(Some(100), Some(100)),
                                                     amountCodedOut: List[Option[BigDecimal]] = List(None, None),
                                                     chargeReference: List[Option[String]] = List(Some("ABCD1234"), Some("ABCD1234"))
                                                    ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = taxYear.toInt,
          transactionId = id1040000123,
          documentDescription = documentDescription.head,
          documentText = Some("documentText"),
          outstandingAmount = outstandingAmount.head,
          originalAmount = 43.21,
          documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = interestOutstandingAmount(0),
          interestRate = Some(100),
          latePaymentInterestId = Some("latePaymentInterestId"),
          interestFromDate = Some(LocalDate.of(2018, 3, 29)),
          interestEndDate = interestEndDate(0),
          accruingInterestAmount = accruingInterestAmount(0),
          lpiWithDunningLock = lpiWithDunningLock(0),
          paymentLotItem = Some("paymentLotItem"),
          paymentLot = Some("paymentLot"),
          effectiveDateOfPayment = dueDate.head,
          documentDueDate = dueDate.head,
          amountCodedOut = amountCodedOut.head),
        DocumentDetail(taxYear = taxYear.toInt,
          transactionId = id1040000124,
          documentDescription = documentDescription(1),
          documentText = Some("documentText"),
          outstandingAmount = outstandingAmount(1),
          originalAmount = 12.34,
          documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = interestOutstandingAmount(1),
          interestRate = Some(100),
          latePaymentInterestId = Some("latePaymentInterestId"),
          interestFromDate = Some(LocalDate.of(2018, 3, 29)),
          interestEndDate = interestEndDate(1),
          accruingInterestAmount = accruingInterestAmount(1),
          lpiWithDunningLock = lpiWithDunningLock(0),
          paymentLotItem = Some("paymentLotItem"),
          paymentLot = Some("paymentLot"),
          effectiveDateOfPayment = dueDate(1),
          documentDueDate = dueDate(1),
          amountCodedOut = amountCodedOut(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000123), Some(LocalDate.parse("2022-08-16")), chargeReference = chargeReference.head, Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000124), Some(LocalDate.parse("2022-08-16")), chargeReference = chargeReference(1), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )
  val testFinancialDetailsModelWithReviewAndReconcileAndPoas: FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(fixedDateTwo.getYear, id1040000125, Some("ITSA- POA 1"), Some("documentText"), 50, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = Some(fixedDateTwo.minusDays(1)), documentDueDate = Some(fixedDateTwo.plusDays(30))),
        DocumentDetail(fixedDateTwo.getYear, id1040000126, Some("ITSA - POA 2"), Some("documentText"), 50, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = Some(fixedDateTwo.minusDays(1)), documentDueDate = Some(fixedDateTwo.plusDays(30))),
        DocumentDetail(fixedDateTwo.getYear, id1040000123, Some("SA POA 1 Reconciliation Debit"), Some("documentText"), 50, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = Some(fixedDateTwo.minusDays(1)), documentDueDate = Some(fixedDateTwo.plusDays(30))),
        DocumentDetail(fixedDateTwo.getYear, id1040000124, Some("SA POA 2 Reconciliation Debit"), Some("documentText"), 75, 12.34, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = Some(fixedDateTwo.minusDays(1)), documentDueDate = Some(fixedDateTwo.plusDays(30)))
      ),
      financialDetails = List(
        FinancialDetail(fixedDateTwo.getYear.toString, Some("SA POA 1 Reconciliation"), Some("4911"), Some(id1040000123), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDateTwo.minusDays(1)))))),
        FinancialDetail(fixedDateTwo.getYear.toString, Some("SA POA 2 Reconciliation"), Some("4913"), Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDateTwo.plusDays(30))))))
      )
    )

  val testFinancialDetailsModelWithReviewAndReconcileInterest: FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(fixedDateTwo.getYear, id1040000125, Some("ITSA- POA 1"), Some("documentText"), 50, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = Some(fixedDateTwo.minusDays(1)), documentDueDate = Some(fixedDateTwo.plusDays(30))),
        DocumentDetail(fixedDateTwo.getYear, id1040000126, Some("ITSA - POA 2"), Some("documentText"), 50, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), Some("latePaymentInterestId"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(100), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = Some(fixedDateTwo.minusDays(1)), documentDueDate = Some(fixedDateTwo.plusDays(30))),
        DocumentDetail(fixedDateTwo.getYear, id1040000123, Some("SA POA 1 Reconciliation Debit"), Some("documentText"), 0, 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100), None,
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(150), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = Some(fixedDateTwo.minusDays(1)), documentDueDate = Some(fixedDateTwo.plusDays(30))),
        DocumentDetail(fixedDateTwo.getYear, id1040000124, Some("SA POA 2 Reconciliation Debit"), Some("documentText"), 0, 12.34, LocalDate.of(2018, 3, 29), Some(100), Some(100), None,
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), Some(150), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = Some(fixedDateTwo.minusDays(1)), documentDueDate = Some(fixedDateTwo.plusDays(30)))
      ),
      financialDetails = List(
        FinancialDetail(fixedDateTwo.getYear.toString, Some("SA POA 1 Reconciliation"), Some("4911"), Some(id1040000123), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(0), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDateTwo.minusDays(1)))))),
        FinancialDetail(fixedDateTwo.getYear.toString, Some("SA POA 2 Reconciliation"), Some("4913"), Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(0), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDateTwo.plusDays(30))))))
      )
    )


  def testFinancialDetailsModelOneItemInList(documentDescription: Option[String],
                                             mainType: Option[String],
                                             mainTransaction: Option[String],
                                             dueDate: Option[LocalDate],
                                             outstandingAmount: BigDecimal,
                                             taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
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

  val testInvalidFinancialDetailsJson: JsValue = Json.obj(
    "amount" -> "invalidAmount",
    "payMethod" -> "Payment by Card",
    "valDate" -> "2019-05-27"
  )

  val testFinancialDetailsErrorModelParsing: FinancialDetailsErrorModel = FinancialDetailsErrorModel(
    testErrorStatus, "Json Validation Error. Parsing FinancialDetails Data Response")

  val testFinancialDetailsErrorModel: FinancialDetailsErrorModel = FinancialDetailsErrorModel(testErrorStatus, testErrorMessage)
  val testFinancialDetailsErrorModelJson: JsValue = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

  val testFinancialDetailsNotFoundErrorModel: FinancialDetailsErrorModel = FinancialDetailsErrorModel(testErrorNotFoundStatus, testErrorMessage)


  def outstandingChargesModel(dueDate: LocalDate, aciAmount: BigDecimal = 12.67): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456.67, 1234), OutstandingChargeModel("ACI", None, aciAmount, 1234))
  )

  val outstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(fixedDate.minusDays(30))

  val outstandingChargesDueInMoreThan30Days: OutstandingChargesModel = outstandingChargesModel(fixedDate.plusDays(35))

  val outstandingChargesDueIn30Days: OutstandingChargesModel = outstandingChargesModel(fixedDate.plusDays(30))

  def financialDetailsDueInMoreThan30Days(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel = testFinancialDetailsModel(
    dueDate = dueDateMoreThan30Days,
    dunningLock = dunningLocks,
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00, Some(100.00), None, None, Some(350.00), None, None, Some(100.00))
  )

  def financialDetailsDueIn30Days(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel = testFinancialDetailsModel(
    dueDate = dueDateDueIn30Days,
    dunningLock = dunningLocks,
    balanceDetails = BalanceDetails(50.00, 0.00, 50.00, None, None, None, None, None, None, None)
  )

  def financialDetailsOverdueData(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel = testFinancialDetailsModel(
    dueDate = dueDateOverdue,
    dunningLock = dunningLocks,
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00, None, None, None, None, None, None, None)
  )

  def financialDetailsOverdueDataWithInterest(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel =
    testFinancialDetailsModelWithInterest(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks
    )
//
  val financialDetailsBalancingCharges: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("TRM New Charge"), Some("TRM Amend Charge")),
    mainType = List(Some("SA Balancing Charge"), Some("SA Balancing Charge")),
    mainTransaction = List(Some("4910"), Some("4910")),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks
  )

  val financialDetailsMFADebits: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("TRM New Charge"), Some("TRM New Charge")),
    mainType = List(Some("ITSA PAYE Charge"), Some("ITSA Calc Error Correction")),
    mainTransaction = List(Some("4000"), Some("4001")),
    dueDate = List(Some(fixedDate.minusDays(1)), Some(fixedDate.plusDays(35))),
    outstandingAmount = List(100, 50),
    taxYear = fixedDate.getYear.toString,
    interestOutstandingAmount = List(None, None),
    accruingInterestAmount = List(None, None)
  )
//

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
  val financialDetailsReviewAndReconcileInterest: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("SA POA 1 Reconciliation Debit"), Some("SA POA 2 Reconciliation Debit")),
    mainType = List(Some("SA POA 1 Reconciliation"), Some("SA POA 2 Reconciliation")),
    mainTransaction = List(Some("4911"), Some("4913")),
    dueDate = List(Some(fixedDate.minusDays(1)), Some(fixedDate.plusDays(30))),
    outstandingAmount = List(0, 0),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None),
    interestOutstandingAmount = List(Some(100.00), Some(40.00)),
    interestEndDate = List(Some(LocalDate.of(2100, 1, 1)), Some(LocalDate.of(2100, 1, 1))),
    lpiWithDunningLock = List(None, None)
  )

  val financialDetailsWithMixedData1: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    mainTransaction = List(Some("4920"), Some("4930")),
    dueDate = List(Some(fixedDate.plusDays(35)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(25, 50),
    taxYear = fixedDate.getYear.toString
  )

  val financialDetailsWithMixedData2: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    mainTransaction = List(Some("4920"), Some("4930")),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString
  )

  val financialDetailsWithMixedData3: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    mainTransaction = List(Some("4920"), Some("4930")),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None)
  )

  val financialDetailsWithMixedData4: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("SA POA 1 Reconciliation Debit"), Some("ITSA - POA 2")),
    mainType = List(Some("SA POA 1 Reconciliation"), Some("SA Payment on Account 2")),
    mainTransaction = List(Some("4911"), Some("4930")),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None)
  )

  val financialDetailsWithMixedData4Penalties: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("Late Submission Penalty"), Some("ITSA - POA 2")),
    mainType = List(Some("LSP"), Some("SA Payment on Account 2")),
    mainTransaction = List(Some("4027"), Some("4930")),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = List(50, 75),
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = List(None, None)
  )

  def financialDetailsWithOutstandingChargesAndLpi(outstandingAmount: List[BigDecimal] = List(0, 0),
                                                   accruingInterestAmount: List[Option[BigDecimal]] = List(None, None),
                                                   interestOutstandingAmount: List[Option[BigDecimal]] = List(None, None),
                                                   amountCodedOut: List[Option[BigDecimal]] = List(None, None)
                                                  ): FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    mainTransaction = List(Some("4920"), Some("4930")),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = outstandingAmount,
    taxYear = fixedDate.getYear.toString,
    accruingInterestAmount = accruingInterestAmount,
    interestOutstandingAmount = interestOutstandingAmount,
    amountCodedOut = amountCodedOut
  )



  val codedOutDocumentDetailsA = DocumentDetail(2022, id1040000124, Some("documentDescription"), Some("documentText"),
    BigDecimal("5.00"), 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100),
    Some("latePaymentInterestId"), Some(LocalDate.of(2018, 3, 29)),
    Some(LocalDate.of(2018, 3, 29)), None, Some(100), Some("paymentLotItem"), Some("paymentLot"),
    amountCodedOut = Some(BigDecimal("2500.00")))

  val whatYouOweEmptyMFA: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.0, 2.0, 3.0, None, None, None, None, None, None, None), List(), Some(OutstandingChargesModel(List())), None)

//  val whatYouOwePartialChargesList: WhatYouOweChargesList = WhatYouOweChargesList(
//    balanceDetails = BalanceDetails(balanceDueWithin30Days = 1.00, overDueAmount = 2.00, totalBalance = 3.00, None, None, None, None, None),
//    chargesList =
//      testFinancialDetailsModelWithInterest(documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
//        mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
//        mainTransaction = List(Some("4920"), Some("4930")),
//        dueDate = dueDateOverdue,
//        dunningLock = oneDunningLock,
//        outstandingAmount = List(50, 75),
//        taxYear = fixedDate.getYear.toString,
//        interestOutstandingAmount = List(Some(42.50), Some(24.05)),
//        interestRate = List(Some(2.6), Some(6.2)),
//        accruingInterestAmount = List(Some(34.56), None)
//      ).getAllDocumentDetailsWithDueDates() ++
//        testFinancialDetailsModelOneItemInList(documentDescription = Some("ITSA - POA 2"),
//          mainType = Some("SA Payment on Account 2"),
//          mainTransaction = Some("4930"),
//          dueDate = Some(fixedDate.plusDays(1)),
//          outstandingAmount = 100,
//          taxYear = fixedDate.getYear.toString).getAllDocumentDetailsWithDueDates() ++
//        testFinancialDetailsModelOneItemInList(documentDescription = Some("ITSA- POA 1"),
//          mainType = Some("SA Payment on Account 1"),
//          mainTransaction = Some("4920"),
//          dueDate = Some(fixedDate.plusDays(45)),
//          outstandingAmount = 125,
//          taxYear = fixedDate.getYear.toString).getAllDocumentDetailsWithDueDates(),
//    outstandingChargesModel = Some(outstandingChargesOverdueData),
//    codedOutDocumentDetail = Some(codedOutDocumentDetailsA)
//  )


  val whatYouOweEmpty: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.0, 2.0, 3.0, None, None, None, None, None, None, None), List(), Some(OutstandingChargesModel(List())), None)

  val creditDocumentDetailList = List(
    documentDetailModel(originalAmount = BigDecimal(-100.00), paymentLotItem = None, paymentLot = None),
    documentDetailModel(originalAmount = BigDecimal(-500.00), paymentLotItem = None, paymentLot = None)
  )

  val creditAndRefundDocumentDetailList = List(
    documentDetailModel(transactionId = id1040000124, outstandingAmount = BigDecimal(-100.00), paymentLotItem = Some("1"),
      paymentLot = Some("01")),
    documentDetailModel(transactionId = id1040000125, outstandingAmount = BigDecimal(-500.00), paymentLotItem = Some("2"), paymentLot = Some("02"))
  )

  val financialDetailCreditCharge = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(1.00), Some(3.00), Some(7.00), Some(2.00), Some(4.00), None),
    documentDetails = creditDocumentDetailList,
    financialDetails = List(
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")))))),
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000126), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out")))))
    )
  )

  val financialDetailCreditAndRefundCharge = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(0), Some(3.00), Some(7.00), Some(2.00), Some(4.00), None),
    documentDetails = creditAndRefundDocumentDetailList,
    financialDetails = List(
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")))))),
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000126), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out")))))
    )
  )

  val creditAndRefundDocumentDetailListMFA = List(
    documentDetailModel(documentDescription = Some("ITSA Overpayment Relief"), outstandingAmount = BigDecimal(-1400.00), paymentLotItem = None, paymentLot = None),
    documentDetailModel(documentDescription = Some("ITSA Standalone Claim"), outstandingAmount = BigDecimal(-500.00), paymentLotItem = None, paymentLot = None)
  )

  val financialDetailCreditChargeMFA = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(1.00), Some(3.00), Some(7.00), Some(2.00), Some(4.00), None),
    documentDetails = creditAndRefundDocumentDetailListMFA,
    financialDetails = List(
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")))))),
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
      FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000126), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
        Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out")))))
    )
  )

  val creditAndRefundDocumentDetailAllCreditTypesList = List(
    documentDetailModel(transactionId = id1040000124, outstandingAmount = BigDecimal(-100.00), paymentLotItem = Some("1"), paymentLot = Some("01")),
    documentDetailModel(transactionId = id1040000125, outstandingAmount = BigDecimal(-500.00), paymentLotItem = Some("2"), paymentLot = Some("02")),
    documentDetailModel(transactionId = id1040000126, outstandingAmount = BigDecimal(-300.00), paymentLotItem = Some("2"), paymentLot = Some("02")),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "BCC01", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -250.00, outstandingAmount = BigDecimal(-250.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "BCC02", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -125.00, outstandingAmount = BigDecimal(-125.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFACREDIT01", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -100.00, outstandingAmount = BigDecimal(-100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFACREDIT02", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -1000.00, outstandingAmount = BigDecimal(-1000.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFACREDIT03", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -800.00, outstandingAmount = BigDecimal(-800.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None),
    documentDetailModel(documentDescription = Some("ITSA PAYE Charge"), transactionId = "CUTOVERCREDIT01", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -200.00, outstandingAmount = BigDecimal(-200.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None),
    documentDetailModel(documentDescription = Some("ITSA PAYE Charge"), transactionId = "CUTOVERCREDIT02", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -2000.00, outstandingAmount = BigDecimal(-2000.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None),
    documentDetailModel(documentDescription = Some("ITSA PAYE Charge"), transactionId = "CUTOVERCREDIT01", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -700.00, outstandingAmount = BigDecimal(-700.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None)
  )

  val financialDetailCreditAndRefundChargeAllCreditTypes = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(1.00), Some(6.00), Some(3.00), Some(7.00), Some(2.00), Some(4.00), None),
    documentDetails = creditAndRefundDocumentDetailAllCreditTypesList,
    financialDetails = List(
      FinancialDetail("2018", Some("SA Balancing Charge Credit"), Some("4905"), Some("BCC01"), totalAmount = Some(250), originalAmount = Some(250), outstandingAmount = Some(250), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail("2018", Some("SA Balancing Charge Credit"), Some("4905"), Some("BCC02"), totalAmount = Some(125), originalAmount = Some(125), outstandingAmount = Some(125), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail("2021", Some("Payment"), Some("0060"), Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")))))),
      FinancialDetail("2021", Some("Payment"), Some("0060"), Some(id1040000125), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(500), Some(500), Some(500), Some(500), Some(NIC4_WALES), Some(500), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
      FinancialDetail("2021", Some("Payment"), Some("0060"), Some(id1040000126), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(300), Some(300), Some(300), Some(300), Some(NIC4_WALES), Some(300), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
      FinancialDetail("2018", Some("ITSA Overpayment Relief"), Some("4004"), Some("MFACREDIT01"), totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail("2018", Some("ITSA Overpayment Relief"), Some("4004"), Some("MFACREDIT02"), totalAmount = Some(1000), originalAmount = Some(1000), outstandingAmount = Some(1000), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail("2018", Some("ITSA PAYE in year Repayment"), Some("4011"), Some("MFACREDIT03"), totalAmount = Some(800), originalAmount = Some(800), outstandingAmount = Some(800), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail("2018", Some("ITSA Cutover Credits"), Some("6110"), Some("CUTOVERCREDIT01"), totalAmount = Some(200), originalAmount = Some(200), outstandingAmount = Some(200), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail("2018", Some("ITSA Cutover Credits"), Some("6110"), Some("CUTOVERCREDIT02"), totalAmount = Some(2000), originalAmount = Some(2000), outstandingAmount = Some(2000), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail("2018", Some("ITSA Cutover Credits"), Some("6110"), Some("CUTOVERCREDIT03"), totalAmount = Some(700), originalAmount = Some(700), outstandingAmount = Some(700), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val creditAndRefundCreditDetailListMFA = {
    val documentDetail1 = documentDetailModel(documentDescription = Some("ITSA Overpayment Relief"), outstandingAmount = BigDecimal(-1400.00), paymentLotItem = None, paymentLot = None)
    val documentDetail2 = documentDetailModel(documentDescription = Some("ITSA Standalone Claim"), outstandingAmount = BigDecimal(-500.00), paymentLotItem = None, paymentLot = None)

    val newCharge = ChargeItem(
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
      lpiWithDunningLock = None,
      amountCodedOut = None,
      dueDate = Some(LocalDate.parse("2022-01-01")), dunningLock = false,
      poaRelevantAmount = None,
      chargeReference = Some("chargeRef"))
    List(
      CreditDetailModel(documentDetail1.documentDate, charge = newCharge, MfaCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment),
      CreditDetailModel(documentDetail2.documentDate, charge = newCharge, MfaCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment)
    )
  }

  val creditAndRefundCreditDetailListMFAWithCutoverAndBCC = {
    val documentDetailMFA1 = documentDetailModel(documentDescription = Some("ITSA Overpayment Relief"), outstandingAmount = BigDecimal(-1400.00), paymentLotItem = None, paymentLot = None)
    val documentDetailMFA2 = documentDetailModel(documentDescription = Some("ITSA Standalone Claim"), outstandingAmount = BigDecimal(-500.00), paymentLotItem = None, paymentLot = None)
    val documentDetailCutoverCredit1 = documentDetailModel(documentDescription = Some("ITSA Cutover Credits"), outstandingAmount = BigDecimal(200.00), paymentLotItem = None, paymentLot = None, originalAmount = 200)
    val documentDetailCutoverCredit2 = documentDetailModel(documentDescription = Some("ITSA Cutover Credits"), outstandingAmount = BigDecimal(1200.00), paymentLotItem = None, paymentLot = None, originalAmount = 2000)

    val newCharge = ChargeItem(
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
      lpiWithDunningLock = None,
      amountCodedOut = None,
      dueDate = Some(LocalDate.parse("2022-01-01")), dunningLock = false,
      poaRelevantAmount = None,
      chargeReference = Some("chargeRef"))
    List(
      CreditDetailModel(documentDetailMFA1.documentDate, charge = newCharge, MfaCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment),
      CreditDetailModel(documentDetailMFA2.documentDate, charge = newCharge, MfaCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment),
      CreditDetailModel(documentDetailCutoverCredit1.documentDate, charge = newCharge, CutOverCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment),
      CreditDetailModel(documentDetailCutoverCredit2.documentDate, charge = newCharge, CutOverCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment),
      CreditDetailModel(documentDetailCutoverCredit2.documentDate, charge = newCharge, BalancingChargeCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment),
      CreditDetailModel(documentDetailCutoverCredit2.documentDate, charge = newCharge, BalancingChargeCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment)
    )
  }

  val creditAndRefundDocumentDetailMFA = documentDetailModel(documentDescription = Some("ITSA Overpayment Relief"), outstandingAmount = BigDecimal(-1400.00), paymentLotItem = None, paymentLot = None)


  val creditAndRefundDocumentDetailListMultipleChargesMFA = List(
    documentDetailModel(
      documentDescription = Some("ITSA Standalone Claim"),
      outstandingAmount = BigDecimal(-500.00),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(-800.00),
      documentDate = LocalDate.of(2018, 4, 16)
    ),
    documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-1400.00),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(-1400.00),
      documentDate = LocalDate.of(2018, 7, 30)
    )
  )

  val creditAndRefundCreditDetailListMultipleChargesMFA = {
    val documentDetail1 = documentDetailModel(
      documentDescription = Some("ITSA Standalone Claim"),
      outstandingAmount = BigDecimal(-500.00),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(-800.00),
      documentDate = LocalDate.of(2018, 4, 16))
    val financialDetailsMfa1 = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
        mainTransaction = Some("4004"), transactionId = Some(documentDetail1.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
      )
    )
    val documentDetail2 = documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-1400.00),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(-1400.00),
      documentDate = LocalDate.of(2018, 7, 30))
    val financialDetailsMfa2 = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
        mainTransaction = Some("4004"), transactionId = Some(documentDetail1.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
      )
    )
    val newCharge1:  ChargeItem = ChargeItem.fromDocumentPair(documentDetail1, financialDetailsMfa1)
    val newCharge2:  ChargeItem = ChargeItem.fromDocumentPair(documentDetail2, financialDetailsMfa2)
    List(
      CreditDetailModel(documentDetail1.documentDate, charge = newCharge1, MfaCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment),
      CreditDetailModel(documentDetail2.documentDate, charge = newCharge2, MfaCreditType, financialDetailCreditCharge.balanceDetails.totalCreditAvailableForRepayment)
    )
  }

  val creditAndRefundDocumentDetailListFullyAllocatedMFA = List(
    documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(0),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(20)
    )
  )

  val creditAndRefundCreditDetailListFullyAllocatedMFA = {
    val documentDetail = documentDetailModel(
      transactionId = "someTransactionId",
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(0),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(20)
    )
    val financialDetailMfa = FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
      mainTransaction = Some("4004"), transactionId = Some(documentDetail.transactionId),
      transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
      clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
    )

    val newChargeItem: ChargeItem = ChargeItem.fromDocumentPair(
      documentDetail, List(financialDetailMfa)
    )
    List(CreditDetailModel(
      date = documentDetail.documentDate,
      charge = newChargeItem,//documentDetail,
      creditType = MfaCreditType,
      availableCredit = None
    ))
  }

  val creditAndRefundDocumentDetailListNotYetAllocatedMFA = List(
    documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-3000),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(-3000)
    )
  )

  val creditAndRefundCreditDetailListNotYetAllocatedMFA = {
    val documentDetail = documentDetailModel(
      transactionId = "someTransactionId",
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-3000),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(-3000)
    )
    val financialDetailsMfa = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
        mainTransaction = Some("4004"), transactionId = Some(documentDetail.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
      )
    )
    val newChargeItem : ChargeItem = ChargeItem.fromDocumentPair(
      documentDetail = documentDetail,
      financialDetails = financialDetailsMfa)
    List(CreditDetailModel(
      date = documentDetail.documentDate,
      charge = newChargeItem,//documentDetail,
      creditType = MfaCreditType,
      availableCredit = None
    ))
  }

  val creditAndRefundDocumentDetailListPartiallyAllocatedMFA = List(
    documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-3000),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(1000)
    )
  )

  val creditAndRefundCreditDetailListPartiallyAllocatedMFA = {
    val documentDetail = documentDetailModel(
      transactionId = "someTransactionId",
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-3000),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(1000)
    )
    val financialDetailsMfa = List(
      FinancialDetail(taxYear = "2022", mainType = Some("ITSA Overpayment Relief"),
        mainTransaction = Some("4004"), transactionId = Some(documentDetail.transactionId),
        transactionDate = None, `type` = None, totalAmount = None, originalAmount = None, outstandingAmount = None,
        clearedAmount = None, chargeType = None, accruedInterest = None, items = Some(Seq(SubItem(Some( LocalDate.parse("2022-08-25") ))))
      )
    )

    val newChargeItem : ChargeItem = ChargeItem.fromDocumentPair(
      documentDetail = documentDetail,
      financialDetails = financialDetailsMfa)

    List(CreditDetailModel(
      date = documentDetail.documentDate,
      charge = newChargeItem,
      creditType = MfaCreditType,
      availableCredit = None
    ))
  }

  val MFADebitsDocumentDetails: List[DocumentDetail] = List(
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT01",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15))),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT02",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15))),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT03",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15)))
  )

  val ReviewAndReconcileDocumentDetails: List[DocumentDetail] = List(
    documentDetailModel(documentDescription = Some("SA POA 1 Reconciliation Debit"), transactionId = "RARDEBIT01",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15))),
    documentDetailModel(documentDescription = Some("SA POA 2 Reconciliation Debit"), transactionId = "RARDEBIT02",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15)))
  )

  val ReviewAndReconcileDocumentDetailsNotDue: List[DocumentDetail] = List(
    documentDetailModel(documentDescription = Some("SA POA 1 Reconciliation Debit"), transactionId = "RARDEBIT01",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15))),
    documentDetailModel(documentDescription = Some("SA POA 2 Reconciliation Debit"), transactionId = "RARDEBIT02",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15)))
  )

  val penaltiesDocumentDetails: List[DocumentDetail] = List(
    documentDetailModel( transactionId = "LSP",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15))),
    documentDetailModel(transactionId = "LPP1",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15))),
    documentDetailModel(transactionId = "LPP2",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, accruingInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2100, 5, 15)))
  )

//  val ReviewAndReconcileDocumentDetailsWithDueDate: List[DocumentDetailWithDueDate] =
//    ReviewAndReconcileDocumentDetails.map(dd => DocumentDetailWithDueDate(dd, dd.documentDueDate,
//      isReviewAndReconcilePoaOneDebit = dd.documentDescription.contains("SA POA 1 Reconciliation Debit"),
//      isReviewAndReconcilePoaTwoDebit = dd.documentDescription.contains("SA POA 2 Reconciliation Debit")
//    ))

  val MFADebitsFinancialDetails: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = MFADebitsDocumentDetails,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA PAYE Charge"), mainTransaction = Some("4000"), transactionId = Some("MFADEBIT01"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA Calc Error Correction"), mainTransaction = Some("4001"), transactionId = Some("MFADEBIT02"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA Manual Penalty Pre CY-4"), mainTransaction = Some("4002"), transactionId = Some("MFADEBIT03"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val financialDetailsWithReviewAndReconcileDebits: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = ReviewAndReconcileDocumentDetailsNotDue,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainType = Some("SA POA 1 Reconciliation Debit"), mainTransaction = Some("4911"), transactionId = Some("RARDEBIT01"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("SA POA 2 Reconciliation Debit"), mainTransaction = Some("4913"), transactionId = Some("RARDEBIT02"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val financialDetailsWithReviewAndReconcileDebitsOverdue: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = ReviewAndReconcileDocumentDetails,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainType = Some("SA POA 1 Reconciliation Debit"), mainTransaction = Some("4911"), transactionId = Some("RARDEBIT01"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("SA POA 2 Reconciliation Debit"), mainTransaction = Some("4913"), transactionId = Some("RARDEBIT02"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val financialDetailsWithAllThreePenalties: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = penaltiesDocumentDetails,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainTransaction = Some("4027"), transactionId = Some("LSP"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainTransaction = Some("4028"), transactionId = Some("LPP1"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainTransaction = Some("4029"), transactionId = Some("LPP2"), chargeReference = Some("chargeRef123"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val financialDetailsWithLPP2NoChargeRef: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(3.00), Some(7.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = penaltiesDocumentDetails,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainTransaction = Some("4029"), transactionId = Some("LPP2"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val MFADebitsDocumentDetailsWithDueDates: List[DocumentDetailWithDueDate] = MFADebitsFinancialDetails.getAllDocumentDetailsWithDueDates()

}

case class ANewCreditAndRefundModel(model: CreditsModel = CreditsModel(0.0, 0.0, 0.0, 0.0, 0.0, Nil)) {

  def withAvailableCredit(availableCredit: BigDecimal): ANewCreditAndRefundModel = {
    ANewCreditAndRefundModel(model.copy(availableCreditForRepayment = availableCredit))
  }

  def withAllocatedFutureCredit(allocatedCredit: BigDecimal): ANewCreditAndRefundModel = {
    ANewCreditAndRefundModel(model.copy(allocatedCreditForFutureCharges = allocatedCredit))
  }

  def withAllocatedOverdueCredit(allocatedCredit: BigDecimal): ANewCreditAndRefundModel = {
    ANewCreditAndRefundModel(model.copy(allocatedCredit = allocatedCredit))
  }

  def withTotalCredit(totalCredit: BigDecimal): ANewCreditAndRefundModel = {
    ANewCreditAndRefundModel(model.copy(totalCredit = totalCredit))
  }

  def withUnallocatedCredit(unallocatedCredit: BigDecimal): ANewCreditAndRefundModel = {
    ANewCreditAndRefundModel(model.copy(unallocatedCredit = unallocatedCredit))
  }

  def withCutoverCredit(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(CutOverCreditType, outstandingAmount, taxYear = Some(TaxYear.getTaxYear(dueDate)), dueDate = Some(dueDate), "cutover")))
  }

  def withFirstRefund(amount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(Repayment, amount, taxYear = None, None, "refund1")))
  }

  def withSecondRefund(amount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(Repayment, amount, taxYear = None, None, "refund2")))
  }

  def withBalancingChargeCredit(dueDate: LocalDate, outstandingAmount: BigDecimal, id: String = "balancing") = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(BalancingChargeCreditType, outstandingAmount, taxYear = Some(TaxYear.getTaxYear(dueDate)), dueDate = Some(dueDate), id)))
  }

  def withRepaymentInterest(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(RepaymentInterest, outstandingAmount, taxYear = Some(TaxYear.getTaxYear(dueDate)), dueDate = Some(dueDate), "repayment")))
  }

  def withMfaCredit(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(MfaCreditType, outstandingAmount, taxYear = Some(TaxYear.getTaxYear(dueDate)), dueDate = Some(dueDate), "mfa")))
  }

  def withPayment(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(PaymentType, outstandingAmount, taxYear = Some(TaxYear.getTaxYear(dueDate)), dueDate = Some(dueDate), "payment")))
  }

  def get(): CreditsModel = model
}

object CreditAndRefundConstants {
  def balanceDetailsModel(firstPendingAmountRequested: Option[BigDecimal] = Some(3.50),
                          secondPendingAmountRequested: Option[BigDecimal] = Some(2.50),
                          availableCredit: Option[BigDecimal] = Some(7.00),
                          allocatedCreditOverdue: Option[BigDecimal] = Some(1.00),
                          allocatedCreditFuture: Option[BigDecimal] = Some(2.00),
                          totalCredit: Option[BigDecimal] = Some(4.00),
                          unallocatedCredit: Option[BigDecimal] = None): BalanceDetails = BalanceDetails(
    balanceDueWithin30Days = 1.00,
    overDueAmount = 2.00,
    totalBalance = 3.00,
    totalCreditAvailableForRepayment = availableCredit,
    allocatedCredit = allocatedCreditOverdue,
    allocatedCreditForFutureCharges = allocatedCreditFuture,
    totalCredit = totalCredit,
    firstPendingAmountRequested = firstPendingAmountRequested,
    secondPendingAmountRequested = secondPendingAmountRequested,
    unallocatedCredit
  )

  def documentDetailWithDueDateFinancialDetailListModel(outstandingAmount: BigDecimal = -1400.0,
                                                        dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                                                        originalAmount: BigDecimal = 1400.00,
                                                        mainType: String = "SA Payment on Account 1",
                                                        mainTransaction: String = "4920",
                                                        paymentLot: Option[String] = None,
                                                        paymentLotItem: Option[String] = None
                                                       ):
  (DocumentDetailWithDueDate, FinancialDetail) = {
    (documentDetailWithDueDateModel(
      paymentLot = paymentLot,
      paymentLotItem = paymentLotItem,
      outstandingAmount = outstandingAmount,
      dueDate = dueDate,
      originalAmount = originalAmount),
      financialDetail(mainType = mainType, mainTransaction = mainTransaction))
  }

  def documentAndFinancialDetailWithCreditType(taxYear: Int = 2018, outstandingAmount: BigDecimal = BigDecimal(-1400.0), originalAmount: BigDecimal = BigDecimal(-2400.0), mainType: String, mainTransaction: String, dueDate: LocalDate = LocalDate.of(2019, 5, 15)):
  (DocumentDetailWithDueDate, FinancialDetail) = {
    (documentDetailWithDueDateModel(
      dueDate = Some(dueDate),
      taxYear = taxYear,
      paymentLot = None,
      paymentLotItem = None,
      documentDescription = Some("TRM New Charge"),
      outstandingAmount = outstandingAmount,
      originalAmount = originalAmount),
      financialDetail(
        taxYear = taxYear,
        mainType = mainType,
        mainTransaction = mainTransaction)
    )
  }

  def documentDetailWithDueDateFinancialDetailListModelMFA(taxYear: Int = 2018, outstandingAmount: BigDecimal = BigDecimal(-1400.0)):
  (DocumentDetailWithDueDate, FinancialDetail) = {
    (documentDetailWithDueDateModel(
      taxYear = taxYear,
      paymentLot = None,
      paymentLotItem = None,
      documentDescription = Some("TRM New Charge"),
      outstandingAmount = outstandingAmount,
      originalAmount = BigDecimal(-2400.0)),
      financialDetail(mainType = "ITSA Overpayment Relief",
        mainTransaction = "4004")
    )
  }
}