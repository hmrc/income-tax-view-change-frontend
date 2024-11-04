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

package views.agent.errorPages

import org.jsoup.select.Elements
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.agent.errorPages.UnauthorisedAgentView

class UnauthorisedAgentViewSpec extends ViewSpec {

  val unauthorisedAgentView: UnauthorisedAgentView = app.injector.instanceOf[UnauthorisedAgentView]
  val unauthorisedAgentPage: Html = unauthorisedAgentView()


  object unauthorisedErrorMessages {
    val heading: String = messages("agent-unauthorised.heading")
    val title: String = s"${messages("agent-unauthorised.heading")} - GOV.UK"
    val note: String = messages("agent-unauthorised.note")
    val bullet1: String = messages("agent-unauthorised.note.b1")
    val bullet2: String = messages("agent-unauthorised.note.b2")
    val bullet3: String = messages("agent-unauthorised.note.b3")
    val link: String = messages("agent-unauthorised.link")
  }

  "The Unauthorised Agent view page" should {

    s"have the title: ${unauthorisedErrorMessages.title}" in new Setup(unauthorisedAgentPage) {
      document.title shouldBe unauthorisedErrorMessages.title
    }

    s"have the heading: ${unauthorisedErrorMessages.heading}" in new Setup(unauthorisedAgentPage) {
      document hasPageHeading unauthorisedErrorMessages.heading
    }

    s"have a paragraph stating: ${unauthorisedErrorMessages.note}" in new Setup(unauthorisedAgentPage) {
      layoutContent.getElementsByTag("p").get(0).text shouldBe unauthorisedErrorMessages.note
    }

    s"have a first bullet point: ${unauthorisedErrorMessages.bullet1}" in new Setup(unauthorisedAgentPage) {
      layoutContent.select("ul li:nth-child(1)").text shouldBe unauthorisedErrorMessages.bullet1
    }

    s"have a second bullet point: ${unauthorisedErrorMessages.bullet2}" in new Setup(unauthorisedAgentPage) {
      layoutContent.select("ul li:nth-child(2)").text shouldBe unauthorisedErrorMessages.bullet2
    }

    s"have a third bullet point: ${unauthorisedErrorMessages.bullet3}" in new Setup(unauthorisedAgentPage) {
      layoutContent.select("ul li:nth-child(3)").text shouldBe unauthorisedErrorMessages.bullet3
    }

    s"have a link: ${unauthorisedErrorMessages.link}" in new Setup(unauthorisedAgentPage) {
      val link: Elements = layoutContent.select("div div div.govuk-\\!-margin-bottom-6 a")
      link.text shouldBe unauthorisedErrorMessages.link
      link.attr("href") shouldBe "#"
    }
  }
}
