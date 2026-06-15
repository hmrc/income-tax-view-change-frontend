/*
 * Copyright 2026 HM Revenue & Customs
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

package models.repaymentHistory

import common.models.incomeSourceDetails.TaxYear
import common.services.DateService
import common.testUtils.TestSupport
import models.financialDetails.PaymentType
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate

class PaymentHistoryEntrySpec extends TestSupport with Matchers {

  override implicit val dateService: DateService = app.injector.instanceOf[DateService]

  private val dateInTaxYear26 = LocalDate.parse("2025-07-10")
  private val dateInTaxYear25 = LocalDate.parse("2025-02-01")

  ".getTaxYear" should {
    "return the tax-year of the date" when {
      "an invalid tax-year is defined (9999)" in {
        val testPaymentHistoryEntry = PaymentHistoryEntry(
          date = dateInTaxYear26,
          creditType = PaymentType,
          amount = None,
          transactionId = None,
          linkUrl = "link-url",
          visuallyHiddenText = "hidden-text",
          taxYear = Some(9999)
        )

        testPaymentHistoryEntry.getTaxYear shouldBe TaxYear.forYearEnd(2026)
      }

      "no tax-year is defined" in {
        val testPaymentHistoryEntry = PaymentHistoryEntry(
          date = dateInTaxYear25,
          creditType = PaymentType,
          amount = None,
          transactionId = None,
          linkUrl = "link-url",
          visuallyHiddenText = "hidden-text",
          taxYear = None
        )

        testPaymentHistoryEntry.getTaxYear shouldBe TaxYear.forYearEnd(2025)
      }
    }
    "return the value of taxYear" when {
      "a valid tax-year is defined" in {
        val testPaymentHistoryEntry = PaymentHistoryEntry(
          date = dateInTaxYear25,
          creditType = PaymentType,
          amount = None,
          transactionId = None,
          linkUrl = "link-url",
          visuallyHiddenText = "hidden-text",
          taxYear = Some(2026)
        )

        testPaymentHistoryEntry.getTaxYear shouldBe TaxYear.forYearEnd(2026)
      }
    }
  }
}

