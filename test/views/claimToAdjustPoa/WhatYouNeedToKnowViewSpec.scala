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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.Html
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants.whatYouNeedToKnowViewModel
import testUtils.TestSupport
import viewUtils.ExternalUrlHelper.currentLPAndRepaymentInterestRatesUrl
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

class WhatYouNeedToKnowViewSpec extends TestSupport {

  val whatYouNeedToKnowView: WhatYouNeedToKnow = app.injector.instanceOf[WhatYouNeedToKnow]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("GB")

  val testCancelUrl: String = "/report-quarterly/income-and-expenses/view"

  class Setup(isAgent: Boolean = false, showIncreaseAfterPaymentContent: Boolean = false) {
    val view: Html = whatYouNeedToKnowView(isAgent, whatYouNeedToKnowViewModel(isAgent, showIncreaseAfterPaymentContent))
    val document: Document = Jsoup.parse(view.toString())
    val groupButton: Elements = document.select("div.govuk-button-group")
    val elements = groupButton.first().children()
  }

  "The WhatYouNeedToKnow page" should {
   val expectedCaption = msgs("claimToAdjustPoa.whatYouNeedToKnow.caption", fixedDate.getYear.toString, (fixedDate.getYear + 1).toString)
    val expectedH1 = msgs("claimToAdjustPoa.whatYouNeedToKnow.h1")
    "render the page heading" in new Setup {
        document.title shouldBe msgs("htmlTitle", msgs("claimToAdjustPoa.whatYouNeedToKnow.heading"))
      }

      "render the caption" in new Setup {
        document.getElementById("caption").text shouldBe expectedCaption
      }

      "render the main heading" in new Setup {
        document.getElementById("h1").text shouldBe expectedH1
      }

      "render the first paragraph" in new Setup {
        document.getElementById("p1").text shouldBe
          msgs("claimToAdjustPoa.whatYouNeedToKnow.p1", fixedDate.getYear.toString, (fixedDate.getYear + 1).toString) + " " +
            msgs("claimToAdjustPoa.whatYouNeedToKnow.bold", (fixedDate.getYear + 2).toString) + msgs("claimToAdjustPoa.whatYouNeedToKnow.onlyForWelsh")
      }

      "render the warning text" in new Setup {
        document.getElementById("warning-text").text shouldBe s"! Warning ${msgs("claimToAdjustPoa.whatYouNeedToKnow.warning-text")}"
      }

      "render the second paragraph" in new Setup {
        document.getElementById("p2").text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.p2")
      }

      "render the subheading" in new Setup {
        document.select("h2").get(1).text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.h2")
      }

      "render the third paragraph" in new Setup {
        document.getElementById("p3").text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.p3")
      }

      "render the fourth paragraph" in new Setup {
        document.getElementById("p4").text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.p4")
      }

      "render the fifth paragraph" in new Setup {
        document.getElementById("p5").text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.p5")
      }

      "render the newTabLinkHTML with href" in new Setup {
        document.select("a.govuk-link").get(4).text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.link")
        document.select("a.govuk-link").get(4).attr("href") shouldBe currentLPAndRepaymentInterestRatesUrl
      }

      "have a 'Continue' button" in new Setup {
        elements.get(0).text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.continue-button")
      }

      "have a 'Cancel' button with backUrl" in new Setup {
        elements.get(1).text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.cancel")
        document.getElementById("cancel").attr("href") shouldBe testCancelUrl
      }

      "render warning text when showIncreaseAfterPaymentContent is true" in new Setup(showIncreaseAfterPaymentContent = true) {
        document.getElementById("p6").text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.increase-after-payment.p1")
      }

    "not render warning text when showIncreaseAfterPaymentContent is false" in new Setup(showIncreaseAfterPaymentContent = false) {
      Option(document.getElementById("p6")) shouldBe None
    }
  }

}