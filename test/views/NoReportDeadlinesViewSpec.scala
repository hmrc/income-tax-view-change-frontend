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
import assets.MessagesLookUp.{NoReportDeadlines => reportDeadlinesMessages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.NoReportDeadlines

class NoReportDeadlinesViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val noReportDeadlinesView = app.injector.instanceOf[NoReportDeadlines]

  "The noReportDeadlines view" should {

    lazy val page: Html = noReportDeadlinesView("testBackURL")(implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    s"have the title '${reportDeadlinesMessages.title}'" in {
      document.title() shouldBe reportDeadlinesMessages.title
    }

    s"have the heading '${reportDeadlinesMessages.heading}'" in {
      document.getElementById("page-heading").text() shouldBe reportDeadlinesMessages.heading
    }

    s"have the text the correct content text" in {
      document.getElementById("p1").text() shouldBe reportDeadlinesMessages.noReports
    }

  }


}
