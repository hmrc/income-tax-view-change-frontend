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

package financials.views.partials.paymentAllocations.creditAndRefunds

import common.models.incomeSourceDetails.TaxYear
import common.testUtils.TestSupport
import financials.models.creditsandrefunds.*
import financials.models.{CutOverCreditType, MfaCreditType}
import financials.views.html.partials.creditAndRefunds.WhereMoneyCameFromTable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import shared.implicits.ImplicitCurrencyFormatter
import shared.implicits.ImplicitCurrencyFormatter.CurrencyFormatter

import java.time.LocalDate


class WhereMoneyCameFromTableSpec extends TestSupport {

  lazy val whereTheMoneyCameFromTable: WhereMoneyCameFromTable = app.injector.instanceOf[WhereMoneyCameFromTable]

  val taxYear: TaxYear = TaxYear(startYear = 2023, endYear = 2024)

  def viewModelWith(row: CreditRow): MoneyInYourAccountViewModel = MoneyInYourAccountViewModel(
    availableCredit = BigDecimal(150.00),
    allocatedCredit = BigDecimal(0),
    unallocatedCredit = BigDecimal(150.00),
    totalCredit = BigDecimal(150.00),
    firstPendingAmountRequested = None,
    secondPendingAmountRequested = None,
    creditRows = List(row),
    checkRefundStatusLink = "/refund-status"
  )


  class Setup(row: CreditRow, isAgent: Boolean = false) {
    val testUser: common.auth.MtdItUser[?] = if (isAgent) agentUserConfirmedClient() else individualUser

    val html: HtmlFormat.Appendable =
      whereTheMoneyCameFromTable(viewModelWith(row))(fakeRequestWithActiveSession, testUser, messages)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
    val firstRow = pageDocument.select("tbody tr").first()
  }

  "WhereTheMoneyCameFromTable" when {

    "given a payment row" should {
      val paymentRow = PaymentCreditRow(
        transactionId = "payment-transaction-id",
        amount = BigDecimal(100.00),
        date = LocalDate.of(2024, 1, 31),
        effectiveDate = LocalDate.of(2024, 1, 29)
      )

      "render the due date, description link, tax year and amount" in new Setup(paymentRow) {
        firstRow.select("td").get(0).text() shouldBe "31 Jan 2024"

        val link = firstRow.select("a#where-the-money-came-from-link-0")
        link.text() shouldBe "Payment you made to HMRC on 29 Jan 2024"
        link.attr("href") should include("payment-made-to-hmrc")

        firstRow.select("td#tax-year-cell-0").text() shouldBe s"2023 to 2024"
        firstRow.select("td").last().text() shouldBe CurrencyFormatter(paymentRow.amount).toCurrencyString
      }

      "render the agent description link when the user is an agent" in new Setup(paymentRow, isAgent = true) {
        firstRow.select("a#where-the-money-came-from-link-0").attr("href") shouldBe paymentRow.descriptionLink(isAgent = true)
      }
    }

    "given an ordinary credit row" should {
      val creditRow = CreditViewRow(
        transactionId = "credit-transaction-id",
        amount = BigDecimal(50.00),
        creditType = MfaCreditType,
        taxYear = taxYear,
        date = LocalDate.of(2024, 2, 15),
        isRevenueAmendment = false
      )

      "render the date, credit-row description link, tax year and amount" in new Setup(creditRow) {
        firstRow.select("td").get(0).text() shouldBe "15 Feb 2024"

        val link = firstRow.select("a#where-the-money-came-from-link-0")
        link.text() shouldBe "Credit from an earlier tax year"
        link.attr("href") should include("credits-from-hmrc")

        firstRow.select("td#tax-year-cell-0").text() shouldBe s"2023 to 2024"
        firstRow.select("td").last().text() shouldBe CurrencyFormatter(creditRow.amount).toCurrencyString
      }
    }

    "given a revenue amendment credit row" should {
      val revenueAmendmentCreditRow = CreditViewRow(
        transactionId = "ra-credit-transaction-id",
        amount = BigDecimal(75.00),
        creditType = CutOverCreditType,
        taxYear = taxYear,
        date = LocalDate.of(2024, 3, 10),
        isRevenueAmendment = true
      )

      "render the date, ra-credit-row description link, tax year and amount" in new Setup(revenueAmendmentCreditRow) {
        firstRow.select("td").get(0).text() shouldBe "10 Mar 2024"

        val link = firstRow.select("a#where-the-money-came-from-link-0")
        link.text() shouldBe "Credit from HMRC enquiry amendment"
        link.attr("href") shouldBe revenueAmendmentCreditRow.descriptionLink(isAgent = false)

        firstRow.select("td#tax-year-cell-0").text() shouldBe s"2023 to 2024"
        firstRow.select("td").last().text() shouldBe CurrencyFormatter(revenueAmendmentCreditRow.amount).toCurrencyString
      }
    }

    "given a refund row" should {
      val refundRow = RefundRow(
        amount = BigDecimal(25.00),
        date = LocalDate.of(2024, 4, 1)
      )

      "render 'no data' for date and tax year, the refund-row description link, and a negative amount" in new Setup(refundRow) {
        firstRow.select("td").get(0).text() shouldBe messages("chargeSummary.noData")

        val link = firstRow.select("a#where-the-money-came-from-link-0")
        link.text() shouldBe messages("money-in-your-account.where-from.refund-row.description")
        link.attr("href") shouldBe refundRow.descriptionLink

        firstRow.select("td#tax-year-cell-0").text() shouldBe messages("chargeSummary.noData")
        firstRow.select("td").last().text() shouldBe s"−${CurrencyFormatter(refundRow.amount).toCurrencyString}"
      }
    }
  }
}