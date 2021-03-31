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

package views.agent

import assets.FinancialDetailsTestConstants._
import config.featureswitch.FeatureSwitching
import models.financialDetails.Charge
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import testUtils.{TestSupport, ViewSpec}
import views.html.agent.ChargeSummary

class ChargeSummarySpec extends TestSupport with FeatureSwitching with ViewSpec {

  class Setup(charge: Charge, paymentEnabled: Boolean = true) {

    val chargeSummary: ChargeSummary = app.injector.instanceOf[ChargeSummary]

    val chargeSummaryView = chargeSummary(
      charge = charge,
      implicitDateFormatter = mockImplicitDateFormatter,
      paymentEnabled = paymentEnabled,
      backUrl = "testBackURL"
    )

    val document = Jsoup.parse(chargeSummaryView.toString())

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))
  }

  object Messages {
    def poaHeading(year: Int, number: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Payment on account $number of 2"

    def balancingChargeHeading(year: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Remaining balance"

    val paidToDate = "Paid to date"
  }

  "The agent charge summary view" should {

    "have the correct heading for a POA 1" in new Setup(chargeModel(mainType = Some("4920"))) {
      document.select("h1").text() shouldBe Messages.poaHeading(2018, 1)
    }

    "have the correct heading for a POA 2" in new Setup(chargeModel(mainType = Some("4930"))) {
      document.select("h1").text() shouldBe Messages.poaHeading(2018, 2)
    }

    "have the correct heading for a balancing charge" in new Setup(chargeModel(taxYear = 2019, mainType = Some("4910"))) {
      document.select("h1").text() shouldBe Messages.balancingChargeHeading(2019)
    }

    "display a due date" in new Setup(chargeModel()) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(1) .govuk-summary-list__value")
        .text() shouldBe "15 May 2019"
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

    "display a remaining amount of 0 if a cleared amount is present but an outstanding amount is not" in new Setup(chargeModel(outstandingAmount = None, clearedAmount = Some(600))) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(4) .govuk-summary-list__value")
        .text() shouldBe "£0.00"
    }

    "display the original amount if no cleared amount is present" in new Setup(chargeModel(clearedAmount = None, originalAmount = Some(1700))) {
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

    "has a link to view what you owe" in new Setup(chargeModel(mainType = Some("4920"))) {
      val link: Option[Elements] = getElementById("what-you-owe-link").map(_.select("a"))
      link.map(_.attr("href")) shouldBe Some("")
      ///report-quarterly/income-and-expenses/view/payments-owed (put link URL in " " above when what yoou owe page is ready))
      link.map(_.text) shouldBe Some("what you owe")
    }

    "has a back link" in new Setup(chargeModel(mainType = Some("4920"))) {
      document.backLink.text shouldBe "Back"
    }

    "has a pay now button display" in new Setup(chargeModel(outstandingAmount = Some(1600))) {
      document.select(".button").text() shouldBe "Pay now"
    }
  }

}
