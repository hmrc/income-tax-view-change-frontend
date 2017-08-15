/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.Messages.{Thankyou => messages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.Helpers.{contentAsString, _}
import utils.TestSupport


class ThankYouViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  "The Thankyou page" should {
    lazy val page = views.html.exit_survey.exit_survey_thankyou()(fakeRequestNoSession, applicationMessages, mockAppConfig)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    s"have the title ${messages.title}" in {
      document.title shouldBe messages.title
    }

    s"have the heading ${messages.heading}" in {
      document.getElementById("page-heading").text shouldBe messages.heading
    }

    s"have the paragraph text ${messages.line1}" in {
      document.getElementById("thankyou-message").text shouldBe messages.line1
    }
    s"have the sign in link text ${messages.signInLink}" in {
      document.getElementById("redirectLink").text shouldBe messages.signInLink
    }
  }

}
