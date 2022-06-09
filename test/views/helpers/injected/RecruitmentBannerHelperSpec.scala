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
import views.html.helpers.injected.RecruitmentBannerHelper

class RecruitmentBannerHelperSpec extends ViewSpec {

  val recruitmentBannerHelper: RecruitmentBannerHelper = app.injector.instanceOf[RecruitmentBannerHelper]

  class Test extends Setup(recruitmentBannerHelper())

  "The recruitment banner" should {

    s"have the correct header message '${messages("banner.recruitment.text")}'" in new Test {
      document.getElementById("recruitment-banner-text").text shouldBe messages("banner.recruitment.text")
    }
    s"have the correct link text '${messages("banner.recruitment.link")}' and url ${appConfig.enterSurveyUrl}" in new Test {
      document.getElementById("recruitment-banner-link").text shouldBe messages("banner.recruitment.link")
      document.getElementById("recruitment-banner-link").attr("href") shouldBe appConfig.enterSurveyUrl
    }
    s"have the correct dismiss text '${messages("banner.recruitment.dismiss")}'" in new Test {
      document.getElementById("recruitment-banner-dismiss").text shouldBe messages("banner.recruitment.dismiss")
    }

  }

}
