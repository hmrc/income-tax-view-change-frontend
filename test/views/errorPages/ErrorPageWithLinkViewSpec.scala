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

package views.errorPages

import play.twirl.api.HtmlFormat
import testUtils.ViewSpec
import views.html.errorPages.templates.ErrorTemplateWithLink

class ErrorPageWithLinkViewSpec extends ViewSpec {
  val view: HtmlFormat.Appendable = app.injector.instanceOf[ErrorTemplateWithLink].apply(pageTitle = "pageTitle",
    heading = "heading",
    message = "message",
    linkText = "linkText",
    linkUrl = controllers.routes.HomeController.show().url,
    linkPrefix = Some("linkPrefix"), isAgent = false)

  "The error page with link view" should {
    "display a h1" in new Setup(view) {
      document hasPageHeading "heading"
    }
    "display the HTML title" in new Setup(view) {
      document.title() shouldBe messages("htmlTitle.errorPage", "pageTitle")
    }
    "display the text" in new Setup(view) {
      layoutContent.selectNth("p", 1).text() shouldBe "message"
    }
    "display the link with link prefix text" in new Setup(view) {
      layoutContent.selectNth("p", 2).text() shouldBe "linkPrefix linkText"
    }
    "display the link url" in new Setup(view) {
      layoutContent.selectNth("p", 2) hasCorrectHref controllers.routes.HomeController.show().url
    }
  }
}
