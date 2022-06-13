/*
 * Copyright 2022 HM Revenue & Customs
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

package views.helpers.injected

import testUtils.ViewSpec
import views.html.helpers.injected.BetaBanner

class BetaBannerHelperSpec extends ViewSpec {

  val betaBanner: BetaBanner = app.injector.instanceOf[BetaBanner]

  class Test extends Setup(betaBanner())

  "The beta banner" should {

    "have the BETA label" in new Test {
      document.getElementsByClass("phase-tag").text shouldBe messages("betaBanner.beta")
    }

    "have the correct content" in new Test {
      document.getElementsByClass("beta-banner").addClass("span").text shouldBe
      s"${messages("betaBanner.beta")} ${messages("betaBanner.newService")} ${messages("betaBanner.your")} ${messages("betaBanner.feedback")} ${messages("betaBanner.improve")}"
    }

    "have the correct link text" in new Test {
      document.getElementById("feedback-link").text shouldBe messages("betaBanner.feedback")
    }

    "have the correct link location" in new Test {
      document.getElementById("feedback-link").attr("href") shouldBe appConfig.betaFeedbackUrl
    }
  }

}
