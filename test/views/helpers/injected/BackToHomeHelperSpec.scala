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
import views.html.helpers.injected.BackToHomeHelper

class BackToHomeHelperSpec extends ViewSpec {

  val backToHome: BackToHomeHelper = app.injector.instanceOf[BackToHomeHelper]

  class Test extends Setup(backToHome("unitTest"))

  "The backToHomeHelper template" should {

    s"Render the text ${messages("base.backToHome")}" in new Test {
      document.backToHome.text shouldBe messages("base.backToHome")
    }

    "Have the correct link class of 'link-back'" in new Test {
      document.backToHome.hasClass("link-back") shouldBe true
    }

    s"Have the correct href to '${controllers.routes.HomeController.show().url}'" in new Test {
      document.backToHome.attr("href") shouldBe controllers.routes.HomeController.show().url
    }
  }

}
