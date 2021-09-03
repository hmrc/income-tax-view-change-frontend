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

package views.agent

import org.jsoup.Jsoup
import testUtils.{TestSupport, ViewSpec}
import views.html.templates.agent.MainTemplate

class MainTemplateViewSpec extends TestSupport with ViewSpec {

  lazy val mainTemplate = app.injector.instanceOf[MainTemplate]

  "The mainTemplate" should {

    lazy val view = mainTemplate("unitTest")(implicitly)
    lazy val document = Jsoup.parse(view.body)

    s"show the footer" in {
      document.getElementsByClass("platform-help-links").text shouldBe "Cookies Accessibility statement Privacy policy Terms and conditions Help using GOV.UK"
    }

  }

}
