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
import testUtils.TestSupport
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

class WhatYouNeedToKnowViewSpec extends TestSupport {

  val whatYouNeedToKnowView: WhatYouNeedToKnow = app.injector.instanceOf[WhatYouNeedToKnow]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("GB")

  val testBackUrl: String = "/testBackUrl"

  class Setup(isAgent: Boolean = false) {
    val view: Html = whatYouNeedToKnowView(backUrl = testBackUrl, isAgent = isAgent)
    val document: Document = Jsoup.parse(view.toString())
  }

  "The WhatYouNeedToKnow page" when {
    "an individual loads the page" should {
      "have the correct title" in new Setup() {
        println(document)
        document.title shouldBe msgs("htmlTitle", msgs("claimToAdjustPoa.whatYouNeedToKnow.heading"))
      }

      "have a 'Continue' button" in new Setup {
        val continueButton: Elements = document.select("div.govuk-button-group")
        continueButton.text shouldBe msgs("claimToAdjustPoa.whatYouNeedToKnow.continue-button")
//        println("AAAA" + continueButton.attr("href"))
        //continueButton.attr("href") shouldBe testBackUrl
      }
    }
  }
}