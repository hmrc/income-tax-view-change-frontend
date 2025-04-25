/*
 * Copyright 2023 HM Revenue & Customs
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

package views.claimToAdjustPoa

import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.test.Helpers.contentAsString
import testUtils.TestSupport
import views.html.claimToAdjustPoa.AmendablePaymentOnAccount

class AmendablePoaControllerViewSpec extends TestSupport {

  class Setup(isAgent: Boolean, poAFullyPaid: Boolean = false, poasHaveBeenAdjustedPreviously: Option[Boolean] = None) {

    val amendablePaymentOnAccount: AmendablePaymentOnAccount = app.injector.instanceOf[AmendablePaymentOnAccount]

    val document: Document =
      Jsoup.parse(
        contentAsString(
          amendablePaymentOnAccount(
            isAgent = isAgent,
            viewModel =
              PaymentOnAccountViewModel(
                poaOneTransactionId = "poa-one-id",
                poaTwoTransactionId = "poa-two-id",
                taxYear = TaxYear.makeTaxYearWithEndYear(2024),
                totalAmountOne = BigDecimal(3000.45),
                totalAmountTwo = BigDecimal(3000.45),
                relevantAmountOne = BigDecimal(5000.50),
                relevantAmountTwo = BigDecimal(5000.50),
                partiallyPaid = false,
                fullyPaid = poAFullyPaid,
                previouslyAdjusted = poasHaveBeenAdjustedPreviously
              ),
          )
        )
      )
  }

  def executeTest(isAgent: Boolean): Unit = {
    s"${if (isAgent) "Agent" else "Individual"}: AmendablePaymentOnAccountView" should {
      "render the heading" in new Setup(isAgent) {
        document.getElementsByClass("govuk-caption-xl").first().ownText() shouldBe messages("paymentOnAccount.caption", "2023", "2024")
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe messages("paymentOnAccount.heading")
      }
      "render the first paragraph text" in new Setup(isAgent) {
        document.getElementById("paragraph-1-text").text() shouldBe (
          messages("paymentOnAccount.p1") + " " +
            messages("paymentOnAccount.class4NationalInsurance.link.text") + " " +
            messages("paymentOnAccount.p2")
          )
        document.getElementById("paragraph-1-text").getElementsByTag("a").attr("href") shouldBe messages("paymentOnAccount.class4NationalInsurance.link")
      }
      "render the example heading and content" in new Setup(isAgent) {
        document.getElementById("heading-example").text() shouldBe messages("paymentOnAccount.heading.example")
        document.getElementById("hint").text() shouldBe messages("paymentOnAccount.hint")
        document.getElementsByClass("govuk-body").first().getElementsByTag("a").attr("href") shouldBe messages("paymentOnAccount.class4NationalInsurance.link")
      }
      "render the Payment On Account Table" in new Setup(isAgent) {
        document.getElementsByClass("govuk-table__head").text() shouldBe messages("paymentOnAccount.table-heading-charge-type") +
          " " + messages("paymentOnAccount.table-heading-created-amount.key")
        val tableBody = document.getElementsByClass("govuk-table__body")
        tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__header:nth-of-type(1)").text shouldBe messages("paymentOnAccount.table-heading-1")
        tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__cell:nth-of-type(1)").text shouldBe "£3,000.45"
        tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__header:nth-of-type(1)").text shouldBe messages("paymentOnAccount.table-heading-2")
        tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__cell:nth-of-type(1)").text shouldBe "£3,000.45"
      }
      "render the Payment On Account Table with relevant amount when POA previously adjusted" in new Setup(isAgent, poasHaveBeenAdjustedPreviously = Some(true)) {
        document.getElementsByClass("govuk-table__head").text() shouldBe messages("paymentOnAccount.table-heading-charge-type") +
          " " + messages("paymentOnAccount.table-heading-created-amount.key") +
          " " + messages("paymentOnAccount.table-heading-adjusted-amount.key")
        val tableBody = document.getElementsByClass("govuk-table__body")
        tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__header:nth-of-type(1)").text shouldBe messages("paymentOnAccount.table-heading-1")
        tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__cell:nth-of-type(1)").text shouldBe "£5,000.50"
        tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__cell:nth-of-type(2)").text shouldBe "£3,000.45"
        tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__header:nth-of-type(1)").text shouldBe messages("paymentOnAccount.table-heading-2")
        tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__cell:nth-of-type(1)").text shouldBe "£5,000.50"
        tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__cell:nth-of-type(2)").text shouldBe "£3,000.45"
      }
      "render the Adjust my payments on account button" in new Setup(isAgent) {
        document.getElementById("adjust-my-payments-button").text() shouldBe messages("paymentOnAccount.button")
        document.getElementById("adjust-my-payments-button").getElementsByTag("a").attr("href") shouldBe getWhatYouNeedToKnowUrl(isAgent)
      }
      "render the Cancel link" in new Setup(isAgent) {
        document.getElementById("cancel-link").text() shouldBe messages("paymentOnAccount.cancel.link")
        document.getElementById("cancel-link").getElementsByTag("a").attr("href") shouldBe getCancelLinkUrl(isAgent)
      }
    }
  }

  def getChargeSummaryUrl(isAgent: Boolean, id: String): String = {
    if (isAgent) controllers.routes.ChargeSummaryController.showAgent(2024, id)
    else         controllers.routes.ChargeSummaryController.show(2024, id)
  }.url

  def getWhatYouNeedToKnowUrl(isAgent: Boolean): String =
    controllers.claimToAdjustPoa.routes.WhatYouNeedToKnowController.show(isAgent).url

  def getCancelLinkUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.routes.HomeController.showAgent()
    else         controllers.routes.HomeController.show()
  }.url

  executeTest(isAgent = true)
  executeTest(isAgent = false)
}
