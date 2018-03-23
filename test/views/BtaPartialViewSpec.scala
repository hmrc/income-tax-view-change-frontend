/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.Messages.{BtaPartial => messages}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.TestSupport

class BtaPartialViewSpec extends TestSupport {

  lazy val page: HtmlFormat.Appendable = views.html.btaPartial()(FakeRequest(), applicationMessages, frontendAppConfig)
  lazy val document = Jsoup.parse(page.body)

  "The BtaPartial view" should {

    s"have the heading '${messages.heading}'" in {
      document.getElementById("it-quarterly-reporting-heading").text shouldBe messages.heading
    }

    s"have the correct p1 message '${messages.p1}'" in {
      document.getElementById("it-quarterly-reporting-p1").text shouldBe messages.p1
    }

    s"have the correct p2 message '${messages.p2}'" in {
      document.getElementById("it-quarterly-reporting-p2").text shouldBe messages.p2
    }

    s"have have a button to the ITVC home page" which {

      lazy val homeButton = document.getElementById("it-quarterly-reporting-home-button")

      s"has the correct link to '${controllers.routes.HomeController.home().url}'" in {
        homeButton.attr("href") shouldBe frontendAppConfig.itvcFrontendEnvironment + controllers.routes.HomeController.home().url
      }

      s"has the correct button text of '${messages.button}'" in {
        homeButton.text shouldBe messages.button
      }

      "has the correct button class of 'button'" in {
        homeButton.hasClass("button") shouldBe true
      }

    }

  }

}
