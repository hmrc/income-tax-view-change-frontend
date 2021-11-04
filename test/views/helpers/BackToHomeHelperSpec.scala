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

import testConstants.MessagesLookUp
import org.jsoup.Jsoup
import testUtils.TestSupport
import views.html.helpers.injected.BackToHomeHelper

class BackToHomeHelperSpec extends TestSupport {

  lazy val backToHomeHelper = app.injector.instanceOf[BackToHomeHelper]

  "The backToHomeHelper template" should {

    lazy val view = backToHomeHelper("unitTest")(implicitly)
    lazy val document = Jsoup.parse(view.body)
    lazy val backToHomeLink = document.getElementById("back")

    s"Render the text ${MessagesLookUp.Base.backToHome}" in {
      backToHomeLink.text shouldBe MessagesLookUp.Base.backToHome
    }

    "Have the correct link class of 'link-back'" in {
      backToHomeLink.hasClass("link-back") shouldBe true
    }

    s"Have the correct href to '${controllers.routes.HomeController.home().url}'" in {
      backToHomeLink.attr("href") shouldBe controllers.routes.HomeController.home().url
    }
  }
}
