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

package views.errorPages

import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.errorPages.CustomNotFoundError

class CustomNotFoundErrorViewSpec extends ViewSpec {

  def customNotFoundErrorView: Html = app.injector.instanceOf[CustomNotFoundError].apply()

  "The Agent Error page" should {

    s"have the title: ${messages("titlePattern.serviceName.govUk", messages("base.error_summary.heading"))}" in new Setup(customNotFoundErrorView) {
      document.title shouldBe messages("titlePattern.serviceName.govUk", messages("base.error_summary.heading"))
    }

    s"have the heading: ${messages("error.custom.heading")}" in new Setup(customNotFoundErrorView) {
      document hasPageHeading messages("error.custom.heading")
    }

    s"have a paragraph stating" in new Setup(customNotFoundErrorView) {
      layoutContent.select(Selectors.p).text shouldBe messages("error.custom.message")
    }

    s"have a link in to the homepage" in new Setup(customNotFoundErrorView) {
      layoutContent.select(Selectors.link).first().text shouldBe messages("error.custom.link")
      layoutContent.select(Selectors.link).first().attr("href") shouldBe controllers.routes.HomeController.show().url
    }
  }
}
