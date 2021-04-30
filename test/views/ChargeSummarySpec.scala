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
import models.financialDetails.Charge
import org.jsoup.Jsoup
import testUtils.ViewSpec
import views.html.chargeSummary

class ChargeSummarySpec extends ViewSpec {

	class Setup(charge: Charge, paymentEnabled: Boolean = true) {
		val view = chargeSummary(charge, mockImplicitDateFormatter, paymentEnabled, "testBackURL")
		val document = Jsoup.parse(view.toString())
	}

	object Messages {
		def poaHeading(year: Int, number: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Payment on account $number of 2"
		def balancingChargeHeading(year: Int) =  s"Tax year 6 April ${year - 1} to 5 April $year Remaining balance"
		val paidToDate = "Paid to date"
	}

	"The charge summary view" should {

		"have the correct heading for a POA 1" in new Setup(chargeModel(mainType = Some("SA Payment on Account 1"))) {
			document.select("h1").text() shouldBe Messages.poaHeading(2018, 1)
		}

		"have the correct heading for a POA 2" in new Setup(chargeModel(mainType = Some("SA Payment on Account 2"))) {
			document.select("h1").text() shouldBe Messages.poaHeading(2018, 2)
		}

		"have the correct heading for a balancing charge" in new Setup(chargeModel(taxYear = 2019, mainType = Some("SA Balancing Charge"))) {
			document.select("h1").text() shouldBe Messages.balancingChargeHeading(2019)
		}

		"display a due date" in new Setup(chargeModel()) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(1) .govuk-summary-list__value")
				.text() shouldBe "OVERDUE 15 May 2019"
		}

		"display a charge amount" in new Setup(chargeModel(originalAmount = Some(1500))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(2) .govuk-summary-list__value")
				.text() shouldBe "£1,500.00"
		}

		"display a cleared amount if present" in new Setup(chargeModel(clearedAmount = Some(1500))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
				.text() shouldBe "£1,500.00"
		}

		"display a cleared amount of 0 if not present" in new Setup(chargeModel(clearedAmount = None)) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
				.text() shouldBe "£0.00"
		}

		"display a remaining amount if a cleared amount is present" in new Setup(chargeModel(outstandingAmount = Some(1600))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(4) .govuk-summary-list__value")
				.text() shouldBe "£1,600.00"
		}

		"display a remaining amount of 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new Setup(chargeModel(outstandingAmount = None, clearedAmount = Some(1400))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(4) .govuk-summary-list__value")
				.text() shouldBe "£0.00"
		}

		"display the original amount if no cleared or outstanding amount is present" in new Setup(chargeModel(outstandingAmount = None, clearedAmount = None, originalAmount = Some(1700))) {
			document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(4) .govuk-summary-list__value")
				.text() shouldBe "£1,700.00"
		}

		"have a payment link when an outstanding amount is to be paid and payments are enabled" in new Setup(chargeModel(), true) {
			document.select("div#payment-link-2018").text() shouldBe "Pay now"
		}

		"not have a payment link when an outstanding amount is to be paid but payments are disabled" in new Setup(chargeModel(), false) {
			document.select("div#payment-link-2018").text() shouldBe ""
		}

		"not have a payment link when there is no outstanding amount" in new Setup(chargeModel(outstandingAmount = None), true) {
			document.select("div#payment-link-2018").text() shouldBe ""
		}

		"not have a payment link when there is an outstanding amount of 0" in new Setup(chargeModel(outstandingAmount = Some(0)), true) {
			document.select("div#payment-link-2018").text() shouldBe ""
		}
	}
}
