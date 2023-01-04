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

package views

import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.NoNextUpdates

class NoNextUpdatesViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val NoNextUpdatesView: NoNextUpdates = app.injector.instanceOf[NoNextUpdates]

  val heading: String = messages("obligations.heading")

  "The NoNextUpdates view" should {

    lazy val page: Html = NoNextUpdatesView("testBackURL")(FakeRequest(), implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    s"have the title ${messages("htmlTitle", heading)}" in {
      document.title() shouldBe messages("htmlTitle", heading)
    }

    s"have the heading $heading" in {
      document.select("h1").text() shouldBe heading
    }

    s"have the text the correct content text" in {
      document.select("p.govuk-body").text() shouldBe messages("obligations.noReports")
    }

  }


}
