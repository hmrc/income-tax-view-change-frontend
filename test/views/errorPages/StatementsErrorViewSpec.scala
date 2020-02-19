/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.Messages.{Statements => messages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class StatementsErrorViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "The EstimatedTaxLiabilityError view" should {

    lazy val page: HtmlFormat.Appendable = views.html.errorPages.statementsError()(FakeRequest(), applicationMessages, mockAppConfig, testMtdUserNoNino)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    s"have the page heading '${messages.pageHeading}'" in {
      document.getElementById("page-heading").text() shouldBe messages.Error.pageHeading
    }

    s"have the p1 '${messages.Error.p1}'" in {
      document.getElementById("statement-error-para").text() shouldBe messages.Error.p1
    }

    "show a back link to the Income Tax home page" in {
      document.getElementById("it-home-back") shouldNot be(null)
    }
  }
}
