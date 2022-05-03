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

import enums.GatewayPage.{GatewayPage, NoMatch, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}
import testUtils.ViewSpec
import views.html.helpers.injected.BackLinkWithFallback

class BackLinkWithFallbackSpec extends ViewSpec {

  val backLink: BackLinkWithFallback = app.injector.instanceOf[BackLinkWithFallback]

  class Test extends Setup(backLink("testUrl", None))
  class TestWithGatewayPage(page: Option[GatewayPage]) extends Setup(backLink("testUrl", page))

  "The BackLink" should {
    "generate a back link" which {
      "has the correct javascript link" in new Test {
        assert(document.select("script").html contains """<a id="back-js" class="govuk-back-link" href="javascript:history.back()">Back</a>""")
      }
      "has the correct noscript fallback url" in new Test {
        assert(document.select("noscript").text contains """id="back-fallback"""")
        assert(document.select("noscript").text contains "href=\"testUrl\"")
      }
      "has the correct noscript fallback link text" in new Test {
        assert(document.select("noscript").text contains ">Back")
      }
    }

    "generate a back link with gateway page" which {
      "has the correct javascript link" in new TestWithGatewayPage(Some(PaymentHistoryPage)) {
        assert(document.select("script").html contains """<a id="back-js" class="govuk-back-link" href="javascript:history.back()">Back</a>""")
      }
      "has the correct noscript fallback url" in new TestWithGatewayPage(Some(PaymentHistoryPage)) {
        assert(document.select("noscript").text contains """id="back-fallback"""")
        assert(document.select("noscript").text contains "href=\"testUrl\"")
      }
      "has the correct noscript fallback link text with Payment History Page" in new TestWithGatewayPage(Some(PaymentHistoryPage)) {
        assert(document.select("noscript").text contains ">Back to Payment History")
      }
      "has the correct noscript fallback link text with WhatYouOwe Page" in new TestWithGatewayPage(Some(WhatYouOwePage)) {
        assert(document.select("noscript").text contains ">Back to What You Owe")
      }
      "has the correct noscript fallback link text with TaxYearSummary Page" in new TestWithGatewayPage(Some(TaxYearSummaryPage)) {
        assert(document.select("noscript").text contains ">Back to Tax Year Summary")
      }
      "has the correct noscript fallback link text with NoMatch Page" in new TestWithGatewayPage(Some(NoMatch)) {
        assert(document.select("noscript").text contains ">Back")
      }
    }
  }
}
