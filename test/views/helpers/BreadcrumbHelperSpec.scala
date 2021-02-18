/*
 * Copyright 2021 HM Revenue & Customs
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

import models.core.breadcrumb.{Breadcrumb, BreadcrumbItem}
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import views.html.helpers.breadcrumbHelper

class BreadcrumbHelperSpec extends ViewSpec {

  class Test extends Setup(
    breadcrumbHelper(
      Breadcrumb(Vector(
        BreadcrumbItem("breadcrumb-tax-year-overview", Some("test-url"), Some("test custom text")),
        BreadcrumbItem("breadcrumb-it", Some("test-url-2"), None),
        BreadcrumbItem("breadcrumb-tax-years", None, None)
      )), "test-page"
    )
  )

  "The breadcrumb helper" should {

    "have the correct number of items in the breadcrumb" in new Test {
      document.breadcrumbNav.select("li").size shouldBe 4
    }

    "have an aria-label for the breadcrumb navigation" in new Test {
      document.breadcrumbNav.attr("aria-label") shouldBe "breadcrumbs"
    }

    "have a breadcrumb link to BTA" in new Test {
      val btaLink: Element = document.breadcrumbNav.selectNth("li", 1).selectHead("a")
      btaLink.attr("href") shouldBe appConfig.businessTaxAccount
      btaLink.text shouldBe "Business tax account"
    }

    "have a breadcrumb link to the first added item with custom text" in new Test {
      val customTextBreadcrumb: Element = document.breadcrumbNav.selectNth("li", 2).selectHead("a")
      customTextBreadcrumb.attr("href") shouldBe "test-url"
      customTextBreadcrumb.text shouldBe "test custom text"
    }

    "have a breadcrumb link to the second added item with no custom text, text comes from messages using breadcrumb id" in new Test {
      val noCustomTextBreadcrumb: Element = document.breadcrumbNav.selectNth("li", 3).selectHead("a")
      noCustomTextBreadcrumb.attr("href") shouldBe "test-url-2"
      noCustomTextBreadcrumb.text shouldBe "Income Tax account"
    }

    "have a breadcrumb without a link as the third added item, text comes from messages using breadcrumb id" in new Test {
      document.breadcrumbNav.selectNth("li", 4).selectHead("span").text shouldBe "Tax years"
    }
  }
}
