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

package assets

import java.time.LocalDate

import assets.BaseTestConstants.{testErrorMessage, testErrorNotFoundStatus, testErrorStatus}
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.libs.json.{JsValue, Json}

object FinancialDetailsTestConstants {

  val id1040000123 = "1040000123"
  val id1040000124 = "1040000124"
  val id1040000125 = "1040000125"

  val testValidFinancialDetailsModelJson: JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "totalBalance" -> 3.00
    ),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "transactionId" -> id1040000123,
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 10.33,
        "originalAmount" -> 10.33,
				"documentDate" -> "2018-03-29",
        "interestOutstandingAmount"-> 100,
        "interestRate"-> 100,
        "interestFromDate"-> "2018-03-29",
        "interestEndDate"-> "2018-03-29",
        "latePaymentInterestAmount" -> 100,
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
        "interestOutstandingAmount"-> 100,
        "interestRate"-> 100,
        "interestFromDate"-> "2018-03-29",
        "interestEndDate"-> "2018-03-29",
        "latePaymentInterestAmount" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> id1040000123,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> "NIC4 Wales",
        "accruedInterest" -> 100,
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> "2019-05-15",
          "subItemId" -> "1",
          "amount" -> 100,
          "dunningLock" -> "Stand over order",
            "interestLock" -> "interestLock",
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
        "transactionId" -> id1040000124,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> "NIC4 Wales",
        "accruedInterest" -> 100,
        "items" -> Json.arr(
          Json.obj("dueDate" -> "2019-05-15",
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


  val testValidFinancialDetailsLpiModelJson: JsValue = Json.obj(
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "transactionId" -> id1040000123,
        "documentDescription" -> "TRM New Charge",
        "outstandingAmount" -> 10.33,
        "originalAmount" -> 10.33,
        "documentDate" -> "2018-03-29",
        "interestOutstandingAmount"-> 100,
        "interestRate"-> 100,
        "interestFromDate"-> "2018-03-29",
        "interestEndDate"-> "2018-03-29",
        "latePaymentInterestAmount" -> 100,
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
        "interestOutstandingAmount"-> 100,
        "interestRate"-> 100,
        "interestFromDate"-> "2018-03-29",
        "interestEndDate"-> "2018-03-29",
        "latePaymentInterestAmount" -> 100,
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> id1040000123,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> "NIC4 Wales",
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> "2019-05-15",
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
        "transactionId" -> id1040000124,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> "NIC4 Wales",
        "items" -> Json.arr(
          Json.obj("dueDate" -> "2019-05-15",
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
        "interestRate"-> 100,
        "interestFromDate"-> "2018-03-29",
        "interestEndDate"-> "2018-03-29",
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
        "interestRate"-> 100,
        "interestFromDate"-> "2018-03-29",
        "interestEndDate"-> "2018-03-29",
        "paymentLotItem" -> "paymentLotItem",
        "paymentLot" -> "paymentLot"
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "transactionId" -> id1040000123,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> "NIC4 Wales",
        "items" -> Json.arr(
          Json.obj(
            "dueDate" -> "2019-05-15",
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
        "transactionId" -> id1040000124,
        "transactionDate" -> "transactionDate",
        "type" -> "type",
        "totalAmount" -> 100,
        "originalAmount" -> 100,
        "outstandingAmount" -> 100,
        "clearedAmount" -> 100,
        "chargeType" -> "NIC4 Wales",
        "items" -> Json.arr(
          Json.obj("dueDate" -> "2019-05-15",
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
                          outstandingAmount: Option[BigDecimal] = Some(1400.00),
                          originalAmount: Option[BigDecimal] = Some(1400.00),
													paymentLotItem: Option[String] = Some("paymentLotItem"),
													paymentLot: Option[String] = Some("paymentLot")): DocumentDetail =
    DocumentDetail(
      taxYear = taxYear.toString,
      transactionId = id1040000123,
      documentDescription,
      outstandingAmount = outstandingAmount,
      originalAmount = originalAmount,
			documentDate = LocalDate.of(2018, 3, 29),
      interestOutstandingAmount = Some(80),
      interestRate = Some(100),
      interestFromDate = Some(LocalDate.of(2018, 3, 29)),
      interestEndDate = Some(LocalDate.of(2018, 6, 15)),
      latePaymentInterestAmount = Some(100),
      paymentLotItem = paymentLotItem,
      paymentLot = paymentLot
    )

  def financialDetail(taxYear: Int = 2018,
                      mainType: String = "SA Payment on Account 1",
                      chargeType: String = "NIC4 Wales",
                      originalAmount: BigDecimal = 100,
                      dunningLock: Option[String] = None,
                      interestLock: Option[String] = None,
                      accruedInterest: Option[BigDecimal] = None,
											additionalSubItems: Seq[SubItem] = Seq()): FinancialDetail = FinancialDetail.apply(
    taxYear = taxYear.toString,
    mainType = Some(mainType),
    transactionId = Some(id1040000123),
    transactionDate = Some("transactionDate"),
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
          dueDate = Some(LocalDate.of(2019, 5, 15).toString),
          subItemId = Some("1"),
          amount = Some(100),
          dunningLock = dunningLock,
          interestLock = interestLock,
          clearingDate = Some("2019-07-23"),
          clearingReason = Some("clearingReason")
        )
      ) ++ additionalSubItems)
  )


  def documentDetailWithDueDateModel(taxYear: Int = 2018,
                                     documentDescription: Option[String] = Some("ITSA- POA 1"),
                                     outstandingAmount: Option[BigDecimal] = Some(1400.00),
                                     originalAmount: Option[BigDecimal] = Some(1400.00),
                                     dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15))): DocumentDetailWithDueDate =
    DocumentDetailWithDueDate(documentDetailModel(taxYear, documentDescription, outstandingAmount, originalAmount), dueDate)


  val documentDetailPOA1: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("ITSA- POA 1"))
  val documentDetailPOA2: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("ITSA - POA 2"))
  val documentDetailBalancingCharge: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM New Charge"))
  val documentDetailAmendedBalCharge: DocumentDetailWithDueDate = documentDetailWithDueDateModel(documentDescription = Some("TRM Amend Charge"))

  val fullDocumentDetailModel: DocumentDetail = documentDetailModel()
  val fullFinancialDetailModel: FinancialDetail = financialDetail()

  val fullDocumentDetailWithDueDateModel: DocumentDetailWithDueDate = DocumentDetailWithDueDate(fullDocumentDetailModel, Some(LocalDate.of(2019, 5, 15)))

  def financialDetailsModel(taxYear: Int = 2018, outstandingAmount: Option[BigDecimal] = Some(1400.0), dunningLock: Option[String] = None): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
      documentDetails = List(documentDetailModel(taxYear, outstandingAmount = outstandingAmount, paymentLot = None, paymentLotItem = None)),
      financialDetails = List(financialDetail(taxYear, dunningLock = dunningLock))
    )

	def chargesWithAllocatedPaymentModel(taxYear: Int = 2018, outstandingAmount: Option[BigDecimal] = Some(1400.0)): FinancialDetailsModel =
		FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
			documentDetails = List(documentDetailModel(taxYear, outstandingAmount = outstandingAmount, paymentLot = None, paymentLotItem = None),
				documentDetailModel(9999, outstandingAmount = outstandingAmount.map (amount => -amount), paymentLot = Some("paymentLot"), paymentLotItem = Some("paymentLotItem"))),
			financialDetails = List(financialDetail(taxYear, additionalSubItems = Seq(SubItem(clearingDate = Some("2018-09-08"), amount = Some(500.0), paymentAmount = Some(500.0),
				paymentLot = Some("paymentLot"), paymentLotItem = Some("paymentLotItem")))))
		)

  val testValidFinancialDetailsModel: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    documentDetails = List(
      DocumentDetail("2019", id1040000123, Some("TRM New Charge"), Some(10.33), Some(10.33), LocalDate.of(2018, 3, 29), Some(100),Some(100),
        Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),Some(100),Some("paymentLotItem"), Some("paymentLot")),
      DocumentDetail("2020", id1040000124, Some("TRM New Charge"), Some(10.34), Some(10.34), LocalDate.of(2018, 3, 29), Some(100),Some(100),
        Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),Some(100),Some("paymentLotItem"), Some("paymentLot"))
    ),
    financialDetails = List(
      FinancialDetail("2019", Some("SA Balancing Charge"),Some(id1040000123)  ,Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"),Some(100), Some(Seq(SubItem(Some("2019-05-15"),Some("1"),Some(100),
        Some("Stand over order"), Some("interestLock"), Some("clearingDate"),Some("clearingReason"),Some("outgoingPaymentMethod"),Some("paymentReference"), Some(100),Some("paymentMethod"),Some("paymentLot"),Some("paymentLotItem"),Some("paymentLot-paymentLotItem"))))),
      FinancialDetail("2020", Some("SA Balancing Charge"),Some(id1040000124)  ,Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"),Some(100), Some(Seq(SubItem(Some("2019-05-15"),Some("1"),Some(100),
        None, None, Some("clearingDate"),Some("clearingReason"),Some("outgoingPaymentMethod"),Some("paymentReference"), Some(100),Some("paymentMethod"),Some("paymentLot"),Some("paymentLotItem"),Some("paymentLot-paymentLotItem")))))
    )
  )

  val noDunningLocks: List[Option[String]] = List(None, None)
  val oneDunningLock: List[Option[String]] = List(Some("Stand over order"), None)
  val twoDunningLocks: List[Option[String]] = List(Some("Stand over order"), Some("Stand over order"))

  val dueDateMoreThan30Days: List[Option[String]] = List(Some(LocalDate.now().plusDays(45).toString), Some(LocalDate.now().plusDays(50).toString))
  val dueDateDueIn30Days: List[Option[String]] = List(Some(LocalDate.now().toString), Some(LocalDate.now().plusDays(1).toString))
  val dueDateOverdue: List[Option[String]] = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString))

  val currentYear: String = LocalDate.now().getYear.toString
  val currentYearMinusOne: String = (LocalDate.now().getYear - 1).toString

  private def testFinancialDetailsModel(dueDate: List[Option[String]],
                                        dunningLock: List[Option[String]],
                                        documentDescription: List[Option[String]] = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
                                        mainType: List[Option[String]] = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
                                        outstandingAmount: List[Option[BigDecimal]] = List(Some(50), Some(75)),
                                        taxYear: String = LocalDate.now().getYear.toString,
                                        balanceDetails: BalanceDetails = BalanceDetails(1.00, 2.00, 3.00)): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = balanceDetails,
      documentDetails = List(
        DocumentDetail(taxYear, id1040000124, documentDescription.head, outstandingAmount.head, Some(43.21), LocalDate.of(2018, 3, 29), Some(100),Some(100),
          Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),Some(100),Some("paymentLotItem"), Some("paymentLot")),
        DocumentDetail(taxYear, id1040000125, documentDescription(1), outstandingAmount(1), Some(12.34), LocalDate.of(2018, 3, 29), Some(100),Some(100),
          Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),Some(100),Some("paymentLotItem"), Some("paymentLot"))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, Some(id1040000124) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate.head, dunningLock = dunningLock.head)))),
        FinancialDetail(taxYear, mainType(1), Some(id1040000125) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate(1), dunningLock = dunningLock(1)))))
      )
    )

  def testFinancialDetailsModelWithNoLpi(documentDescription: List[Option[String]],
                                       mainType: List[Option[String]],
                                       dueDate: List[Option[String]],
                                       outstandingAmount: List[Option[BigDecimal]],
                                       taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
      documentDetails = List(
        DocumentDetail(taxYear, id1040000124, documentDescription.head, outstandingAmount.head, Some(43.21), LocalDate.of(2018, 3, 29), Some(100),Some(100),
          Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),None,Some("paymentLotItem"), Some("paymentLot")),
        DocumentDetail(taxYear, id1040000125, documentDescription(1), outstandingAmount(1), Some(12.34), LocalDate.of(2018, 3, 29), Some(100),Some(100),
          Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),None,Some("paymentLotItem"), Some("paymentLot"))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, Some(id1040000124) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), Some(id1040000125) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def testFinancialDetailsModelWithInterest(documentDescription: List[Option[String]],
                                mainType: List[Option[String]],
                                dueDate: List[Option[String]],
                                dunningLock: List[Option[String]],
                                outstandingAmount: List[Option[BigDecimal]],
                                taxYear: String,
                                interestOutstandingAmount: List[Option[BigDecimal]],
                                interestRate: List[Option[BigDecimal]],
                                latePaymentInterestAmount: List[Option[BigDecimal]]): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
      documentDetails = List(
        DocumentDetail(taxYear, id1040000124, documentDescription.head, outstandingAmount.head, Some(43.21), LocalDate.of(2018, 3, 29), interestOutstandingAmount.head, interestRate.head, Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount.head),
        DocumentDetail(taxYear, id1040000125, documentDescription(1), outstandingAmount(1), Some(12.34), LocalDate.of(2018, 3, 29), interestOutstandingAmount(1), interestRate(1), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, Some(id1040000124) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = dueDate.head, dunningLock = dunningLock.head)))),
        FinancialDetail(taxYear, mainType(1), Some(id1040000125) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = dueDate(1), dunningLock = dunningLock(1)))))
      )
    )

  def testFinancialDetailsModelWithLPI(documentDescription: List[Option[String]],
                                       mainType: List[Option[String]],
                                       dueDate: List[Option[String]],
                                       dunningLock: List[Option[String]],
                                       outstandingAmount: List[Option[BigDecimal]],
                                       taxYear: String,
                                       interestRate: List[Option[BigDecimal]],
                                       latePaymentInterestAmount: List[Option[BigDecimal]]): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
      documentDetails = List(
        DocumentDetail(taxYear, id1040000124, documentDescription.head, outstandingAmount.head, Some(43.21), LocalDate.of(2018, 3, 29),None, interestRate.head, Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount.head),
        DocumentDetail(taxYear, id1040000125, documentDescription(1), outstandingAmount(1), Some(12.34), LocalDate.of(2018, 3, 29), None, interestRate(1), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount(1))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, Some(id1040000124) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate.head, dunningLock = dunningLock.head)))),
        FinancialDetail(taxYear, mainType(1), Some(id1040000125) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate(1), dunningLock = dunningLock(1)))))
      )
    )

  def testFinancialDetailsModelWithNoLpi(documentDescription: List[Option[String]],
                                            mainType: List[Option[String]],
                                            dueDate: List[Option[String]],
                                            outstandingAmount: List[Option[BigDecimal]],
                                            taxYear: String,
                                            interestOutstandingAmount: List[Option[BigDecimal]],
                                            interestRate: List[Option[BigDecimal]]): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
      documentDetails = List(
        DocumentDetail(taxYear, id1040000124, documentDescription.head, outstandingAmount.head, Some(43.21), LocalDate.of(2018, 3, 29), interestOutstandingAmount.head, interestRate.head, Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25"))),
        DocumentDetail(taxYear, id1040000125, documentDescription(1), outstandingAmount(1), Some(12.34), LocalDate.of(2018, 3, 29), interestOutstandingAmount(1), interestRate(1), Some(LocalDate.parse("2019-05-25")), Some(LocalDate.parse("2019-06-25")))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, Some(id1040000124) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), Some(id1040000125) , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def testFinancialDetailsModelWithChargesOfSameType(documentDescription: List[Option[String]],
                                                     mainType: List[Option[String]],
                                                     dueDate: List[Option[String]],
                                                     outstandingAmount: List[Option[BigDecimal]],
                                                     taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
      documentDetails = List(
        DocumentDetail(taxYear, id1040000123, documentDescription.head, outstandingAmount.head, Some(43.21), LocalDate.of(2018, 3, 29), Some(100),Some(100),
          Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),Some(100),Some("paymentLotItem"), Some("paymentLot")),
        DocumentDetail(taxYear, id1040000124, documentDescription(1), outstandingAmount(1), Some(12.34), LocalDate.of(2018, 3, 29), Some(100),Some(100),
          Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),Some(100),Some("paymentLotItem"), Some("paymentLot"))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, Some(id1040000123) ,Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), Some(id1040000124), Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def testFinancialDetailsModelOneItemInList(documentDescription: Option[String],
                                             mainType: Option[String],
                                             dueDate: Option[String],
                                             outstandingAmount: Option[BigDecimal],
                                             taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
      documentDetails = List(
        DocumentDetail(taxYear, id1040000124, documentDescription, outstandingAmount, Some(43.21), LocalDate.of(2018, 3, 29), Some(100),Some(100),
          Some(LocalDate.of(2018, 3, 29)),Some(LocalDate.of(2018, 3, 29)),Some(100),Some("paymentLotItem"), Some("paymentLot"))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType, Some(id1040000124), Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate))))
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


  def outstandingChargesModel(dueDate: String, aciAmount: BigDecimal = 12.67): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456.67, 1234), OutstandingChargeModel("ACI", None, aciAmount, 1234))
  )

  val outstandingChargesOverdueData: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().minusDays(30).toString)

  val outstandingChargesDueInMoreThan30Days: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().plusDays(35).toString)

  val outstandingChargesDueIn30Days: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().plusDays(30).toString)

  def financialDetailsDueInMoreThan30Days(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel = testFinancialDetailsModel(
    dueDate = dueDateMoreThan30Days,
    dunningLock = dunningLocks,
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00)
  )

  def financialDetailsDueIn30Days(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel = testFinancialDetailsModel(
    dueDate = dueDateDueIn30Days,
    dunningLock = dunningLocks,
    balanceDetails = BalanceDetails(1.00, 0.00, 1.00)
  )

  def financialDetailsOverdueData(dunningLocks: List[Option[String]] = noDunningLocks): FinancialDetailsModel = testFinancialDetailsModel(
    dueDate = dueDateOverdue,
    dunningLock = dunningLocks,
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00)
  )

  val financialDetailsBalancingCharges: FinancialDetailsModel = testFinancialDetailsModel(
    documentDescription = List(Some("TRM New Charge"), Some("TRM Amend Charge")),
    mainType = List(Some("SA Balancing Charge"), Some("SA Balancing Charge")),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks
  )

  val financialDetailsWithMixedData1: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = List(Some(LocalDate.now().plusDays(35).toString), Some(LocalDate.now().minusDays(1).toString)),
    outstandingAmount = List(Some(25), Some(50)),
    taxYear = LocalDate.now().getYear.toString
  )

  val financialDetailsWithMixedData2: FinancialDetailsModel = testFinancialDetailsModelWithChargesOfSameType(
    documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
    mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    dueDate = List(Some(LocalDate.now().plusDays(30).toString), Some(LocalDate.now().minusDays(1).toString)),
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString
  )

  def whatYouOweDataWithDataDueIn30Days(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 0.00, 1.00),
    dueInThirtyDaysList = financialDetailsDueIn30Days(dunningLocks).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesDueIn30Days)
  )

  val whatYouOweDataWithMixedData1: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(),
    futurePayments = List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates.head),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  def whatYouOweDataWithOverdueData(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 3.00, 3.00),
    overduePaymentList = financialDetailsOverdueData(dunningLocks).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )


  def whatYouOweDataWithDataDueInMoreThan30Days(dunningLocks: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 2.00, 2.00),
    futurePayments = financialDetailsDueInMoreThan30Days(dunningLocks).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days)
  )

  val whatYouOweDataWithMixedData2: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    overduePaymentList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates(1)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData2.getAllDocumentDetailsWithDueDates.head),
    futurePayments = List(),
    outstandingChargesModel = Some(OutstandingChargesModel(List()))
  )

  val whatYouOwePartialChargesList: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(balanceDueWithin30Days = 1.00, overDueAmount = 2.00, totalBalance = 3.00),
    overduePaymentList =
      testFinancialDetailsModelWithInterest(documentDescription = List(Some("ITSA- POA 1"), Some("ITSA - POA 2")),
        mainType = List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
        dueDate = dueDateOverdue,
        dunningLock = oneDunningLock,
        outstandingAmount = List(Some(50), Some(75)),
        taxYear = LocalDate.now().getYear.toString,
        interestOutstandingAmount = List(Some(42.50), Some(24.05)),
        interestRate = List(Some(2.6), Some(6.2)),
        latePaymentInterestAmount = List(Some(34.56), None)
      ).getAllDocumentDetailsWithDueDates,
    dueInThirtyDaysList =
      testFinancialDetailsModelOneItemInList(documentDescription = Some("ITSA - POA 2"),
        mainType = Some("SA Payment on Account 2"),
        dueDate = Some(LocalDate.now().plusDays(1).toString),
        outstandingAmount = Some(100),
        taxYear = LocalDate.now().getYear.toString).getAllDocumentDetailsWithDueDates,
    futurePayments =
      testFinancialDetailsModelOneItemInList(documentDescription = Some("ITSA- POA 1"),
        mainType = Some("SA Payment on Account 1"),
        dueDate = Some(LocalDate.now().plusDays(45).toString),
        outstandingAmount = Some(125),
        taxYear = LocalDate.now().getYear.toString).getAllDocumentDetailsWithDueDates,
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )
}
