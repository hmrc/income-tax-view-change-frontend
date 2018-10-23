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

package views.helpers

import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import testUtils.TestSupport

class BetaBannerHelperSpec extends TestSupport {

  "The beta banner" should {

    lazy val view = views.html.helpers.betaBanner()(applicationMessages, frontendAppConfig)
    lazy val document = Jsoup.parse(view.body)

    "have the BETA label" in {
      document.getElementsByClass("phase-tag").text shouldBe "BETA"
    }

    "have the correct content" in {
      document.getElementsByClass("beta-banner").addClass("span").text shouldBe
        "BETA This is a new service – your feedback will help us to improve it."
    }

    "have the correct link text" in {
      document.getElementById("feedback-link").text shouldBe "feedback"
    }

    "have the correct link location" in {
      document.getElementById("feedback-link").attr("href") shouldBe frontendAppConfig.betaFeedbackUrl
    }
  }
}
