/*
 * Copyright 2022 HM Revenue & Customs
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

import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testListLink
import testUtils.TestSupport
import views.html.bta.BtaNavBar

class BtaNavBarViewSpec extends TestSupport{

  val btaNavBarView = app.injector.instanceOf[BtaNavBar]
  lazy val page: HtmlFormat.Appendable = btaNavBarView(testListLink)(implicitly)
  lazy val document = Jsoup.parse(page.body)

  val home = "testEnHome"
  val manageAccount = "testEnAccount"
  val message = "testEnMessages"
  val help = "testEnHelp"

  "The BtaNavBar view" should {

    s"have right text '$home' and h-ref" in {
      val link = document.getElementById(s"nav-bar-link-$home")
      link.text shouldBe home
      link.attr("href") shouldBe "http://localhost:9081/report-quarterly/income-and-expenses/view"
    }

    s"have right text '$manageAccount' and h-ref'" in {
      val link = document.getElementById(s"nav-bar-link-$manageAccount")
      link.text shouldBe manageAccount
      link.attr("href") shouldBe "testUrl"
    }

    s"have right text '$message' and h-ref" in {
      val link = document.getElementById(s"nav-bar-link-$message")
      link.text shouldBe message
      link.attr("href") shouldBe "testUrl"
    }

    s"have right text '$help' and h-ref" in {
      val link = document.getElementById(s"nav-bar-link-$help")
      link.text shouldBe help
      link.attr("href") shouldBe "testUrl"
    }

    "badge should show the right number" in{
      document.getElementsByClass("hmrc-notification-badge").text shouldBe "1"
    }

  }

}
