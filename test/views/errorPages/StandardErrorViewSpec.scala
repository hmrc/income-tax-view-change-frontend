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

package views.errorPages

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.TestSupport

class StandardErrorViewSpec extends TestSupport {

  "The Standard Error view" should {

    lazy val page: HtmlFormat.Appendable = views.html.errorPages.standardError(
      messagesApi.apply("standardError.title"),
      messagesApi.apply("standardError.heading"),
      messagesApi.apply("standardError.message")
    )
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    "render the correct title" in {
      document.title() shouldBe "There is a problem with the service - Income Tax reporting through software - GOV.UK"
    }

    "render the correct heading" in {
      document.getElementsByTag("h1").text() shouldBe "Sorry, there is a problem with the service"
    }

    "render the correct message" in {
      document.getElementsByClass("lede").text() shouldBe "Try again later."
    }
  }
}
