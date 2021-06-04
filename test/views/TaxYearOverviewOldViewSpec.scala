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

package views

import java.time.LocalDate

import assets.MessagesLookUp.TaxYearOverview
import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi}
import implicits.ImplicitCurrencyFormatter._
import models.calculation.CalcOverview
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import models.financialTransactions.TransactionModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.taxYearOverviewOld

class TaxYearOverviewOldViewSpec extends TestSupport with FeatureSwitching {

	val testYear: Int = 2020
	val completeOverview: CalcOverview = CalcOverview(
		timestamp = Some("2020-01-01T00:35:34.185Z"),
		income = 1.01,
		deductions = 2.02,
		totalTaxableIncome = 3.03,
		taxDue = 4.04,
		payment = 5.05,
		totalRemainingDue = 6.06,
		crystallised = false
	)

	val transactionModel: TransactionModel = TransactionModel(
		clearedAmount = Some(7.07),
		outstandingAmount = Some(8.08)
	)

	val chargeModel: DocumentDetailWithDueDate = DocumentDetailWithDueDate(
		documentDetail = DocumentDetail(
			taxYear = "2018",
			transactionId = "transactionId",
			documentDescription = Some("ITSA- POA 1"),
			outstandingAmount = Some(8.08),
			originalAmount = Some(100.00),
			documentDate = "2018-03-29"
		),
		dueDate = Some(LocalDate.now)
	)

	class Setup(taxYear: Int = testYear,
							overview: CalcOverview = completeOverview,
							transaction: Option[TransactionModel] = Some(transactionModel),
							charge: Option[DocumentDetailWithDueDate] = Some(chargeModel)) {

		val page: Html = taxYearOverviewOld(taxYear, overview, transaction, charge, mockImplicitDateFormatter, "testBackURL")
		val document: Document = Jsoup.parse(page.body)
		val content: Element = document.selectFirst("#content")

		def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)

		def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

		def getElementByCss(selector: String): Option[Element] = Option(document.select(selector).first())
	}

	"taxYearOverviewOld with NewFinancialDetailsApi disabled" must {
		disable(NewFinancialDetailsApi)
		"have the correct title" in new Setup {
			document.title shouldBe TaxYearOverview.title(testYear - 1, testYear)
		}

		"have the correct heading" in new Setup(taxYear = testYear) {
			content.select("h1").text shouldBe TaxYearOverview.heading(testYear - 1, testYear)
		}

		"have a status of the tax year" in new Setup {
			val status: Elements = content.select("#tax-year-status")
			status.text shouldBe TaxYearOverview.status("ONGOING")
			status.select("span").hasClass("govUk-tag") shouldBe true
		}


		"have a table of income and deductions" which {

			"has a row and link to view income updates" in new Setup() {

				val link: Option[Element] =
					getElementByCss("#income-deductions-table > tbody > tr:nth-child(1) > td:nth-child(1) > a")
				link.map(_.attr("href")) shouldBe Some(controllers.routes.IncomeSummaryController.showIncomeSummary(testYear).url)
				link.map(_.text) shouldBe Some(TaxYearOverview.income)
			}
		 "has a row and link to view deductions updates" in new Setup {

				val link: Option[Element] =
					getElementByCss("#income-deductions-table > tbody > tr:nth-child(2) > td:nth-child(1) > a")
				link.map(_.text) shouldBe Some(TaxYearOverview.deductions)
				link.map(_.attr("href")) shouldBe Some(controllers.routes.DeductionsSummaryController.showDeductionsSummary(testYear).url)
			}
			"has a total row for total taxable income" in new Setup {
				val row: Elements = content.select("#income-deductions-table tr:nth-child(3)")
				row.select("td:nth-child(1)").text shouldBe TaxYearOverview.taxableIncome
				row.select("td:nth-child(2)").text shouldBe completeOverview.totalTaxableIncome.toCurrencyString
			}
		}

		"have a table for tax due and payments" which {
			"has a row for tax due" in new Setup {
				val row: Elements = content.select("#taxdue-payments-table tr:nth-child(1)")
				row.select("td:nth-child(1)").text shouldBe TaxYearOverview.taxDue
				row.select("td:nth-child(2)").text shouldBe completeOverview.taxDue.toCurrencyString
			}
		}

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

	"taxYearOverview with NewFinancialDetailsApi FS enabled" must {
		enable(NewFinancialDetailsApi)
		"have the correct title" in new Setup {
			document.title shouldBe TaxYearOverview.title(testYear - 1, testYear)
		}

		"have the correct heading" in new Setup(taxYear = testYear) {
			content.select("h1").text shouldBe TaxYearOverview.heading(testYear - 1, testYear)
		}

		"have a status of the tax year" in new Setup {
			val status: Elements = content.select("#tax-year-status")
			status.text shouldBe TaxYearOverview.status("ONGOING")
			status.select("span").hasClass("govUk-tag") shouldBe true
		}


		"have a table of income and deductions" which {
			"has a row and link to view income updates" in new Setup() {

				val link: Option[Element] =
					getElementByCss("#income-deductions-table > tbody > tr:nth-child(1) > td:nth-child(1) > a")
				link.map(_.attr("href")) shouldBe Some(controllers.routes.IncomeSummaryController.showIncomeSummary(testYear).url)
				link.map(_.text) shouldBe Some(TaxYearOverview.income)
			}
			"has a row and link to view deductions updates" in new Setup {

				val link: Option[Element] =
					getElementByCss("#income-deductions-table > tbody > tr:nth-child(2) > td:nth-child(1) > a")
				link.map(_.text) shouldBe Some(TaxYearOverview.deductions)
				link.map(_.attr("href")) shouldBe Some(controllers.routes.DeductionsSummaryController.showDeductionsSummary(testYear).url)
			}
			"has a total row for total taxable income" in new Setup {
				val row: Elements = content.select("#income-deductions-table tr:nth-child(3)")
				row.select("td:nth-child(1)").text shouldBe TaxYearOverview.taxableIncome
				row.select("td:nth-child(2)").text shouldBe completeOverview.totalTaxableIncome.toCurrencyString
			}
		}

		"have a table for tax due and payments" which {
			"has a row for tax due" in new Setup {
				val row: Elements = content.select("#taxdue-payments-table tr:nth-child(1)")
				row.select("td:nth-child(1)").text shouldBe TaxYearOverview.taxDue
				row.select("td:nth-child(2)").text shouldBe completeOverview.taxDue.toCurrencyString
			}
		}

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

		disable(NewFinancialDetailsApi)
	}

}
