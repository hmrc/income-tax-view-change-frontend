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
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails.DocumentDetailWithDueDate
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.Assertion
import play.twirl.api.Html
import testUtils.{TestSupport, ViewSpec}
import views.html.agent.ChargeSummary

import java.time.LocalDate

class ChargeSummaryViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  class Setup(documentDetailWithDueDate: DocumentDetailWithDueDate,
              chargeHistoryOpt: Option[List[ChargeHistoryModel]] = None) {

    val chargeSummary: ChargeSummary = app.injector.instanceOf[ChargeSummary]

    val chargeSummaryView: Html = chargeSummary(
      documentDetailWithDueDate = documentDetailWithDueDate,
      chargeHistoryOpt = chargeHistoryOpt,
      backUrl = "testBackURL"
    )

    val document: Document = Jsoup.parse(chargeSummaryView.toString())
    lazy val content: Element = document.selectHead("#content")

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def verifyChargesHistoryContent(rows: String*): Assertion = {
      content select Selectors.table text() shouldBe
        s"""
           |Date Description Amount
           |${rows.mkString("\n")}
           |""".stripMargin.trim.linesIterator.mkString(" ")
    }
  }

  object Messages {
    def poaHeading(year: Int, number: Int): String = s"Tax year 6 April ${year - 1} to 5 April $year Payment on account $number of 2"

    def balancingChargeHeading(year: Int): String = s"Tax year 6 April ${year - 1} to 5 April $year Remaining balance"

    val paidToDate = "Paid to date"
    val chargeHistoryHeading = "Payment history"
  }

  "The agent charge summary view" should {

    "have the correct heading for a POA 1" in new Setup(documentDetailPOA1) {
      document.select("h1").text shouldBe Messages.poaHeading(2018, 1)
    }

    "have the correct heading for a POA 2" in new Setup(documentDetailPOA2) {
      document.select("h1").text shouldBe Messages.poaHeading(2018, 2)
    }

    "have the correct heading for a balancing charge" in new Setup(documentDetailBalancingCharge) {
      document.select("h1").text shouldBe Messages.balancingChargeHeading(2018)
    }

    "display a due date" in new Setup(documentDetailWithDueDateModel()) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(1) .govuk-summary-list__value")
        .text shouldBe "OVERDUE 15 May 2019"
    }

    "display a charge amount" in new Setup(documentDetailWithDueDateModel(originalAmount = Some(1500))) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(2) .govuk-summary-list__value")
        .text shouldBe "£1,500.00"
    }

    "display a remaining amount" in new Setup(documentDetailWithDueDateModel(outstandingAmount = Some(1600))) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
        .text shouldBe "£1,600.00"
    }

    "display the original amount if no outstanding amount is present" in new Setup(documentDetailWithDueDateModel(outstandingAmount = None, originalAmount = Some(1700))) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
        .text shouldBe "£1,700.00"
    }

    "have a payment link when an outstanding amount is to be paid and payments are enabled" in new Setup(documentDetailWithDueDateModel()) {
      document.select("div#payment-link-2018").text() shouldBe "Pay now"
    }

    "not have a payment link when there is an outstanding amount of 0" in new Setup(documentDetailWithDueDateModel(outstandingAmount = Some(0))) {
      document.select("div#payment-link-2018").text() shouldBe ""
    }

    "has a link to view what you owe" in new Setup(documentDetailPOA1) {
      val link: Option[Elements] = getElementById("what-you-owe-link").map(_.select("a"))
      link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/payments-owed")
      link.map(_.text) shouldBe Some("what you owe")
    }

    "has a back link" in new Setup(documentDetailPOA1) {
      document.backLink.text shouldBe "Back"
    }

    "has a pay now button display" in new Setup(documentDetailWithDueDateModel(outstandingAmount = Some(1600))) {
      document.select(".button").text() shouldBe "Pay now"
    }


    "not display Payment (charge) history" when {
      "charge history is not provided" in new Setup(documentDetailPOA1, chargeHistoryOpt = None) {
        content doesNotHave Selectors.h2
        content doesNotHave Selectors.table
      }
    }

    "display Payment (charge) history" when {
      "charge history list is given" when {

        "the list is empty" should {
          "display the charge history heading" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(Nil)) {
            content select Selectors.h2 text() shouldBe Messages.chargeHistoryHeading
          }

          "display the Charge creation time (Document date), Name of charge type and Amount of the charge type" when {
            "a payment on account 1 of 2" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(Nil)) {
              verifyChargesHistoryContent("29 Mar 2018 Payment on account 1 of 2 created £1,400.00")
            }
            "a payment on account 2 of 2" in new Setup(documentDetailPOA2, chargeHistoryOpt = Some(Nil)) {
              verifyChargesHistoryContent("29 Mar 2018 Payment on account 2 of 2 created £1,400.00")
            }
            "balancing charge" in new Setup(documentDetailBalancingCharge, chargeHistoryOpt = Some(Nil)) {
              verifyChargesHistoryContent("29 Mar 2018 Remaining balance created £1,400.00")
            }
          }
        }

        "the list contains records" should {
          val fullChargeHistory = List(
            ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 12345, LocalDate.of(2018, 7, 6), "amended return"),
            ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 54321, LocalDate.of(2019, 8, 12), "Customer Request")
          )

          "display the charge history heading" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(fullChargeHistory)) {
            content select Selectors.h2 text() shouldBe Messages.chargeHistoryHeading
          }

          "display the list of amendments with Charge creation as first line" when {
            "a payment on account 1 of 2" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(fullChargeHistory)) {
              verifyChargesHistoryContent(
                "29 Mar 2018 Payment on account 1 of 2 created £1,400.00",
                "6 Jul 2018 Payment on account 1 of 2 reduced due to amended return £12,345.00",
                "12 Aug 2019 Payment on account 1 of 2 reduced by taxpayer request £54,321.00")
            }
            "a payment on account 2 of 2" in new Setup(documentDetailPOA2, chargeHistoryOpt = Some(fullChargeHistory)) {
              verifyChargesHistoryContent(
                "29 Mar 2018 Payment on account 2 of 2 created £1,400.00",
                "6 Jul 2018 Payment on account 2 of 2 reduced due to amended return £12,345.00",
                "12 Aug 2019 Payment on account 2 of 2 reduced by taxpayer request £54,321.00")
            }
            "balancing charge" in new Setup(documentDetailAmendedBalCharge, chargeHistoryOpt = Some(fullChargeHistory)) {
              verifyChargesHistoryContent(
                "29 Mar 2018 Remaining balance created £1,400.00",
                "6 Jul 2018 Remaining balance reduced due to amended return £12,345.00",
                "12 Aug 2019 Remaining balance reduced by taxpayer request £54,321.00")
            }
          }
        }

      }
    }
  }

}
