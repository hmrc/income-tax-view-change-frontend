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

import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.agent.errorPages.AgentError

class AgentErrorViewSpec extends ViewSpec {

  object AgentErrorMessages {
    val heading: String = messages("agent-error.heading")
    val title: String = messages("htmlTitle.errorPage", messages("agent-error.heading"))
    val setupAccountLink: String = s"${messages("agent-error.link")} ${messages("pagehelp.opensInNewTabText")}"
    val notAnAgentNote: String = s"${messages("agent-error.note")} $setupAccountLink."
  }

  def agentErrorView: Html = app.injector.instanceOf[AgentError].apply()

  "The Agent Error page" should {

    s"have the title: ${AgentErrorMessages.heading}" in new Setup(agentErrorView) {
      document.title shouldBe AgentErrorMessages.title
    }

    s"have the heading: ${AgentErrorMessages.heading}" in new Setup(agentErrorView) {
      document hasPageHeading AgentErrorMessages.heading
    }

    "not have a back link" in new Setup(agentErrorView) {
      document doesNotHave Selectors.backLink
    }

    s"have a paragraph stating: ${AgentErrorMessages.notAnAgentNote}" in new Setup(agentErrorView) {
      layoutContent.select(Selectors.p).text shouldBe AgentErrorMessages.notAnAgentNote
    }

    s"have a link in the paragraph: ${AgentErrorMessages.setupAccountLink}" in new Setup(agentErrorView) {
      layoutContent.selectFirst(Selectors.p)
        .hasCorrectLink(AgentErrorMessages.setupAccountLink, "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account")
    }

    s"have a sign out button stating: ${messages("base.sign-out")}" in new Setup(agentErrorView) {
      val signoutLinkButton = layoutContent.select("a[class=govuk-button]")
      signoutLinkButton.text shouldBe messages("base.sign-out")
      signoutLinkButton.attr("href") shouldBe controllers.routes.SignOutController.signOut().url
    }

  }
}
