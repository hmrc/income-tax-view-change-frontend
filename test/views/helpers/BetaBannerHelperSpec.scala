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

package views.helpers

import org.jsoup.Jsoup
import testUtils.TestSupport
import views.html.helpers.injected.BetaBanner

class BetaBannerHelperSpec extends TestSupport {

  lazy val betaBanner = app.injector.instanceOf[BetaBanner]

  "The beta banner" should {

    lazy val view     = betaBanner()(implicitly)
    lazy val document = Jsoup.parse(view.body)

    s"have the ${messages("base.phase")} label" in {
      document.getElementsByClass("phase-tag").text shouldBe messages("base.phase")
    }

    "have the correct content" in {
      document.getElementsByClass("beta-banner").addClass("span").text shouldBe
        s"${messages("betaBanner.beta")} ${messages("betaBanner.newService")} ${messages("betaBanner.your")} ${messages("betaBanner.feedback")} ${messages("betaBanner.improve")}"
    }

    "have the correct link text" in {
      document.getElementById("feedback-link").text shouldBe messages("betaBanner.feedback")
    }

    "have the correct link location" in {
      document.getElementById("feedback-link").attr("href") shouldBe appConfig.betaFeedbackUrl
    }
  }
}
