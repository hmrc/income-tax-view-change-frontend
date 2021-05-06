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

import assets.BaseTestConstants._
import models.financialDetails._
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

object FinancialDetailsTestConstants {

  val testValidFinancialDetailsModelJson: JsValue = Json.obj(
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "transactionId" -> "1040000123",
        "documentDescription" -> "ITSA- Bal Charge",
        "outstandingAmount" -> 10.33,
        "originalAmount" -> 10.33
      ),
      Json.obj(
        "taxYear" -> "2020",
        "transactionId" -> "1040000124",
        "documentDescription" -> "ITSA- Bal Charge",
        "outstandingAmount" -> 10.34,
        "originalAmount" -> 10.34
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "mainType" -> "SA Balancing Charge",
        "items" -> Json.arr(
          Json.obj("dueDate" -> "2019-05-15")
        )
      ),
      Json.obj(
        "taxYear" -> "2020",
        "mainType" -> "SA Balancing Charge",
        "items" -> Json.arr(
          Json.obj("dueDate" -> "2019-05-18")
        )
      )
    )
  )


  def documentDetailModel(taxYear: Int = 2018,
                          documentDescription: Option[String] = Some("ITSA- POA 1"),
                          outstandingAmount: Option[BigDecimal] = Some(1400.00),
                          originalAmount: Option[BigDecimal] = Some(1400.00)): DocumentDetail =
    DocumentDetail(
      taxYear = taxYear.toString,
      transactionId = "1040000123",
      documentDescription,
      outstandingAmount = outstandingAmount,
      originalAmount = originalAmount
    )

  def financialDetail(taxYear: Int = 2018): FinancialDetail = FinancialDetail(
    taxYear = taxYear.toString,
    mainType = Some("SA Payment on Account 1"),
    items = Some(List(SubItem(dueDate = Some(LocalDate.of(2019, 5, 15).toString))))
  )

  def documentDetailWithDueDateModel(taxYear: Int = 2018,
                                     documentDescription: Option[String] = Some("ITSA- POA 1"),
                                     outstandingAmount: Option[BigDecimal] = Some(1400.00),
                                     originalAmount: Option[BigDecimal] = Some(1400.00),
                                     dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15))): DocumentDetailWithDueDate =
    DocumentDetailWithDueDate(documentDetailModel(taxYear, documentDescription, outstandingAmount, originalAmount), dueDate)

  val fullDocumentDetailModel: DocumentDetail = documentDetailModel()
  val fullFinancialDetailModel: FinancialDetail = financialDetail()

  val fullDocumentDetailWithDueDateModel: DocumentDetailWithDueDate = DocumentDetailWithDueDate(fullDocumentDetailModel, Some(LocalDate.of(2019, 5, 15)))

  def financialDetailsModel(taxYear: Int = 2018, outstandingAmount: Option[BigDecimal] = Some(1400.0)): FinancialDetailsModel =
    FinancialDetailsModel(
      documentDetails = List(documentDetailModel(taxYear, outstandingAmount = outstandingAmount)),
      financialDetails = List(financialDetail(taxYear))
    )

  val testValidFinancialDetailsModel: FinancialDetailsModel = FinancialDetailsModel(
    documentDetails = List(
      DocumentDetail("2019", "1040000123", Some("ITSA- Bal Charge"), Some(10.33), Some(10.33)),
      DocumentDetail("2020", "1040000124", Some("ITSA- Bal Charge"), Some(10.34), Some(10.34))
    ),
    financialDetails = List(
      FinancialDetail("2019", Some("SA Balancing Charge"), Some(Seq(SubItem(Some("2019-05-15"))))),
      FinancialDetail("2020", Some("SA Balancing Charge"), Some(Seq(SubItem(Some("2019-05-18")))))
    )
  )

  def testFinancialDetailsModel(documentDescription: List[Option[String]],
                                mainType: List[Option[String]],
                                dueDate: List[Option[String]],
                                outstandingAmount: List[Option[BigDecimal]],
                                taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      documentDetails = List(
        DocumentDetail(taxYear, "1040000124", documentDescription.head, outstandingAmount.head, Some(43.21)),
        DocumentDetail(taxYear, "1040000125", documentDescription(1), outstandingAmount(1), Some(12.34))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), Some(Seq(SubItem(dueDate(1)))))
      )
    )

  def testFinancialDetailsModelWithChargesOfSameType(documentDescription: List[Option[String]],
                                                     mainType: List[Option[String]],
                                                     dueDate: List[Option[String]],
                                                     outstandingAmount: List[Option[BigDecimal]],
                                                     taxYear: String): FinancialDetailsModel =
    FinancialDetailsModel(
      documentDetails = List(
        DocumentDetail(taxYear, "1040000123", documentDescription.head, outstandingAmount.head, Some(43.21)),
        DocumentDetail(taxYear, "1040000124", documentDescription(1), outstandingAmount(1), Some(12.34))
      ),
      financialDetails = List(
        FinancialDetail(taxYear, mainType.head, Some(Seq(SubItem(dueDate.head)))),
        FinancialDetail(taxYear, mainType(1), Some(Seq(SubItem(dueDate(1)))))
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

}
