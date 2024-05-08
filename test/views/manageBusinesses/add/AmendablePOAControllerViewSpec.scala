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

package views.manageBusinesses.add

import models.incomeSourceDetails.TaxYear
import models.claimToAdjustPoa.PaymentOnAccountViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.test.Helpers.contentAsString
import testUtils.TestSupport
import views.html.claimToAdjustPoa.AmendablePaymentOnAccount

class AmendablePOAControllerViewSpec extends TestSupport {

  class Setup(isAgent: Boolean) {

    val amendablePaymentOnAccount: AmendablePaymentOnAccount = app.injector.instanceOf[AmendablePaymentOnAccount]

    val document: Document =
      Jsoup.parse(
        contentAsString(
          amendablePaymentOnAccount(
            isAgent = isAgent,
            paymentOnAccount =
              PaymentOnAccountViewModel(
                poaOneTransactionId = "poa-one-id",
                poaTwoTransactionId = "poa-two-id",
                taxYear = TaxYear(2024),
                paymentOnAccountOne = BigDecimal(5000),
                paymentOnAccountTwo = BigDecimal(5000),
                poARelevantAmountOne = BigDecimal(5000),
                poARelevantAmountTwo = BigDecimal(5000)
              )
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
      "render the hint" in new Setup(isAgent) {
        document.getElementsByClass("govuk-inset-text").text() shouldBe messages("paymentOnAccount.inset-text")
        document.getElementsByClass("govuk-body").first().getElementsByTag("a").attr("href") shouldBe messages("paymentOnAccount.class4NationalInsurance.link")
      }
      "render the first Payment On Account Summary Card" in new Setup(isAgent) {
        document.getElementsByClass("govuk-summary-card__title").get(0).text() shouldBe messages("paymentOnAccount.table-heading-1")
        document.getElementById("poa1-more-details-date-link").text() shouldBe messages("paymentOnAccount.table-heading.link")
        document.getElementById("poa1-more-details-date-link").getElementsByTag("a").attr("href") shouldBe getChargeSummaryUrl(isAgent, "poa-one-id")
        document.getElementsByClass("govuk-summary-list__key").get(0).text() shouldBe messages("paymentOnAccount.table-heading.key")
        document.getElementsByClass("govuk-summary-list__value govuk-summary-list__value govuk-table__cell--numeric").get(0).text() shouldBe "£5,000.00"
      }
      "render the second Payment On Account Summary Card" in new Setup(isAgent) {
        document.getElementsByClass("govuk-summary-card__title").get(1).text() shouldBe messages("paymentOnAccount.table-heading-2")
        document.getElementById("poa2-more-details-date-link").text() shouldBe messages("paymentOnAccount.table-heading.link")
        document.getElementById("poa2-more-details-date-link").getElementsByTag("a").attr("href") shouldBe getChargeSummaryUrl(isAgent, "poa-two-id")
        document.getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe messages("paymentOnAccount.table-heading.key")
        document.getElementsByClass("govuk-summary-list__value govuk-summary-list__value govuk-table__cell--numeric").get(1).text() shouldBe "£5,000.00"
      }
      "render the second paragraph text" in new Setup(isAgent) {
        document.getElementById("paragraph-2-text").text() shouldBe messages("paymentOnAccount.p3")
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
    if (isAgent) controllers.routes.HomeController.showAgent
    else         controllers.routes.HomeController.show()
  }.url

  executeTest(isAgent = true)
  executeTest(isAgent = false)
}
