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
import views.html.helpers.injected.BackLinkWithFallback

class BackLinkWithFallbackSpec extends ViewSpec {

  val backLink: BackLinkWithFallback = app.injector.instanceOf[BackLinkWithFallback]

  class Test extends Setup(backLink("testUrl"))

  "The BackLink" should {

    "generate a back link" which {
      "has the correct javascript link" in new Test {
        document.select("script").text() contains """<a id="back" class="govuk-back-link" href="javascript:history.back()">Back</a>"""
      }
      "has the correct noscript fallback url" in new Test {
        document.select("noscript").text() contains "href=\"testUrl\""
      }
    }
  }

}
