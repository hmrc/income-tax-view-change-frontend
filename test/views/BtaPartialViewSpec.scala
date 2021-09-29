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

import assets.MessagesLookUp.{BtaPartial => btaPartialMessages}
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.BtaPartial

class BtaPartialViewSpec extends TestSupport {

  val btaPartialView = app.injector.instanceOf[BtaPartial]
  lazy val page: HtmlFormat.Appendable = btaPartialView()(implicitly)
  lazy val document = Jsoup.parse(page.body)

  "The BtaPartial view" should {

    s"have the heading '${btaPartialMessages.heading}'" in {
      document.getElementById("it-quarterly-reporting-heading").text shouldBe btaPartialMessages.heading
    }

    s"have the correct p1 message '${btaPartialMessages.p1}'" in {
      document.getElementById("it-quarterly-reporting-p1").text shouldBe btaPartialMessages.p1
    }

    s"have the correct p2 message '${btaPartialMessages.p2}'" in {
      document.getElementById("it-quarterly-reporting-p2").text shouldBe btaPartialMessages.p2
    }

    s"have have a button to the ITVC home page" which {

      lazy val homeButton = document.getElementById("it-quarterly-reporting-home-button")

      s"has the correct link to '${controllers.routes.HomeController.home().url}'" in {
        homeButton.attr("href") shouldBe appConfig.itvcFrontendEnvironment + controllers.routes.HomeController.home().url
      }

      s"has the correct button text of '${btaPartialMessages.button}'" in {
        homeButton.text shouldBe btaPartialMessages.button
      }

      "has the correct button class of 'govuk-button'" in {
        homeButton.hasClass("govuk-button") shouldBe true
      }

    }

  }

}
