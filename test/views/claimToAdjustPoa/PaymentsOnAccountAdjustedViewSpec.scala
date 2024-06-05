/*
 * Copyright 2024 HM Revenue & Customs
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

import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n._
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.claimToAdjustPoa.PaymentsOnAccountAdjustedView

class PaymentsOnAccountAdjustedViewSpec extends TestSupport{

  val paymentsOnAccountAdjustedView: PaymentsOnAccountAdjustedView = app.injector.instanceOf[PaymentsOnAccountAdjustedView]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("GB")

  val taxYear: TaxYear = TaxYear(2023, 2024)
  val poaTotalAmount: BigDecimal = 2000.00

  class Setup(isAgent: Boolean) {
    val view: Html = paymentsOnAccountAdjustedView(isAgent = isAgent, poaTaxYear = taxYear, poaTotalAmount = poaTotalAmount)
    val document: Document = Jsoup.parse(view.toString())
  }

  def taxYearSummaryUrl(isAgent: Boolean) = {
    if(isAgent) {
      controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear.endYear).url
    } else {
      controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear.endYear).url
    }
  }

  def whatYouOweUrl(isAgent: Boolean) = {
    if(isAgent) {
      controllers.routes.WhatYouOweController.showAgent.url
    } else {
      controllers.routes.WhatYouOweController.show().url
    }
  }

  "The PaymentsOnAccountAdjusted page" when {
    "a user loads the page" should {
      "render the heading" in new Setup(isAgent = false) {
        document.title shouldBe msgs("htmlTitle", msgs("claimToAdjustPoa.success.h1"))
      }
      "render the first paragraph" in new Setup(isAgent = false) {
        document.getElementsByClass("govuk-panel__body").text shouldBe
          msgs("claimToAdjustPoa.success.p1", taxYear.startYear.toString, taxYear.endYear.toString, poaTotalAmount.toCurrencyString)
      }
      "render the second paragraph" in new Setup(isAgent = false) {
        document.getElementById("p2").text shouldBe
          msgs("claimToAdjustPoa.success.p2", taxYear.startYear.toString, taxYear.endYear.toString, taxYear.nextYear.endYear.toString)
      }
      "render the third paragraph" in new Setup(isAgent = false) {
        document.getElementById("p3").text shouldBe
          msgs("claimToAdjustPoa.success.checkYour") + " " + msgs("claimToAdjustPoa.success.link", taxYear.startYear.toString, taxYear.endYear.toString) + " " +
            msgs("claimToAdjustPoa.success.afterLinkText")
        document.getElementById("p3").getElementById("link").attr("href") shouldBe taxYearSummaryUrl(false)
      }
      "render the final paragraph" in new Setup(isAgent = false) {
        document.getElementById("p4").text shouldBe
          msgs("claimToAdjustPoa.success.check") + " " + msgs("claimToAdjustPoa.success.whatYouOwe") + " " + msgs("claimToAdjustPoa.success.forUpcomingCharges")
        document.getElementById("p4").getElementById("link").attr("href") shouldBe whatYouOweUrl(false)
      }
    }
  }

  "The PaymentsOnAccountAdjusted page" when {
    "an agent loads the page" should {
      "render the heading" in new Setup(isAgent = true) {
        document.title shouldBe msgs("htmlTitle.agent", msgs("claimToAdjustPoa.success.h1"))
      }
      "render the first paragraph" in new Setup(isAgent = true) {
        document.getElementsByClass("govuk-panel__body").text shouldBe
          msgs("claimToAdjustPoa.success.p1", taxYear.startYear.toString, taxYear.endYear.toString, poaTotalAmount.toCurrencyString)
      }
      "render the second paragraph" in new Setup(isAgent = true) {
        document.getElementById("p2").text shouldBe
          msgs("claimToAdjustPoa.success.p2", taxYear.startYear.toString, taxYear.endYear.toString, taxYear.nextYear.endYear.toString)
      }
      "render the third paragraph" in new Setup(isAgent = true) {
        document.getElementById("p3").text shouldBe
          msgs("claimToAdjustPoa.success.checkYour") + " " + msgs("claimToAdjustPoa.success.link", taxYear.startYear.toString, taxYear.endYear.toString) + " " +
            msgs("claimToAdjustPoa.success.afterLinkText")
        document.getElementById("p3").getElementById("link").attr("href") shouldBe taxYearSummaryUrl(true)
      }
      "render the final paragraph" in new Setup(isAgent = true) {
        document.getElementById("p4").text shouldBe
          msgs("claimToAdjustPoa.success.check") + " " + msgs("claimToAdjustPoa.success.whatYouOwe") + " " + msgs("claimToAdjustPoa.success.forUpcomingCharges")
        document.getElementById("p4").getElementById("link").attr("href") shouldBe whatYouOweUrl(true)
      }
    }
  }

}
