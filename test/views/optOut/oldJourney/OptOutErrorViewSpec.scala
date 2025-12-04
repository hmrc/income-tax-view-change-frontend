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

package views.optOut.oldJourney

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optOut.oldJourney.OptOutErrorView

class OptOutErrorViewSpec extends TestSupport {

  val optOutErrorPage: OptOutErrorView = app.injector.instanceOf[OptOutErrorView]

  class Setup(isAgent: Boolean) {
    val pageDocument: Document = Jsoup.parse(contentAsString(optOutErrorPage(isAgent)))
  }

  object optOutErrorPageMessages {
    val heading: String = messages("optout.optOutError.heading")
    val options: String = messages("optout.optOutError.options")
    val optOutNextUpdatesBullet1: String = messages("optout.optOutError.nextUpdates-bullet-1")
    val optOutNextUpdatesBulletLink: String = messages("optout.optOutError.nextUpdates-link")
    val optOutNextUpdatesBullet2: String = messages("optout.optOutError.nextUpdates-bullet-2")
    val optOutNextUpdatesTotal: String = optOutNextUpdatesBullet1 ++ " " ++ optOutNextUpdatesBulletLink ++ " " ++ optOutNextUpdatesBullet2
    val optOutHomeBullet1: String = messages("optout.optOutError.home-bullet-1")
    val optOutHomeBullet2: String = messages("optout.optOutError.home-bullet-2")
    val optOutHomeTotal: String = optOutHomeBullet1 ++ " " ++ optOutHomeBullet2

    val nextUpdatesLink: String = controllers.routes.NextUpdatesController.show().url
    val nextUpdatesLinkAgent: String = controllers.routes.NextUpdatesController.showAgent().url
    val homePageLink: String = controllers.routes.HomeController.show().url
    val homePageLinkAgent: String = controllers.routes.HomeController.showAgent().url

  }

  "Opt-out confirm page for individuals" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title().contains(optOutErrorPageMessages.heading)
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe optOutErrorPageMessages.heading
    }

    "render the navigation options 1 " in new Setup(false) {
      pageDocument.getElementById("options").text() shouldBe optOutErrorPageMessages.options
    }

    "render the navigation options" in new Setup(false) {
      pageDocument.getElementById("nextUpdatesBullet").text() shouldBe optOutErrorPageMessages.optOutNextUpdatesTotal
      pageDocument.getElementById("next-updates-link").attr("href") shouldBe optOutErrorPageMessages.nextUpdatesLink
      pageDocument.getElementById("homepageBullet").text() shouldBe optOutErrorPageMessages.optOutHomeTotal
      pageDocument.getElementById("home-link").attr("href") shouldBe optOutErrorPageMessages.homePageLink
    }
  }

  "Opt-out confirm page for agents" should {

    "have the correct title" in new Setup(true) {
      pageDocument.title().contains(optOutErrorPageMessages.heading)
    }

    "have the correct heading" in new Setup(true) {
      pageDocument.select("h1").text() shouldBe optOutErrorPageMessages.heading
    }

    "render the navigation options 1 " in new Setup(true) {
      pageDocument.getElementById("options").text() shouldBe optOutErrorPageMessages.options
    }

    "render the navigation options" in new Setup(true) {
      pageDocument.getElementById("nextUpdatesBullet").text() shouldBe optOutErrorPageMessages.optOutNextUpdatesTotal
      pageDocument.getElementById("next-updates-link").attr("href") shouldBe optOutErrorPageMessages.nextUpdatesLinkAgent
      pageDocument.getElementById("homepageBullet").text() shouldBe optOutErrorPageMessages.optOutHomeTotal
      pageDocument.getElementById("home-link").attr("href") shouldBe optOutErrorPageMessages.homePageLinkAgent
    }
  }
}
