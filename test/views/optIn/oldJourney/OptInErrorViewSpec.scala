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

package views.optIn.oldJourney

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.oldJourney.OptInErrorView

class OptInErrorViewSpec extends TestSupport {

  val optInErrorPage: OptInErrorView = app.injector.instanceOf[OptInErrorView]

  class Setup(isAgent: Boolean) {
    val pageDocument: Document = Jsoup.parse(contentAsString(optInErrorPage(isAgent)))
  }

  object optInErrorPageMessages {
    val heading: String = messages("optin.optInError.heading")
    val options: String = messages("optin.optInError.options")
    val optInNextUpdatesBullet1: String = messages("optin.optInError.nextUpdates-bullet-1")
    val optInNextUpdatesBulletLink: String = messages("optin.optInError.nextUpdates-link")
    val optInNextUpdatesBullet2: String = messages("optin.optInError.nextUpdates-bullet-2")
    val optInNextUpdatesTotal: String = optInNextUpdatesBullet1 ++ " " ++ optInNextUpdatesBulletLink ++ " " ++ optInNextUpdatesBullet2
    val optInHomeBullet1: String = messages("optin.optInError.home-bullet-1")
    val optInHomeBullet2: String = messages("optin.optInError.home-bullet-2")
    val optInHomeTotal: String = optInHomeBullet1 ++ " " ++ optInHomeBullet2

    val nextUpdatesLink: String = controllers.routes.NextUpdatesController.show().url
    val nextUpdatesLinkAgent: String = controllers.routes.NextUpdatesController.showAgent().url
    val homePageLink: String = controllers.routes.HomeController.show().url
    val homePageLinkAgent: String = controllers.routes.HomeController.showAgent().url

  }

  "OptIn confirm page for individuals" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title().contains(optInErrorPageMessages.heading)
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe optInErrorPageMessages.heading
    }

    "render the navigation options 1 " in new Setup(false) {
      pageDocument.getElementById("options").text() shouldBe optInErrorPageMessages.options
    }

    "render the navigation options" in new Setup(false) {
      pageDocument.getElementById("nextUpdatesBullet").text() shouldBe optInErrorPageMessages.optInNextUpdatesTotal
      pageDocument.getElementById("next-updates-link").attr("href") shouldBe optInErrorPageMessages.nextUpdatesLink
      pageDocument.getElementById("homepageBullet").text() shouldBe optInErrorPageMessages.optInHomeTotal
      pageDocument.getElementById("home-link").attr("href") shouldBe optInErrorPageMessages.homePageLink
    }
  }

  "OptIn confirm page for agents" should {

    "have the correct title" in new Setup(true) {
      pageDocument.title().contains(optInErrorPageMessages.heading)
    }

    "have the correct heading" in new Setup(true) {
      pageDocument.select("h1").text() shouldBe optInErrorPageMessages.heading
    }

    "render the navigation options 1 " in new Setup(true) {
      pageDocument.getElementById("options").text() shouldBe optInErrorPageMessages.options
    }

    "render the navigation options" in new Setup(true) {
      pageDocument.getElementById("nextUpdatesBullet").text() shouldBe optInErrorPageMessages.optInNextUpdatesTotal
      pageDocument.getElementById("next-updates-link").attr("href") shouldBe optInErrorPageMessages.nextUpdatesLinkAgent
      pageDocument.getElementById("homepageBullet").text() shouldBe optInErrorPageMessages.optInHomeTotal
      pageDocument.getElementById("home-link").attr("href") shouldBe optInErrorPageMessages.homePageLinkAgent
    }
  }
}