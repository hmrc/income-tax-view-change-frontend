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

import assets.FinancialDetailsTestConstants._
import models.financialDetails.{DocumentDetail, FinancialDetail}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.chargeSummary

import java.time.LocalDate

class ChargeSummarySpec extends ViewSpec {

	class Setup(documentDetail: DocumentDetail, dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)), paymentEnabled: Boolean = true) {
		val view: Html = chargeSummary(documentDetail, dueDate, mockImplicitDateFormatter, paymentEnabled, "testBackURL")
		val document: Document = Jsoup.parse(view.toString())
	}

	object Messages {
		def poaHeading(year: Int, number: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Payment on account $number of 2"
		def balancingChargeHeading(year: Int) =  s"Tax year 6 April ${year - 1} to 5 April $year Remaining balance"
		val paidToDate = "Paid to date"
	}

	"The charge summary view" should {

		"have the correct heading for a POA 1" in new Setup(documentDetailModel(documentDescription = Some("ITSA- POA 1"))) {
			document.select("h1").text() shouldBe Messages.poaHeading(2018, 1)
		}

		"have the correct heading for a POA 2" in new Setup(documentDetailModel(documentDescription = Some("ITSA - POA 2"))) {
			document.select("h1").text() shouldBe Messages.poaHeading(2018, 2)
		}

		"have the correct heading for a balancing charge" in new Setup(documentDetailModel(taxYear = 2019, documentDescription = Some("ITSA- Bal Charge"))) {
			document.select("h1").text() shouldBe Messages.balancingChargeHeading(2019)
		}

		"display a due date" in new Setup(documentDetailModel()) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(1) .govuk-summary-list__value")
				.text() shouldBe "OVERDUE 15 May 2019"
		}

		"display a charge amount" in new Setup(documentDetailModel(originalAmount = Some(1500))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(2) .govuk-summary-list__value")
				.text() shouldBe "£1,500.00"
		}

		"display a remaining amount" in new Setup(documentDetailModel(outstandingAmount = Some(1600))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
				.text() shouldBe "£1,600.00"
		}

		"display a remaining amount of 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
				.text() shouldBe "£0.00"
		}

		"display the original amount if no cleared or outstanding amount is present" in new Setup(documentDetailModel(outstandingAmount = None, originalAmount = Some(1700))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
				.text() shouldBe "£1,700.00"
		}

		"have a payment link when an outstanding amount is to be paid and payments are enabled" in new Setup(documentDetailModel(), paymentEnabled = true) {
			document.select("div#payment-link-2018").text() shouldBe "Pay now"
		}

		"not have a payment link when an outstanding amount is to be paid but payments are disabled" in new Setup(documentDetailModel(), paymentEnabled = false) {
			document.select("div#payment-link-2018").text() shouldBe ""
		}

		"not have a payment link when there is an outstanding amount of 0" in new Setup(documentDetailModel(outstandingAmount = Some(0)), paymentEnabled = true) {
			document.select("div#payment-link-2018").text() shouldBe ""
		}
	}
}
