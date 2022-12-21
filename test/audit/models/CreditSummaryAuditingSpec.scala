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

package audit.models

import models.creditDetailModel.{CreditDetailModel, CutOverCreditType, MfaCreditType}
import models.financialDetails.DocumentDetail
import play.api.i18n.MessagesApi
import testUtils.TestSupport

import java.time.LocalDate

class CreditSummaryAuditingSpec extends TestSupport {

  import CreditSummaryAuditing._

  implicit val msgApi: MessagesApi = messagesApi

  val creditDetailsModelPaid = CreditDetailModel(
    date = LocalDate.of(2018, 1, 2),
    documentDetail = DocumentDetail(
      taxYear = "2018",
      transactionId = "1001",
      documentDescription = None,
      documentText = None,
      outstandingAmount = Some(BigDecimal("0")),
      originalAmount = None,
      documentDate = LocalDate.of(2018, 1, 2)
    ),
    creditType = MfaCreditType,
    balanceDetails = None
  )

  val creditDetailsModelPartiallyPaid = creditDetailsModelPaid
    .copy(documentDetail = creditDetailsModelPaid.documentDetail
      .copy(outstandingAmount = Some(BigDecimal("-150.00"))),
      date = LocalDate.of(2019, 11, 12)
    )

  val creditDetailsModelUnPaid = creditDetailsModelPaid
    .copy(documentDetail = creditDetailsModelPaid.documentDetail
      .copy(
        originalAmount = Some(BigDecimal("1.5")),
        outstandingAmount = Some(BigDecimal("1.5"))
      ),
      creditType = CutOverCreditType,
      date = LocalDate.of(2021, 3, 7)
    )


  "CreditSummaryDetails conversion" should {
    "- paid credit" in {
      val creditSummaryDetails: CreditSummaryDetails = creditDetailsModelPaid
      creditSummaryDetails.date shouldBe "2018-01-02"
      creditSummaryDetails.description shouldBe "Credit from HMRC adjustment"
      creditSummaryDetails.status shouldBe "Fully allocated"
    }

    "- partially paid credit" in {
      val creditSummaryDetails: CreditSummaryDetails = creditDetailsModelPartiallyPaid
      creditSummaryDetails.date shouldBe "2019-11-12"
      creditSummaryDetails.description shouldBe "Credit from HMRC adjustment"
      creditSummaryDetails.status shouldBe "Partially allocated"
    }

    "- unpaid credit" in {
      val creditSummaryDetails: CreditSummaryDetails = creditDetailsModelUnPaid
      creditSummaryDetails.date shouldBe "2021-03-07"
      creditSummaryDetails.description shouldBe "Credit from an earlier tax year"
      creditSummaryDetails.status shouldBe "Not allocated"
    }

  }

}
