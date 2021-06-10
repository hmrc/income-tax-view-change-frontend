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
import models.financialDetails.DocumentDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import testUtils.ViewSpec
import java.time.LocalDate

import models.chargeHistory.ChargeHistoryModel
import views.html.chargeSummary

class ChargeSummarySpec extends ViewSpec {

	class Setup(documentDetail: DocumentDetail, dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)), chargeHistory: List[ChargeHistoryModel] = List()) {
		val view: Html = chargeSummary(documentDetail, dueDate, mockImplicitDateFormatter, "testBackURL", chargeHistory, chargeHistoryEnabled = true)
		val document: Document = Jsoup.parse(view.toString())
	}

	object Messages {
		def poaHeading(year: Int, number: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Payment on account $number of 2"
		def balancingChargeHeading(year: Int) =  s"Tax year 6 April ${year - 1} to 5 April $year Remaining balance"
		val paidToDate = "Paid to date"
		val chargeHistoryHeading = "Payment history"
		def paymentOnAccountCreated(number: Int) = s"Payment on account $number of 2 created"
		val balancingChargeCreated = "Remaining balance created"
		def paymentOnAccountAmended(number: Int) = s"Payment on account $number of 2 reduced due to amended return"
		val balancingChargeAmended = "Remaining balance reduced due to amended return"
		def paymentOnAccountRequest(number: Int) = s"Payment on account $number of 2 reduced by taxpayer request"
		val balancingChargeRequest = "Remaining balance reduced by taxpayer request"
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

		"have a payment link when an outstanding amount is to be paid" in new Setup(documentDetailModel()) {
			document.select("div#payment-link-2018").text() shouldBe "Pay now"
		}

		"not have a payment link when there is an outstanding amount of 0" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
			document.select("div#payment-link-2018").text() shouldBe ""
		}

		"display a charge history" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
			document.select("main h2").text shouldBe Messages.chargeHistoryHeading
		}

		"display only the charge creation item when no history found for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0))) {
			document.select("tbody tr").size() shouldBe 1
			document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
			document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.paymentOnAccountCreated(1)
			document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
		}

		"display only the charge creation item when no history found for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA - POA 2"))) {
			document.select("tbody tr").size() shouldBe 1
			document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.paymentOnAccountCreated(2)
		}

		"display only the charge creation item when no history found for a balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0), documentDescription = Some("ITSA- Bal Charge"))) {
			document.select("tbody tr").size() shouldBe 1
			document.select("tbody tr td:nth-child(2)").text() shouldBe Messages.balancingChargeCreated
		}

		"display the charge creation item when history is found" in new Setup(documentDetailModel(outstandingAmount = Some(0)), chargeHistory = List(ChargeHistoryModel("", "", "", "", 1500, "2018-07-06", "amended return"))) {
			document.select("tbody tr").size() shouldBe 2
			document.select("tbody tr:nth-child(1) td:nth-child(1)").text() shouldBe "29 Mar 2018"
			document.select("tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountCreated(1)
			document.select("tbody tr:nth-child(1) td:nth-child(3)").text() shouldBe "£1,400.00"
		}

		"display the correct message for an amended charge for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0)), chargeHistory = List(ChargeHistoryModel("", "", "", "", 1500, "2018-07-06", "amended return"))) {
			document.select("tbody tr").size() shouldBe 2
			document.select("tbody tr:nth-child(2) td:nth-child(1)").text() shouldBe "6 Jul 2018"
			document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountAmended(1)
			document.select("tbody tr:nth-child(2) td:nth-child(3)").text() shouldBe "£1,500.00"
		}

		"display the correct message for an amended charge for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0),documentDescription = Some("ITSA - POA 2")), chargeHistory = List(ChargeHistoryModel("", "", "", "", 1500, "2018-07-06", "amended return"))) {
			document.select("tbody tr").size() shouldBe 2
			document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountAmended(2)
		}

		"display the correct message for an amended charge for a balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0),documentDescription = Some("ITSA- Bal Charge")), chargeHistory = List(ChargeHistoryModel("", "", "", "", 1500, "2018-07-06", "amended return"))) {
			document.select("tbody tr").size() shouldBe 2
			document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.balancingChargeAmended
		}

		"display the correct message for a customer requested change for a payment on account 1 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0)), chargeHistory = List(ChargeHistoryModel("", "", "", "", 1500, "2018-07-06", "Customer Request"))) {
			document.select("tbody tr").size() shouldBe 2
			document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountRequest(1)
		}

		"display the correct message for a customer requested change for a payment on account 2 of 2" in new Setup(documentDetailModel(outstandingAmount = Some(0),documentDescription = Some("ITSA - POA 2")), chargeHistory = List(ChargeHistoryModel("", "", "", "", 1500, "2018-07-06", "Customer Request"))) {
			document.select("tbody tr").size() shouldBe 2
			document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.paymentOnAccountRequest(2)
		}

		"display the correct message for a customer requested change for a balancing charge" in new Setup(documentDetailModel(outstandingAmount = Some(0),documentDescription = Some("ITSA- Bal Charge")), chargeHistory = List(ChargeHistoryModel("", "", "", "", 1500, "2018-07-06", "Customer Request"))) {
			document.select("tbody tr").size() shouldBe 2
			document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe Messages.balancingChargeRequest
		}
	}
}
