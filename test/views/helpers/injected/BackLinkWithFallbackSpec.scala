/*
 * Copyright 2025 HM Revenue & Customs
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

import enums.GatewayPage._
import testUtils.ViewSpec
import views.html.helpers.injected.BackLinkWithFallback

class BackLinkWithFallbackSpec extends ViewSpec {

  val backLink: BackLinkWithFallback = app.injector.instanceOf[BackLinkWithFallback]

  class Test extends Setup(backLink("testUrl", None))

  class TestWithGatewayPage(page: Option[GatewayPage]) extends Setup(backLink("testUrl", page))

  "BackLinkWithFallback" should {

    "generate a JavaScript back link" which {
      "contains the expected JavaScript snippet" in new Test {
        val scriptContent = document.select("script").html()

        scriptContent should include("document.addEventListener(\"DOMContentLoaded\", function() {")
        scriptContent should include("var container = document.getElementById(\"backlink-container\");")
        scriptContent should include("var backLink = document.createElement(\"a\");")
        scriptContent should include("backLink.id = \"back-js\";")
        scriptContent should include("backLink.className = \"govuk-back-link\";")
        scriptContent should include("backLink.href = \"#\";")
        scriptContent should include("backLink.addEventListener(\"click\", function(event) {")
      }
    }

    "generate a noscript fallback link" which {
      "contains the correct URL" in new Test {
        val noscriptContent = document.select("noscript").html()
        noscriptContent should include("id=\"back-fallback\"")
        noscriptContent should include("href=\"testUrl\"")
      }

      "contains the correct default text" in new Test {
        val noscriptContent = document.select("noscript").text()
        noscriptContent should include("Back")
      }
    }

    "generate a noscript fallback with a gateway page" which {
      "contains the correct URL for Payment History Page" in new TestWithGatewayPage(Some(PaymentHistoryPage)) {
        val noscriptContent = document.select("noscript").html()
        noscriptContent should include("id=\"back-fallback\"")
        noscriptContent should include("href=\"testUrl\"")
      }

      "has the correct text for Payment History Page" in new TestWithGatewayPage(Some(PaymentHistoryPage)) {
        val noscriptContent = document.select("noscript").text()
        noscriptContent should include(messages("Back"))
      }

      "has the correct text for WhatYouOwe Page" in new TestWithGatewayPage(Some(WhatYouOwePage)) {
        val noscriptContent = document.select("noscript").text()
        noscriptContent should include(messages("Back"))
      }

      "has the correct text for Tax Year Summary Page" in new TestWithGatewayPage(Some(TaxYearSummaryPage)) {
        val noscriptContent = document.select("noscript").text()
        noscriptContent should include(messages("back.taxYearSummary"))
      }

      "has the correct text for NoMatch Page" in new TestWithGatewayPage(Some(NoMatch)) {
        val noscriptContent = document.select("noscript").text()
        noscriptContent should include(messages("back.nomatch"))
      }
    }
  }
}
