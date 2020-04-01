/*
 * Copyright 2020 HM Revenue & Customs
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

package views

import assets.Messages.{Breadcrumbs, TaxYearOverview}
import models.calculation.CalcOverview
import models.financialTransactions.TransactionModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.i18n.Messages.Implicits.applicationMessages
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.taxYearOverview

import implicits.ImplicitCurrencyFormatter._

class TaxYearOverviewViewSpec extends TestSupport {

  val testYear: Int = 2020
  val completeOverview: CalcOverview = CalcOverview(
    timestamp = Some("2020-01-01T00:35:34.185Z"),
    income = 1.01,
    deductions = 2.02,
    totalTaxableIncome = 3.03,
    taxDue = 4.04,
    payment = 5.05,
    totalRemainingDue = 6.06
  )

  val transactionModel: TransactionModel = TransactionModel(
    clearedAmount = Some(7.07),
    outstandingAmount = Some(8.08)
  )

  class Setup(taxYear: Int = testYear,
              overview: CalcOverview = completeOverview,
              transaction: Option[TransactionModel] = Some(transactionModel)) {

    val page: Html = taxYearOverview(taxYear, overview, transaction)
    val document: Document = Jsoup.parse(page.body)
    val content: Element = document.selectFirst("#content")

  }

  "taxYearOverview" must {
    "have the correct title" in new Setup {
      document.title shouldBe TaxYearOverview.title
    }

    "have a breadcrumb trail" in new Setup(taxYear = testYear) {
      content.select("#breadcrumb-bta").text shouldBe Breadcrumbs.bta
      content.select("#breadcrumb-bta").attr("href") shouldBe appConfig.businessTaxAccount
      content.select("#breadcrumb-it").text shouldBe Breadcrumbs.it
      content.select("#breadcrumb-it").attr("href") shouldBe controllers.routes.HomeController.home().url
      content.select("#breadcrumb-tax-years").text shouldBe Breadcrumbs.taxYears
      content.select("#breadcrumb-tax-years").attr("href") shouldBe controllers.routes.TaxYearsController.viewTaxYears().url
      content.select("#breadcrumb-tax-year-overview").text shouldBe Breadcrumbs.taxYearOverview(testYear - 1, testYear)
      content.select("#breadcrumb-tax-year-overview").hasAttr("href") shouldBe false
    }

    "have the correct heading" in new Setup(taxYear = testYear) {
      content.select("h1").text shouldBe TaxYearOverview.heading(testYear - 1, testYear)
    }

    "have a status of the tax year" in new Setup {
      val status: Elements = content.select("#tax-year-status")
      status.text shouldBe TaxYearOverview.status("ONGOING")
      status.select("span").hasClass("govUk-tag") shouldBe true
    }

    "have a summary of the tables on the page" in new Setup {
      content.select("#tax-year-summary").text shouldBe TaxYearOverview.summary
    }

    "have a table of income and deductions" which {
      "has a row for income" in new Setup {
        val row: Elements = content.select("#income-deductions-table tr:nth-child(1)")
        row.select("td:nth-child(1)").text shouldBe TaxYearOverview.income
        row.select("td[class=numeric]").text shouldBe completeOverview.income.toCurrencyString
      }
      "has a row for deductions" in new Setup {
        val row: Elements = content.select("#income-deductions-table tr:nth-child(2)")
        row.select("td:nth-child(1)").text shouldBe TaxYearOverview.deductions
        row.select("td[class=numeric]").text shouldBe completeOverview.deductions.toNegativeCurrencyString
      }
      "has a total row for total taxable income" in new Setup {
        val row: Elements = content.select("#income-deductions-table tr:nth-child(3)")
        row.select("td:nth-child(1)").text shouldBe TaxYearOverview.taxableIncome
        row.select("td[class=numeric]").text shouldBe completeOverview.totalTaxableIncome.toCurrencyString
      }
    }

    "have a table for tax due and payments" which {
      "has a row for tax due" in new Setup {
        val row: Elements = content.select("#taxdue-payments-table tr:nth-child(1)")
        row.select("td:nth-child(1)").text shouldBe TaxYearOverview.taxDue
        row.select("td[class=numeric]").text shouldBe completeOverview.taxDue.toCurrencyString
      }
      "has a row for payments" in new Setup {
        val row: Elements = content.select("#taxdue-payments-table tr:nth-child(2)")
        row.select("td:nth-child(1)").text shouldBe TaxYearOverview.payment
        row.select("td[class=numeric]").text shouldBe completeOverview.payment.toNegativeCurrencyString
      }
      "has a row for total remaining due" in new Setup {
        val row: Elements = content.select("#taxdue-payments-table tr:nth-child(3)")
        row.select("td:nth-child(1)").text shouldBe TaxYearOverview.totalRemaining
        row.select("td[class=numeric]").text shouldBe completeOverview.totalRemainingDue.toCurrencyString
      }
    }
  }

  "taxYearOverview" when {
    "a timestamp is present in the calculation" must {
      "display the date of the calculation" in new Setup {
        content.select("#calculation-date").text shouldBe TaxYearOverview.calculationDate("1 January 2020")
      }
    }

    "a timestamp is not present in the calculation" must {
      "not show any calculation date" in new Setup(overview = completeOverview.copy(timestamp = None)) {
        content.select("#calculation-date").isEmpty shouldBe true
      }
    }
  }

}
