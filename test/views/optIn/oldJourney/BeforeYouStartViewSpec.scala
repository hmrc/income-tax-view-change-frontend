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

package views.optIn.oldJourney

import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.oldJourney.BeforeYouStart

class BeforeYouStartViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val beforeYouStartView: BeforeYouStart = app.injector.instanceOf[BeforeYouStart]
  val startButtonUrl = "/some/optIn/url"

  class Setup(isAgent: Boolean = true) {
    val pageDocument: Document = Jsoup.parse(contentAsString(beforeYouStartView(isAgent, startButtonUrl)))
  }

  object beforeYouStart {
    val heading: String = messages("optIn.beforeYouStart.heading")
    val title: String = messages("htmlTitle", heading)
    val desc1: String = messages("optIn.beforeYouStart.desc1")
    val desc2: String = messages("optIn.beforeYouStart.desc2")
    val reportQuarterly: String = messages("optIn.beforeYouStart.reportQuarterly")
    val voluntaryStatus: String = messages("optIn.beforeYouStart.voluntaryStatus")
    val voluntaryStatusText: String = messages("optIn.beforeYouStart.voluntaryStatus.text")
    val startButton: String = messages("optIn.beforeYouStart.button.start")
  }

  "Before you start page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe beforeYouStart.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe beforeYouStart.heading
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("desc1").text() shouldBe beforeYouStart.desc1
      pageDocument.getElementById("desc2").text() shouldBe beforeYouStart.desc2
      pageDocument.getElementById("reportQuarterly").text() shouldBe beforeYouStart.reportQuarterly
      pageDocument.getElementById("voluntaryStatus").text() shouldBe beforeYouStart.voluntaryStatus
      pageDocument.getElementById("voluntaryStatus-text").text() shouldBe beforeYouStart.voluntaryStatusText
      pageDocument.getElementById("start-button").text() shouldBe beforeYouStart.startButton
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementById("desc1").text() shouldBe beforeYouStart.desc1
      pageDocument.getElementById("desc2").text() shouldBe beforeYouStart.desc2
      pageDocument.getElementById("reportQuarterly").text() shouldBe beforeYouStart.reportQuarterly
      pageDocument.getElementById("voluntaryStatus").text() shouldBe beforeYouStart.voluntaryStatus
      pageDocument.getElementById("voluntaryStatus-text").text() shouldBe beforeYouStart.voluntaryStatusText
      pageDocument.getElementById("start-button").text() shouldBe beforeYouStart.startButton
    }

  }
}
