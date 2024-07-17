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
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.libs.json.{JsValue, Json}
import services.DateService
import testConstants.BaseTestConstants.{app, chargeReference, testErrorMessage, testErrorNotFoundStatus, testErrorStatus, testTaxYear}
import testConstants.FinancialDetailsTestConstants.{documentDetailWithDueDateModel, financialDetail}

import java.time.LocalDate


object FinancialDetailsTestConstants {

  val fixedDate: LocalDate = LocalDate.of(2023, 12, 15)

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
        "latePaymentInterestAmount" -> 100,
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
        "latePaymentInterestAmount" -> 100,
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
            "paymentId" -> "paymentLot-paymentLotItem"
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
        "latePaymentInterestAmount" -> 100,
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
        "latePaymentInterestAmount" -> 100,
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
            "paymentId" -> "paymentLot-paymentLotItem"
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
        "latePaymentInterestAmount" -> 100,
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
        "latePaymentInterestAmount" -> 100,
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
                          latePaymentInterestAmount: Option[BigDecimal] = Some(100),
                          interestOutstandingAmount: Option[BigDecimal] = Some(80),
                          transactionId: String = id1040000123,
                          lpiWithDunningLock: Option[BigDecimal] = Some(100),
                          effectiveDateOfPayment: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15))): DocumentDetail =
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
      latePaymentInterestAmount = latePaymentInterestAmount,
      lpiWithDunningLock = lpiWithDunningLock,
      paymentLotItem = paymentLotItem,
      paymentLot = paymentLot,
      effectiveDateOfPayment = effectiveDateOfPayment
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
                      transactionId: Option[String] = Some(id1040000123)
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
          clearingReason = Some("clearingReason")
        )
      ) ++ additionalSubItems)
  )


  def documentDetailWithDueDateModel(taxYear: Int = 2018,
                                     documentDescription: Option[String] = Some("ITSA- POA 1"),
                                     documentText: Option[String] = Some("documentText"),
                                     outstandingAmount: BigDecimal = 1400.00,
                                     originalAmount: BigDecimal = 1400.00,
                                     transactionId: String = id1040000123,
                                     latePaymentInterestAmount: Option[BigDecimal] = Some(100),
                                     paymentLot: Option[String] = Some("paymentLot"),
                                     paymentLotItem: Option[String] = None,
                                     dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                                     isMFADebit: Boolean = false): DocumentDetailWithDueDate =
    DocumentDetailWithDueDate(documentDetailModel(taxYear = taxYear, documentDescription = documentDescription, outstandingAmount = outstandingAmount,
      originalAmount = originalAmount, documentText = documentText, transactionId = transactionId, paymentLot = paymentLot, paymentLotItem = paymentLotItem,
      latePaymentInterestAmount = latePaymentInterestAmount, documentDueDate = dueDate), dueDate = dueDate, isMFADebit = isMFADebit)

  val balanceDetails: BalanceDetails = BalanceDetails(
    balanceDueWithin30Days = 1.00,
    overDueAmount = 2.00,
    totalBalance = 3.00,
    availableCredit = Some(100.00),
    None,
    None,
    None,
    None
  )

  val documentDetailPOA1: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("ITSA- POA 1"))
  val documentDetailPOA2: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("ITSA - POA 2"))
  val documentDetailBalancingCharge: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"))
  val documentDetailAmendedBalCharge: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM Amend Charge"))
  val documentDetailClass2Nic: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CLASS2_NICS), paymentLot = None, latePaymentInterestAmount = None)
  val documentDetailPaye: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED), paymentLot = None, latePaymentInterestAmount = None)
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
                             outstandingAmount: BigDecimal = 1400.0,
                             dunningLock: Option[String] = None,
                             lpiWithDunningLock: Option[BigDecimal] = Some(100),
                             dueDateValue: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                             latePaymentInterestAmount: Option[BigDecimal] = Some(100)
                           ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(documentDetailModel(taxYear, outstandingAmount = outstandingAmount, paymentLot = None,
        paymentLotItem = None, lpiWithDunningLock = lpiWithDunningLock, latePaymentInterestAmount = latePaymentInterestAmount)),
      financialDetails = List(financialDetail(taxYear, dunningLock = dunningLock, dueDateValue = dueDateValue)
      )
    )

  def financialDetailsModelWithMFADebit() =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(documentDetailModel(testTaxYear, paymentLot = None,
        paymentLotItem = None, latePaymentInterestAmount = None)),
      financialDetails = List(financialDetail(testTaxYear, mainType = "ITSA Calc Error Correction", mainTransaction = "4022")
      )
    )

  def chargesWithAllocatedPaymentModel(taxYear: Int = 2018,
                                       outstandingAmount: BigDecimal = 1400.0,
                                       lpiWithDunningLock: Option[BigDecimal] = Some(100)): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
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
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
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
        latePaymentInterestAmount = Some(100),
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
        latePaymentInterestAmount = Some(100),
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
          paymentId = Some("paymentLot-paymentLotItem"))))),
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
                                        balanceDetails: BalanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None)): FinancialDetailsModel =
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
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
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

  def testFinancialDetailsModelWithCodingOutNics2(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 12.34,
          originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None, interestRate = None,
          latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
          interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
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
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some(CODING_OUT_ACCEPTED), outstandingAmount = 12.34,
          originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None, interestRate = None,
          latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
          interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
          effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
          documentDueDate = Some(LocalDate.parse("2021-08-25"))
        )),
      financialDetails = List(
        FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some("CODINGOUT01"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
          Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25"))))))
      )
    )

  def testFinancialDetailsModelWithCancelledPayeSa(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2021, transactionId = "CODINGOUT01", documentDescription = Some("TRM New Charge"),
          documentText = Some(CODING_OUT_CANCELLED), outstandingAmount = 12.34,
          originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
          interestOutstandingAmount = None, interestRate = None,
          latePaymentInterestId = None, interestFromDate = None,
          interestEndDate = None, latePaymentInterestAmount = None, amountCodedOut = Some(0),
          effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
          documentDueDate = Some(LocalDate.parse("2021-08-25"))
        )),
      financialDetails = List(
        FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some("CODINGOUT01"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100),
          Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25"))))))
      )
    )

  def testFinancialDetailsModelWithMFADebits(): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear = 2018, transactionId = "id1040000123", documentDescription = Some("TRM New Charge"),
          documentText = Some("documentText"), outstandingAmount = 1400.00, originalAmount = 1400.00,
          documentDate = LocalDate.of(2018, 3, 29), latePaymentInterestAmount = Some(100),
          interestOutstandingAmount = Some(80), lpiWithDunningLock = Some(100),
          effectiveDateOfPayment = Some(LocalDate.parse("2019-05-15")),
          documentDueDate = Some(LocalDate.parse("2019-05-15"))
        )),
      financialDetails = List(
        FinancialDetail("2018", Some("ITSA PAYE Charge"), Some("4000"), Some("id1040000123"), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(1400),
          Some(1400), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2019-05-15"))))))
      )
    )

  def testFinancialDetailsModelWithLPI(documentDescription: List[Option[String]],
                                       mainType: List[Option[String]],
                                       mainTransaction: List[Option[String]],
                                       dueDate: List[Option[LocalDate]],
                                       dunningLock: List[Option[String]],
                                       outstandingAmount: List[BigDecimal],
                                       taxYear: String,
                                       interestRate: List[Option[BigDecimal]],
                                       latePaymentInterestAmount: List[Option[BigDecimal]]): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), None, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount.head, effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), None, interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount(1), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
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
                                                  latePaymentInterestAmount: Option[BigDecimal],
                                                  lpiWithDunningLock: Option[BigDecimal]
                                                 ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), None, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount, lpiWithDunningLock = Some(1000), effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), None, interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount, lpiWithDunningLock = Some(1000), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
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
                                                      latePaymentInterestAmount: Option[BigDecimal],
                                                      lpiWithDunningLock: Option[BigDecimal]
                                                     ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), None, interestRate.head, Some("latePaymentInterestId1"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount, lpiWithDunningLock = Some(0), effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000125, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), None, interestRate(1), Some("latePaymentInterestId2"), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount, lpiWithDunningLock = Some(0), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
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
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
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
                                                     latePaymentInterestAmount: List[Option[BigDecimal]] = List(Some(100), Some(100))
                                                    ): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(
        DocumentDetail(taxYear.toInt, id1040000123, documentDescription.head, Some("documentText"), outstandingAmount.head, 43.21, LocalDate.of(2018, 3, 29), interestOutstandingAmount(0), Some(100), Some("latePaymentInterestId"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), latePaymentInterestAmount(0), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate.head, documentDueDate = dueDate.head),
        DocumentDetail(taxYear.toInt, id1040000124, documentDescription(1), Some("documentText"), outstandingAmount(1), 12.34, LocalDate.of(2018, 3, 29), interestOutstandingAmount(1), Some(100), Some("latePaymentInterestId"),
          Some(LocalDate.of(2018, 3, 29)), Some(LocalDate.of(2018, 3, 29)), latePaymentInterestAmount(1), Some(100), Some("paymentLotItem"), Some("paymentLot"), effectiveDateOfPayment = dueDate(1), documentDueDate = dueDate(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, mainTransaction.head, Some(id1040000123), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), mainTransaction(1), Some(id1040000124), Some(LocalDate.parse("2022-08-16")), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
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
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00, None, None, None, None, Some(100.00))
  )

  def financialDetailsDueIn30Days(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel = testFinancialDetailsModel(
    dueDate = dueDateDueIn30Days,
    dunningLock = dunningLocks,
    balanceDetails = BalanceDetails(1.00, 0.00, 1.00, None, None, None, None, None)
  )

  def financialDetailsOverdueData(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel = testFinancialDetailsModel(
    dueDate = dueDateOverdue,
    dunningLock = dunningLocks,
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00, None, None, None, None, None)
  )

  def financialDetailsOverdueDataWithInterest(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel =
    testFinancialDetailsModelWithInterest(
      dueDate = dueDateOverdue,
      dunningLock = dunningLocks
    )

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
    mainTransaction = List(Some("4000"), Some("4022")),
    dueDate = List(Some(fixedDate.minusDays(1)), Some(fixedDate.plusDays(35))),
    outstandingAmount = List(100, 50),
    taxYear = fixedDate.getYear.toString,
    interestOutstandingAmount = List(None, None),
    latePaymentInterestAmount = List(None, None)
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
    latePaymentInterestAmount = List(None, None)
  )

  def financialDetailsWithOutstandingChargesAndLpi(outstandingAmount: List[BigDecimal] = List(0, 0),
                                                   latePaymentInterestAmount: List[Option[BigDecimal]] = List(None, None),
                                                   interestOutstandingAmount: List[Option[BigDecimal]] = List(None, None)
                                                  ): FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    mainTransaction = List(Some("4920"), Some("4930")),
    dueDate = List(Some(fixedDate.plusDays(30)), Some(fixedDate.minusDays(1))),
    outstandingAmount = outstandingAmount,
    taxYear = fixedDate.getYear.toString,
    latePaymentInterestAmount = latePaymentInterestAmount,
    interestOutstandingAmount = interestOutstandingAmount
  )

  def whatYouOweDataWithPaidPOAs(dunningLocks: List[Option[String]] = noDunningLocks)(implicit dateService: DateService): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 0.00, 1.00, None, None, None, None, None),
    chargesList = financialDetailsDueIn30Days(dunningLocks)
      .getAllDocumentDetailsWithDueDates()(dateService)
      .map(d => d.copy(documentDetail = d.documentDetail.copy(outstandingAmount = 0.0))),
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )


  def whatYouOweDataWithDataDueIn30Days(dunningLocks: List[Option[String]] = noDunningLocks)(implicit dateService: DateService): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 0.00, 1.00, None, None, None, None, None),
    chargesList = financialDetailsDueIn30Days(dunningLocks).getAllDocumentDetailsWithDueDates()(dateService),
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )

  val whatYouOweDataWithMixedData1: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates().head,
      financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates()(dateService)(1)),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  def whatYouOweDataWithOverdueData(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueData(dunningLocks).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueDataAndInterest(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsOverdueDataWithInterest(dunningLocks).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )


  def whatYouOweDataWithDataDueInMoreThan30Days(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00, None, None, None, None, Some(BigDecimal(100.00))),
    chargesList = financialDetailsDueInMoreThan30Days(dunningLocks).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )

  def whatYouOweDataWithZeroMoneyInAccount(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00, None, None, None, None, Some(BigDecimal(0.00))),
    chargesList = financialDetailsDueInMoreThan30Days(dunningLocks).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )

  val whatYouOweDataWithMixedData2: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates().head,
      financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates()(dateService)(1)),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val codedOutDocumentDetailsA = DocumentDetail(2022, id1040000124, Some("documentDescription"), Some("documentText"),
    BigDecimal("5.00"), 43.21, LocalDate.of(2018, 3, 29), Some(100), Some(100),
    Some("latePaymentInterestId"), Some(LocalDate.of(2018, 3, 29)),
    Some(LocalDate.of(2018, 3, 29)), None, Some(100), Some("paymentLotItem"), Some("paymentLot"),
    amountCodedOut = Some(BigDecimal("2500.00")))

  val whatYouOwePartialChargesList: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(balanceDueWithin30Days = 1.00, overDueAmount = 2.00, totalBalance = 3.00, None, None, None, None, None),
    chargesList =
      testFinancialDetailsModelWithInterest(documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
        mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
        mainTransaction = List(Some("4920"), Some("4930")),
        dueDate = dueDateOverdue,
        dunningLock = oneDunningLock,
        outstandingAmount = List(50, 75),
        taxYear = fixedDate.getYear.toString,
        interestOutstandingAmount = List(Some(42.50), Some(24.05)),
        interestRate = List(Some(2.6), Some(6.2)),
        latePaymentInterestAmount = List(Some(34.56), None)
      ).getAllDocumentDetailsWithDueDates() ++
        testFinancialDetailsModelOneItemInList(documentDescription = Some("ITSA - POA 2"),
          mainType = Some("SA Payment on Account 2"),
          mainTransaction = Some("4930"),
          dueDate = Some(fixedDate.plusDays(1)),
          outstandingAmount = 100,
          taxYear = fixedDate.getYear.toString).getAllDocumentDetailsWithDueDates() ++
        testFinancialDetailsModelOneItemInList(documentDescription = Some("ITSA- POA 1"),
          mainType = Some("SA Payment on Account 1"),
          mainTransaction = Some("4920"),
          dueDate = Some(fixedDate.plusDays(45)),
          outstandingAmount = 125,
          taxYear = fixedDate.getYear.toString).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesOverdueData),
    codedOutDocumentDetail = Some(codedOutDocumentDetailsA)
  )

  val whatYouOweDataWithMFADebitsData: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    chargesList = financialDetailsMFADebits.getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOweEmptyMFA: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.0, 2.0, 3.0, None, None, None, None, None), List(), Some(OutstandingChargesModel(List())), None)


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
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(1.00), Some(2.00), Some(4.00), None),
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
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(0), Some(2.00), Some(4.00), None),
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
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(1.00), Some(2.00), Some(4.00), None),
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
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "BCC01", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -250.00, outstandingAmount = BigDecimal(-250.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "BCC02", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -125.00, outstandingAmount = BigDecimal(-125.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFACREDIT01", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -100.00, outstandingAmount = BigDecimal(-100.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFACREDIT02", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -1000.00, outstandingAmount = BigDecimal(-1000.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFACREDIT03", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -800.00, outstandingAmount = BigDecimal(-800.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None),
    documentDetailModel(documentDescription = Some("ITSA PAYE Charge"), transactionId = "CUTOVERCREDIT01", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -200.00, outstandingAmount = BigDecimal(-200.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None),
    documentDetailModel(documentDescription = Some("ITSA PAYE Charge"), transactionId = "CUTOVERCREDIT02", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -2000.00, outstandingAmount = BigDecimal(-2000.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None),
    documentDetailModel(documentDescription = Some("ITSA PAYE Charge"), transactionId = "CUTOVERCREDIT01", documentDate = LocalDate.of(2018, 3, 29), originalAmount = -700.00, outstandingAmount = BigDecimal(-700.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None)
  )

  val financialDetailCreditAndRefundChargeAllCreditTypes = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(1.00), Some(6.00), Some(2.00), Some(4.00), None),
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

    List(
      CreditDetailModel(documentDetail1.documentDate, documentDetail1, MfaCreditType, Some(financialDetailCreditCharge.balanceDetails)),
      CreditDetailModel(documentDetail2.documentDate, documentDetail2, MfaCreditType, Some(financialDetailCreditCharge.balanceDetails))
    )
  }

  val creditAndRefundCreditDetailListMFAWithCutoverAndBCC = {
    val documentDetailMFA1 = documentDetailModel(documentDescription = Some("ITSA Overpayment Relief"), outstandingAmount = BigDecimal(-1400.00), paymentLotItem = None, paymentLot = None)
    val documentDetailMFA2 = documentDetailModel(documentDescription = Some("ITSA Standalone Claim"), outstandingAmount = BigDecimal(-500.00), paymentLotItem = None, paymentLot = None)
    val documentDetailCutoverCredit1 = documentDetailModel(documentDescription = Some("ITSA Cutover Credits"), outstandingAmount = BigDecimal(200.00), paymentLotItem = None, paymentLot = None, originalAmount = 200)
    val documentDetailCutoverCredit2 = documentDetailModel(documentDescription = Some("ITSA Cutover Credits"), outstandingAmount = BigDecimal(1200.00), paymentLotItem = None, paymentLot = None, originalAmount = 2000)
    val documentDetailBCC1 = documentDetailModel(documentDescription = Some("ITSA- Bal Charge"), outstandingAmount = BigDecimal(1200.00), paymentLotItem = None, paymentLot = None, originalAmount = 2000)
    val documentDetailBCC2 = documentDetailModel(documentDescription = Some("ITSA- Bal Charge"), outstandingAmount = BigDecimal(1400.00), paymentLotItem = None, paymentLot = None, originalAmount = 200)

    List(
      CreditDetailModel(documentDetailMFA1.documentDate, documentDetailMFA1, MfaCreditType, Some(financialDetailCreditCharge.balanceDetails)),
      CreditDetailModel(documentDetailMFA2.documentDate, documentDetailMFA2, MfaCreditType, Some(financialDetailCreditCharge.balanceDetails)),
      CreditDetailModel(documentDetailCutoverCredit1.documentDate, documentDetailCutoverCredit1, CutOverCreditType, Some(financialDetailCreditCharge.balanceDetails)),
      CreditDetailModel(documentDetailCutoverCredit2.documentDate, documentDetailCutoverCredit2, CutOverCreditType, Some(financialDetailCreditCharge.balanceDetails)),
      CreditDetailModel(documentDetailCutoverCredit2.documentDate, documentDetailBCC1, BalancingChargeCreditType, Some(financialDetailCreditCharge.balanceDetails)),
      CreditDetailModel(documentDetailCutoverCredit2.documentDate, documentDetailBCC2, BalancingChargeCreditType, Some(financialDetailCreditCharge.balanceDetails))
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
    val documentDetail2 = documentDetailModel(
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-1400.00),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(-1400.00),
      documentDate = LocalDate.of(2018, 7, 30))

    List(
      CreditDetailModel(documentDetail1.documentDate, documentDetail1, MfaCreditType, Some(financialDetailCreditCharge.balanceDetails)),
      CreditDetailModel(documentDetail2.documentDate, documentDetail2, MfaCreditType, Some(financialDetailCreditCharge.balanceDetails))
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
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(0),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(20)
    )
    List(CreditDetailModel(
      date = documentDetail.documentDate,
      documentDetail = documentDetail,
      creditType = MfaCreditType
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
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-3000),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(-3000)
    )
    List(CreditDetailModel(
      date = documentDetail.documentDate,
      documentDetail = documentDetail,
      creditType = MfaCreditType
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
      documentDescription = Some("ITSA Overpayment Relief"),
      outstandingAmount = BigDecimal(-3000),
      paymentLotItem = None,
      paymentLot = None,
      originalAmount = BigDecimal(1000)
    )
    List(CreditDetailModel(
      date = documentDetail.documentDate,
      documentDetail = documentDetail,
      creditType = MfaCreditType
    ))
  }

  val MFADebitsDocumentDetails: List[DocumentDetail] = List(
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT01",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15))),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT02",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15))),
    documentDetailModel(documentDescription = Some("TRM New Charge"), transactionId = "MFADEBIT03",
      documentDate = LocalDate.of(2018, 3, 29), originalAmount = 100.00,
      outstandingAmount = BigDecimal(100.00), paymentLotItem = None, paymentLot = None, latePaymentInterestAmount = None,
      effectiveDateOfPayment = Some(LocalDate.of(2019, 5, 15)),
      documentDueDate = Some(LocalDate.of(2019, 5, 15)))
  )

  val MFADebitsFinancialDetails: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(1.00), Some(2.00), Some(4.00), None),
    documentDetails = MFADebitsDocumentDetails,
    financialDetails = List(
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA PAYE Charge"), mainTransaction = Some("4000"), transactionId = Some("MFADEBIT01"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA Calc Error Correction"), mainTransaction = Some("4022"), transactionId = Some("MFADEBIT02"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15)))))),
      FinancialDetail(taxYear = "2018", mainType = Some("ITSA Manual Penalty Pre CY-4"), mainTransaction = Some("4002"), transactionId = Some("MFADEBIT03"),
        totalAmount = Some(100), originalAmount = Some(100), outstandingAmount = Some(100), items = Some(Seq(SubItem(Some(LocalDate.of(2019, 5, 15))))))
    )
  )

  val MFADebitsDocumentDetailsWithDueDates: List[DocumentDetailWithDueDate] = MFADebitsFinancialDetails.getAllDocumentDetailsWithDueDates()

  def whatYouOweDataWithAvailableCredits(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00, Some(300.00), None, None, None, Some(BigDecimal(100.00))),
    chargesList = financialDetailsDueInMoreThan30Days(dunningLocks).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )

}

case class ANewCreditAndRefundModel(model: CreditsModel = CreditsModel(0.0, 0.0, Nil)) {

  def withAvailableCredit(availableCredit: BigDecimal): ANewCreditAndRefundModel = {
    ANewCreditAndRefundModel(model.copy(availableCredit = availableCredit))
  }

  def withAllocatedCredit(allocatedCredit: BigDecimal): ANewCreditAndRefundModel = {
    ANewCreditAndRefundModel(model.copy(allocatedCredit = allocatedCredit))
  }

  def withCutoverCredit(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(CutOverCreditType, outstandingAmount, taxYear = Some(s"${dueDate.getYear}"), dueDate = Some(dueDate))))
  }

  def withFirstRefund(amount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(Repayment, amount, taxYear = None, None)))
  }

  def withSecondRefund(amount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(Repayment, amount, taxYear = None, None)))
  }

  def withBalancingChargeCredit(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(BalancingChargeCreditType, outstandingAmount, taxYear = Some(s"${dueDate.getYear}"), dueDate = Some(dueDate))))
  }

  def withRepaymentInterest(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(RepaymentInterest, outstandingAmount, taxYear = Some(s"${dueDate.getYear}"), dueDate = Some(dueDate))))
  }

  def withMfaCredit(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(MfaCreditType, outstandingAmount, taxYear = Some(s"${dueDate.getYear}"), dueDate = Some(dueDate))))
  }

  def withPayment(dueDate: LocalDate, outstandingAmount: BigDecimal) = {
    ANewCreditAndRefundModel(model.copy(transactions = model.transactions :+
      Transaction(PaymentType, outstandingAmount, taxYear = Some(s"${dueDate.getYear}"), dueDate = Some(dueDate))))
  }

  def get(): CreditsModel = model
}

object CreditAndRefundConstants {
  def balanceDetailsModel(firstPendingAmountRequested: Option[BigDecimal] = Some(3.50),
                          secondPendingAmountRequested: Option[BigDecimal] = Some(2.50),
                          availableCredit: Option[BigDecimal] = Some(7.00),
                          allocatedCredit: Option[BigDecimal] = Some(1.00),
                          unallocatedCredit: Option[BigDecimal] = None): BalanceDetails = BalanceDetails(
    balanceDueWithin30Days = 1.00,
    overDueAmount = 2.00,
    totalBalance = 3.00,
    availableCredit = availableCredit,
    allocatedCredit = allocatedCredit,
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
